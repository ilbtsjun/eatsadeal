package com.backend.common.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;

@Component
@RequiredArgsConstructor
public class RedisRateLimiter {

    private final StringRedisTemplate redisTemplate;

    private static final DefaultRedisScript<Long> INCREMENT_SCRIPT =
            new DefaultRedisScript<>(
                    """
                    local current = redis.call('INCR', KEYS[1])

                    if current == 1 then
                        redis.call('EXPIRE', KEYS[1], ARGV[1])
                    end

                    return current
                    """,
                    Long.class
            );

    public boolean tryAcquire(String key, long maxCount, Duration window) {
        Long currentCount = redisTemplate.execute(INCREMENT_SCRIPT,
                Collections.singletonList(key),
                String.valueOf(window.toSeconds())
        );

        return currentCount != null && currentCount <= maxCount;
    }

    public boolean tryCooldown(String key, Duration cooldown) {
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(key, "1", cooldown);

        return Boolean.TRUE.equals(acquired);
    }

    public long increment(String key, Duration window) {
        Long currentCount = redisTemplate.execute(
                INCREMENT_SCRIPT,
                Collections.singletonList(key),
                String.valueOf(window.toSeconds())
        );

        return currentCount == null
                ? 0
                : currentCount;
    }

    public void delete(String key) {
        redisTemplate.delete(key);
    }
}
