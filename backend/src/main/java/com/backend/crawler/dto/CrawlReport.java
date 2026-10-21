package com.backend.crawler.dto;

import java.util.List;

public record CrawlReport(
        int saved,
        List<String> skipped,
        List<String> failed,
        List<String> partial,
        List<String> empty
) {}