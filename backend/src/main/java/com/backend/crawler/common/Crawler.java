package com.backend.crawler.common;

import com.backend.brand.entity.Brand;

public interface Crawler {
    String getName();
    CrawlResult crawl(Brand brand);
}