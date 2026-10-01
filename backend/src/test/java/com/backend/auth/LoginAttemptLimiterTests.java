package com.backend.auth;

import com.backend.auth.service.LoginAttemptLimiter;
import com.backend.common.error.BusinessException;
import com.backend.common.error.ErrorCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoginAttemptLimiterTests {

    @Mock
    private StringRedisTemplate redis;

    @Mock
    private ValueOperations<String, String> ops;

    @InjectMocks
    private LoginAttemptLimiter limiter;

    @BeforeEach
    void setUp() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("1.2.3.4");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        lenient().when(redis.opsForValue()).thenReturn(ops);
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    @DisplayName("기록이 없으면 통과한다")
    void allowedWhenNoRecord() {
        when(ops.get(anyString())).thenReturn(null);
        assertDoesNotThrow(() -> limiter.checkAllowed("[EMAIL]"));
    }

    @Test
    @DisplayName("같은 아이디 실패 5회 이상이면 TOO_MANY_REQUEST")
    void blockedById() {
        when(ops.get(startsWith("login:fail:id:"))).thenReturn("5");

        BusinessException e = assertThrows(BusinessException.class, () -> limiter.checkAllowed("[EMAIL]"));
        assertEquals(ErrorCode.TOO_MANY_REQUEST, e.getErrorCode());
    }

    @Test
    @DisplayName("같은 IP 실패 20회 이상이면 아이디가 달라도 TOO_MANY_REQUEST")
    void blockedByIp() {
        when(ops.get(startsWith("login:fail:id:"))).thenReturn("0");
        when(ops.get("login:fail:ip:1.2.3.4")).thenReturn("20");

        BusinessException e = assertThrows(BusinessException.class, () -> limiter.checkAllowed("other"));
        assertEquals(ErrorCode.TOO_MANY_REQUEST, e.getErrorCode());
    }

    @Test
    @DisplayName("첫 실패 시 아이디/IP 키 모두 증가시키고 10분 TTL을 건다")
    void recordFailureSetsTtlOnFirst() {
        when(ops.increment(anyString())).thenReturn(1L);

        limiter.recordFailure("[EMAIL]");

        verify(ops, times(2)).increment(anyString());
        verify(redis).expire(startsWith("login:fail:id:"), eq(Duration.ofMinutes(10)));
        verify(redis).expire(eq("login:fail:ip:1.2.3.4"), eq(Duration.ofMinutes(10)));
    }

    @Test
    @DisplayName("두 번째 이후 실패에서는 TTL을 다시 걸지 않는다 (창이 늘어나지 않음)")
    void recordFailureDoesNotExtendTtl() {
        when(ops.increment(anyString())).thenReturn(3L);

        limiter.recordFailure("[EMAIL]");

        verify(redis, never()).expire(anyString(), any(Duration.class));
    }

    @Test
    @DisplayName("Redis 키에 아이디 원문이 들어가지 않고, 대소문자/공백이 달라도 같은 키를 쓴다")
    void keyIsHashedAndNormalized() {
        when(ops.increment(anyString())).thenReturn(2L);

        limiter.recordFailure("  [EMAIL]  ");
        limiter.recordFailure("[EMAIL]");

        var captor = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(ops, times(4)).increment(captor.capture());
        var idKeys = captor.getAllValues().stream().filter(k -> k.startsWith("login:fail:id:")).toList();
        assertEquals(2, idKeys.size());
        assertEquals(idKeys.get(0), idKeys.get(1));
        assertFalse(idKeys.get(0).contains("[EMAIL]"), "원문 아이디가 키에 노출되면 안 된다");
    }

    @Test
    @DisplayName("reset은 아이디 키만 지운다 (IP 기록은 유지)")
    void resetDeletesOnlyIdKey() {
        limiter.reset("[EMAIL]");

        verify(redis).delete(startsWith("login:fail:id:"));
        verify(redis, never()).delete(startsWith("login:fail:ip:"));
    }
}
