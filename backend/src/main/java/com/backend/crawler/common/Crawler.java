package com.backend.crawler.common;

import com.backend.dto.EventCreateDto;

import java.util.List;

public interface Crawler {
    // 프랜차이즈 이름 반환 (예: "BHC", "Starbucks")
    String getName();
    // 크롤링 실행 및 이벤트 목록 반환 메서드
    List<EventCreateDto> crawl();
}