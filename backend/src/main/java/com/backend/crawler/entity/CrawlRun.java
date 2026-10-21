package com.backend.crawler.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "crawl_run")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CrawlRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 36)
    private String runId;

    private Long brandId;

    @Column(nullable = false, length = 50)
    private String brandKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CrawlStatus status;

    @Column(nullable = false)
    private int collectedCount;

    @Column(nullable = false)
    private int errorCount;

    private Integer createdCount;
    private Integer updatedCount;
    private Integer unchangedCount;

    @Column(length = 500)
    private String message;

    @Column(nullable = false)
    private LocalDateTime startedAt;

    @Column(nullable = false)
    private LocalDateTime finishedAt;

    @Column(nullable = false)
    private long elapsedMs;

    public static CrawlRun of(String runId, Long brandId, String brandKey, CrawlStatus status,
                              int collectedCount, int errorCount, String message,
                              LocalDateTime startedAt, LocalDateTime finishedAt) {
        CrawlRun run = new CrawlRun();
        run.runId = runId;
        run.brandId = brandId;
        run.brandKey = brandKey;
        run.status = status;
        run.collectedCount = collectedCount;
        run.errorCount = errorCount;
        run.message = message == null || message.length() <= 500 ? message : message.substring(0, 500);
        run.startedAt = startedAt;
        run.finishedAt = finishedAt;
        run.elapsedMs = Duration.between(startedAt, finishedAt).toMillis();
        return run;
    }
}