package com.backend.report.service;

import com.backend.common.error.BusinessException;
import com.backend.common.error.ErrorCode;
import com.backend.common.redis.RedisRateLimiter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;


@Component
@RequiredArgsConstructor
public class ReportPolicy {

    private static final String SEND_COUNT_PREFIX = "report:send-count:";
    private static final String DUPLICATE_PREFIX = "report:duplicate:";

    private static final Duration SEND_LIMIT_WINDOW = Duration.ofMinutes(10);
    private static final Duration DUPLICATE_WINDOW = Duration.ofMinutes(10);

    private static final int MAX_REPORT_COUNT = 5;

    private final RedisRateLimiter redisRateLimiter;

    public void validateReportAllowed(String subject, String title, String content) {
        boolean countAvailable = redisRateLimiter.tryAcquire(
                SEND_COUNT_PREFIX + subject,
                MAX_REPORT_COUNT,
                SEND_LIMIT_WINDOW
        );

        if (!countAvailable) {
            throw new BusinessException(ErrorCode.TOO_MANY_REQUEST,
                    "제보는 10분에 최대 5건까지 보낼 수 있습니다. 잠시 후 다시 시도해주세요.");
        }

        boolean notDuplicated = redisRateLimiter.tryCooldown(
                DUPLICATE_PREFIX + subject + ":" + sha256(title + "\n" + content),
                DUPLICATE_WINDOW
        );

        if (!notDuplicated) {
            throw new BusinessException(ErrorCode.TOO_MANY_REQUEST, "동일한 내용의 제보가 이미 접수되었습니다.");
        }
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 알고리즘을 사용할 수 없습니다.", e);
        }
    }
}
