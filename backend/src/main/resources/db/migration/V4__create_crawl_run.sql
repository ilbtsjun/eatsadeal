CREATE TABLE crawl_run (
                           id              BIGINT       NOT NULL AUTO_INCREMENT,
                           run_id          VARCHAR(36)  NOT NULL,
                           brand_id        BIGINT,
                           brand_key       VARCHAR(50)  NOT NULL,
                           status          ENUM ('SUCCESS', 'PARTIAL', 'EMPTY', 'FAILED', 'SKIPPED_INACTIVE') NOT NULL,
                           collected_count INT          NOT NULL,
                           error_count     INT          NOT NULL,
                           created_count   INT,
                           updated_count   INT,
                           unchanged_count INT,
                           message         VARCHAR(500),
                           started_at      DATETIME(6)  NOT NULL,
                           finished_at     DATETIME(6)  NOT NULL,
                           elapsed_ms      BIGINT       NOT NULL,
                           PRIMARY KEY (id),
                           INDEX idx_crawl_run_run_id (run_id),
                           INDEX idx_crawl_run_brand_started (brand_key, started_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;