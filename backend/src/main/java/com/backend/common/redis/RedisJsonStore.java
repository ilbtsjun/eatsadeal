package com.backend.common.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisJsonStore {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public <T> void save(String key, T value, Duration ttl) {
        try {
            String json = objectMapper.writeValueAsString(value);

            redisTemplate.opsForValue().set(key, json, ttl);

        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Redis JSON 저장에 실패했습니다.", e);
        }
    }

    public <T> T find(String key, Class<T> type) {
        String json = redisTemplate.opsForValue().get(key);

        if (json == null) {
            return null;
        }

        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException e) {
            log.error("Redis JSON 변환 실패. key={}", key, e);

            redisTemplate.delete(key);

            return null;
        }
    }

    public void delete(String key) {
        redisTemplate.delete(key);
    }

    public boolean exists(String key) {
        return Boolean.TRUE.equals(
                redisTemplate.hasKey(key)
        );
    }
}
