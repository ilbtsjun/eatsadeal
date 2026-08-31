package com.backend.crawler.common;

import com.backend.entity.EventCode;

import java.time.LocalDateTime;
import java.util.Set;

public record EventCreateDto(
        String title,
        String description,
        String url,
        String img,
        LocalDateTime startDate,
        LocalDateTime endDate,
        String brandName,
        Set<EventCode> eventCodes
) {
}
