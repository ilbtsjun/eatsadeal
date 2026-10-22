package com.backend.crawler.common;

import com.backend.event.dto.CreateEvent;

import java.util.List;

public record CrawlResult(List<CreateEvent> events, int errors, String firstError) {
    public static CrawlResult of(List<CreateEvent> events, CrawlErrors errors) {
        return new CrawlResult(events, errors.count(), errors.first());
    }
}