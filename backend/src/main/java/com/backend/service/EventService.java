package com.backend.service;

import com.backend.dto.CreateEvent;
import com.backend.dto.GetEventListResponse;
import com.backend.dto.GetEventResponse;
import com.backend.dto.GetSearch;
import com.backend.entity.Brand;
import com.backend.entity.Event;
import com.backend.repository.BrandRepository;
import com.backend.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventService {
    private final EventRepository eventRepository;
    private final BrandRepository brandRepository;

    @Transactional
    public void createEvent(CreateEvent createEvent){
        if(eventRepository.existsByUrl(createEvent.url())){
            return;
        }
        Brand brand = brandRepository.findByName(createEvent.brandName());
        if(brand == null){
            throw new IllegalArgumentException("브랜드가 없습니다.");
        }
        Event event = Event.builder()
                .title(createEvent.title())
                .description(createEvent.description())
                .url(createEvent.url())
                .img(createEvent.img())
                .startDate(createEvent.startDate())
                .endDate(createEvent.endDate())
                .brand(brand)
                .eventCodes(createEvent.eventCodes())
                .build();
        eventRepository.save(event);
    }

    @Transactional(readOnly = true)
    public Page<GetEventListResponse> searchEvents(GetSearch request) {
        Pageable pageable = createPageable(request.sort(), request.page(), request.size());
        String normalizedKeyword = StringUtils.hasText(request.keyword())
                        ? request.keyword().trim()
                        : null;
        Page<Event> events = eventRepository.searchEvents(
                request.brandId(),
                request.categoryId(),
                request.eventCode(),
                normalizedKeyword,
                LocalDateTime.now(),
                pageable
        );
        return events.map(this::toListResponse);
    }

    @Transactional
    public GetEventResponse getEvent(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("이벤트를 찾을 수 없습니다."));
        eventRepository.increaseViewCount(eventId);
        return toDetailResponse(event);
    }

    private Pageable createPageable(String sort, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Sort sortCondition = switch (sort == null ? "latest" : sort) {
            case "popular" -> Sort.by(Sort.Order.desc("viewCount"), Sort.Order.desc("id"));
            case "endingSoon" -> Sort.by(Sort.Order.asc("endDate"), Sort.Order.desc("id"));
            case "oldest" -> Sort.by(Sort.Order.asc("startDate"), Sort.Order.asc("id"));
            default -> Sort.by(Sort.Order.desc("startDate"), Sort.Order.desc("id"));
        };
        return PageRequest.of(safePage, safeSize, sortCondition);
    }

    private GetEventListResponse toListResponse(Event event) {
        Brand brand = event.getBrand();
        return new GetEventListResponse(
                event.getId(),
                event.getTitle(),
                event.getDescription(),
                event.getUrl(),
                event.getImg(),
                event.getStartDate(),
                event.getEndDate(),
                event.getViewCount(),
                event.getIsActive(),
                brand.getId(),
                brand.getName(),
                event.getEventCodes()
        );
    }

    private GetEventResponse toDetailResponse(Event event) {
        Brand brand = event.getBrand();
        return new GetEventResponse(
                event.getId(),
                event.getTitle(),
                event.getDescription(),
                event.getUrl(),
                event.getImg(),
                event.getStartDate(),
                event.getEndDate(),
                event.getViewCount(),
                event.getIsActive(),
                brand.getId(),
                brand.getName(),
                brand.getImg(),
                event.getEventCodes()
        );
    }
}