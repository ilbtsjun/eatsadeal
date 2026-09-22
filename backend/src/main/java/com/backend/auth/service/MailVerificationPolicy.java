package com.backend.auth.service;

import com.backend.common.error.BusinessException;
import com.backend.common.error.ErrorCode;
import com.backend.common.redis.RedisKeyUtil;
import com.backend.common.redis.RedisRateLimiter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class MailVerificationPolicy {

    private static final String COOLDOWN_PREFIX = "mail:cooldown:";
    private static final String SEND_COUNT_PREFIX = "mail:send-count:";
    private static final String ATTEMPT_PREFIX = "mail:attempt:";

    private static final Duration RESEND_COOLDOWN = Duration.ofSeconds(60);
    private static final Duration SEND_LIMIT_WINDOW = Duration.ofMinutes(10);
    private static final Duration ATTEMPT_WINDOW = Duration.ofMinutes(10);

    private static final int MAX_SEND_COUNT = 5;
    private static final int MAX_ATTEMPT_COUNT = 5;

    private final RedisRateLimiter redisRateLimiter;

    public void validateSendingAllowed(String purpose, String email) {
        String emailHash = RedisKeyUtil.emailHash(email);
        String cooldownKey = COOLDOWN_PREFIX + purpose + ":" + emailHash;
        String sendCountKey = SEND_COUNT_PREFIX + purpose + ":" + emailHash;

        boolean cooldownAvailable = redisRateLimiter.tryCooldown(
                        cooldownKey,
                        RESEND_COOLDOWN
                );

        if (!cooldownAvailable) {
            throw new BusinessException(ErrorCode.TOO_MANY_REQUEST, "인증번호는 60초 후에 다시 요청할 수 있습니다.");
        }

        boolean countAvailable = redisRateLimiter.tryAcquire(
                        sendCountKey,
                        MAX_SEND_COUNT,
                        SEND_LIMIT_WINDOW
                );

        if (!countAvailable) {
            throw new BusinessException(ErrorCode.TOO_MANY_REQUEST, "인증번호 발송 횟수를 초과했습니다. 잠시 후 다시 시도해주세요.");
        }
    }

    public boolean isVerificationAttemptAllowed(String purpose, String email) {
        String key = createAttemptKey(purpose, email);

        long count = redisRateLimiter.increment(key, ATTEMPT_WINDOW);

        return count <= MAX_ATTEMPT_COUNT;
    }

    public void clearAttempts(String purpose, String email) {
        redisRateLimiter.delete(createAttemptKey(purpose, email));
    }

    private String createAttemptKey(String purpose, String email) {
        return ATTEMPT_PREFIX + purpose + ":" + RedisKeyUtil.emailHash(email);
    }
}
