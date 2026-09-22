package com.backend.crawler.common;

import com.backend.event.dto.CreateEvent;

import java.util.List;

public interface Crawler {
    String getName();
    List<CreateEvent> crawl();
}