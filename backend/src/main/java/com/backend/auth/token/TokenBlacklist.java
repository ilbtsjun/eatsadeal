package com.backend.auth.token;

import java.time.Duration;
import java.util.Date;

import com.backend.common.redis.RedisKeyUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TokenBlacklist {

    private final StringRedisTemplate redisTemplate;

    private static final String PREFIX = "jwt:blacklist:";

    public void add(String token, Date expiration) {
        if(token == null || expiration == null){
            return;
        }

        long remainingMillis = expiration.getTime() - System.currentTimeMillis();

        if (remainingMillis <= 0) {
            return;
        }

        redisTemplate.opsForValue().set(
                createKey(token),
                "blacklisted",
                Duration.ofMillis(remainingMillis)
        );
    }

    public boolean contains(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        return Boolean.TRUE.equals(redisTemplate.hasKey(createKey(token)));
    }

    private String createKey(String token) {
        return PREFIX + RedisKeyUtil.tokenHash(token);
    }
}