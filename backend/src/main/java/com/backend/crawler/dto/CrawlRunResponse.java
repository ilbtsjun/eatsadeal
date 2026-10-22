package com.backend.crawler.dto;

import com.backend.crawler.entity.CrawlRun;
import com.backend.crawler.entity.CrawlStatus;

import java.time.LocalDateTime;

public record CrawlRunResponse(
        Long id,
        String runId,
        String brandKey,
        CrawlStatus status,
        int collectedCount,
        int errorCount,
        Integer createdCount,
        Integer updatedCount,
        Integer unchangedCount,
        String message,
        LocalDateTime startedAt,
        LocalDateTime finishedAt,
        long elapsedMs
) {
    public static CrawlRunResponse from(CrawlRun run) {
        return new CrawlRunResponse(
                run.getId(),
                run.getRunId(),
                run.getBrandKey(),
                run.getStatus(),
                run.getCollectedCount(),
                run.getErrorCount(),
                run.getCreatedCount(),
                run.getUpdatedCount(),
                run.getUnchangedCount(),
                run.getMessage(),
                run.getStartedAt(),
                run.getFinishedAt(),
                run.getElapsedMs());
    }
}
