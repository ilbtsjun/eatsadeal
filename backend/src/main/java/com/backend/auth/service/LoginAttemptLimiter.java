package com.backend.auth.service;

import com.backend.common.error.BusinessException;
import com.backend.common.error.ErrorCode;
import com.backend.common.redis.RedisKeyUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class LoginAttemptLimiter {
    private static final Duration WINDOW = Duration.ofMinutes(10);
    private static final long MAX_PER_ID = 5;
    private static final long MAX_PER_IP = 20;
    private static final String ID_PREFIX = "login:fail:id:";
    private static final String IP_PREFIX = "login:fail:ip:";

    private final StringRedisTemplate redis;

    public void checkAllowed(String loginId) {
        if (count(idKey(loginId)) >= MAX_PER_ID || count(ipKey()) >= MAX_PER_IP) {
            throw new BusinessException(ErrorCode.TOO_MANY_REQUEST, "로그인 시도 횟수를 초과했습니다. 10분 후 다시 시도해주세요.");
        }
    }

    public void recordFailure(String loginId) {
        increase(idKey(loginId));
        increase(ipKey());
    }

    public void reset(String loginId) {
        redis.delete(idKey(loginId));
    }

    private long count(String key) {
        String v = redis.opsForValue().get(key);
        return v == null ? 0 : Long.parseLong(v);
    }

    private void increase(String key) {
        Long c = redis.opsForValue().increment(key);
        if(c != null && c == 1L){
            redis.expire(key, WINDOW);
        }
    }

    private String idKey(String loginId) {
        return ID_PREFIX + RedisKeyUtil.emailHash(loginId.trim().toLowerCase());
    }

    private String ipKey() {
        var attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        String ip = attrs == null ? "unknown" : attrs.getRequest().getRemoteAddr();
        return IP_PREFIX + ip;
    }
}
