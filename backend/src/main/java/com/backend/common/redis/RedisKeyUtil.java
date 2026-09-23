package com.backend.common.redis;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class RedisKeyUtil {

    private RedisKeyUtil() {}

    public static String emailHash(String email) {
        return sha256(normalizeEmail(email));
    }

    public static String tokenHash(String token) {
        return sha256(token);
    }

    public static String normalizeEmail(String email) {
        return email == null
                ? ""
                : email.trim().toLowerCase();
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));

            StringBuilder result = new StringBuilder();

            for (byte current : hash) {
                result.append(String.format("%02x", current));
            }

            return result.toString();

        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 알고리즘을 사용할 수 없습니다.", e);
        }
    }
}
