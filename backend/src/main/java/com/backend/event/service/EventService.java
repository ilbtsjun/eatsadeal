package com.backend.event.service;

import com.backend.event.dto.EventCode;
import com.backend.event.dto.*;
import com.backend.entity.Brand;
import com.backend.event.entity.Event;
import com.backend.event.repository.EventRepository;
import com.backend.repository.BrandRepository;
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
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventService {
    private final EventRepository eventRepository;
    private final BrandRepository brandRepository;

    @Transactional
    public void createEvent(CreateEvent request){
        validateDateRange(request.startDate(), request.endDate());
        if(eventRepository.existsByUrl(request.url())){
            throw new IllegalArgumentException("이미 존재하는 이벤트 URL입니다.");
        }
        Brand brand = brandRepository.findById(request.brandId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 브랜드입니다."));
        Event event = Event.builder()
                .title(request.title())
                .description(request.description())
                .url(request.url())
                .img(request.img())
                .startDate(request.startDate())
                .endDate(request.endDate())
                .brand(brand)
                .eventCodes(request.eventCodes())
                .build();
        eventRepository.save(event);
    }

    @Transactional
    public void upsertCrawledEvent(CreateEvent request) {
        validateDateRange(request.startDate(), request.endDate());

        Optional<Event> optionalEvent = eventRepository.findByUrl(request.url());

        if (optionalEvent.isPresent()) {
            Event event = optionalEvent.get();

            event.update(
                    request.title(),
                    request.description(),
                    request.url(),
                    request.img(),
                    request.startDate(),
                    request.endDate(),
                    true
            );

            return;
        }

        Brand brand = brandRepository.findById(request.brandId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 브랜드입니다."));

        Event event = Event.builder()
                .title(request.title())
                .description(request.description())
                .url(request.url())
                .img(request.img())
                .startDate(request.startDate())
                .endDate(request.endDate())
                .brand(brand)
                .eventCodes(request.eventCodes())
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

    @Transactional
    public void updateEvent(Long eventId, UpdateEvent request){
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("이벤트를 찾을 수 없습니다."));

        if (StringUtils.hasText(request.url()) && eventRepository.existsByUrlAndIdNot(request.url(), eventId)) {
            throw new IllegalArgumentException("이미 존재하는 URL입니다.");
        }

        String newTitle = StringUtils.hasText(request.title())
                ? request.title()
                : event.getTitle();
        String newDescription = StringUtils.hasText(request.description())
                ? request.description()
                : event.getDescription();
        String newUrl = StringUtils.hasText(request.url())
                ? request.url()
                : event.getUrl();
        String newImg = StringUtils.hasText(request.img())
                ? request.img()
                : event.getImg();
        LocalDateTime newStartDate = request.startDate() == null
                ? event.getStartDate()
                : request.startDate();
        LocalDateTime newEndDate = request.endDate() == null
                ? event.getEndDate()
                : request.endDate();
        Boolean newIsActive = request.isActive() == null
                ? event.getIsActive()
                : request.isActive();

        validateDateRange(newStartDate, newEndDate);

        event.update(newTitle, newDescription, newUrl, newImg, newStartDate, newEndDate, newIsActive);

        if (request.eventCodes() != null) {
            Set<EventCode> requestedCodes = request.eventCodes();

            Set<EventCode> codesToAdd = new HashSet<>(requestedCodes);
            codesToAdd.removeAll(event.getEventCodes());

            Set<EventCode> codesToRemove = new HashSet<>(event.getEventCodes());
            codesToRemove.removeAll(requestedCodes);

            codesToAdd.forEach(event::addEventCode);
            codesToRemove.forEach(event::removeEventCode);
        }
    }

    @Transactional
    public void deactivateEvent(Long eventId){
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("이벤트를 찾을 수 없습니다."));
        event.deactivate();
    }

    @Transactional(readOnly = true)
    public List<GetEventCodeListResponse> getEventCodes(){
        return Arrays.stream(EventCode.values())
                .map(GetEventCodeListResponse::from)
                .toList();
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

    private void validateDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate == null) {
            throw new IllegalArgumentException("시작일은 필수입니다.");
        }
        if (endDate != null && endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("종료일은 시작일보다 빠를 수 없습니다.");
        }
    }
}