package com.backend.crawler.repository;

import com.backend.crawler.entity.CrawlRun;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CrawlRunRepository extends JpaRepository<CrawlRun, Long> {
    Optional<CrawlRun> findTopByOrderByIdDesc();
    List<CrawlRun> findByRunIdOrderByIdAsc(String runId);
    List<CrawlRun> findAllByOrderByIdDesc(Pageable pageable);
}
