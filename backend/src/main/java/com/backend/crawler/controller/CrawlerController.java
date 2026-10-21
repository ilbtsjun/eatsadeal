package com.backend.crawler.controller;

import com.backend.common.error.BusinessException;
import com.backend.common.error.ErrorCode;
import com.backend.crawler.dto.CrawlReport;
import com.backend.crawler.dto.CrawlRunResponse;
import com.backend.crawler.dto.CrawlTarget;
import com.backend.crawler.service.CrawlerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/crawl")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Crawler", description = "관리자 수동 크롤링 API")
public class CrawlerController {

    private final CrawlerService crawlerService;

    @Operation(summary = "최근 크롤링 결과",
            description = "가장 최근에 실행한 크롤링 1회의 브랜드별 결과를 반환합니다.")
    @GetMapping("/runs/latest")
    public List<CrawlRunResponse> latestRuns() {
        return crawlerService.getLatestRuns();
    }

    @Operation(summary = "크롤링 실행 이력",
            description = "브랜드별 실행 이력을 최신순으로 반환합니다. limit은 1~200입니다.")
    @GetMapping("/runs")
    public List<CrawlRunResponse> recentRuns(@RequestParam(defaultValue = "50") int limit) {
        return crawlerService.getRecentRuns(limit);
    }

    @Operation(
            summary = "전체 크롤링",
            description = "전체 크롤러를 실행합니다. 비활성 브랜드는 건너뜁니다.")
    @PostMapping
    public CrawlReport crawlAll() {
        return crawlerService.crawlAll();
    }

    @Operation(summary = "카테고리 또는 브랜드 단위 크롤링",
            description = "카테고리(chicken 등)는 비활성 브랜드를 건너뛰고, 브랜드 단건은 비활성이면 409를 반환합니다.")
    @PostMapping("/{target}")
    public CrawlReport crawl(@PathVariable String target) {
        if (CrawlTarget.isGroup(target)) {
            return crawlerService.crawlGroup(target);
        }
        CrawlTarget brand = CrawlTarget.fromKey(target);
        if (brand == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "지원하지 않는 크롤링 대상입니다: " + target);
        }
        return crawlerService.crawlOne(brand);
    }
}