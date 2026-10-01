package com.backend.crawler.controller;

import com.backend.common.error.BusinessException;
import com.backend.common.error.ErrorCode;
import com.backend.crawler.dto.CrawlTarget;
import com.backend.crawler.service.CrawlerService;
import com.backend.crawler.target.chicken.*;
import com.backend.crawler.target.hamburger.*;
import com.backend.crawler.target.pizza.*;
import com.backend.event.dto.CreateEvent;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/crawl")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Crawler", description = "관리자 수동 크롤링 API")
public class CrawlerController {

    private final CrawlerService crawlerService;

    @Operation(
            summary = "전체 크롤링",
            description = "전체 크롤러를 실행합니다.")
    @PostMapping
    public List<CreateEvent> crawlAll() {
        return crawlerService.crawlAll();
    }

    @Operation(summary = "카테고리 또는 브랜드 단위 크롤링",
                description = "chicken")
    @PostMapping("/{target}")
    public List<CreateEvent> crawl(@PathVariable String target) {
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