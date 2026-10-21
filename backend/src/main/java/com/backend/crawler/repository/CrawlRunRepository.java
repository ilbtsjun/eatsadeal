package com.backend.crawler.repository;

import com.backend.crawler.entity.CrawlRun;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CrawlRunRepository extends JpaRepository<CrawlRun, Long> {
}