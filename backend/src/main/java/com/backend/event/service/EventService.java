package com.backend.event.service;

import com.backend.auth.service.CurrentUserService;
import com.backend.common.dto.UserRole;
import com.backend.common.error.BusinessException;
import com.backend.common.error.ErrorCode;
import com.backend.common.log.CudLogging;
import com.backend.common.log.QueryTrace;
import com.backend.event.dto.EventCode;
import com.backend.event.dto.*;
import com.backend.brand.entity.Brand;
import com.backend.event.entity.Event;
import com.backend.event.repository.EventRepository;
import com.backend.favorite.repository.FavoriteRepository;
import com.backend.brand.repository.BrandRepository;
import com.backend.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventService {
    private final EventRepository eventRepository;
    private final BrandRepository brandRepository;
    private final FavoriteRepository favoriteRepository;
    private final CurrentUserService currentUserService;
    private final StringRedisTemplate redisTemplate;

    @Transactional
    @CudLogging("이벤트 생성")
    public void createEvent(CreateEvent rawRequest){
        CreateEvent request = withNormalizedUrl(rawRequest);
        validateDateRange(request.startDate(), request.endDate());
        if(eventRepository.existsByUrl(request.url())){
            throw new BusinessException(ErrorCode.ALREADY_EXISTS);
        }
        Brand brand = brandRepository.findById(request.brandId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        Event event = Event.builder()
                .title(request.title())
                .description(request.description())
                .url(request.url())
                .img(request.img())
                .startDate(request.startDate())
                .endDate(request.endDate())
                .brand(brand)
                .isActive(request.isActive())
                .eventCodes(request.eventCodes())
                .build();
        event.changeAdminLocked(true);
        eventRepository.save(event);
    }

    @Transactional
    @CudLogging("이벤트 생성(크롤러)")
    public UpsertResult upsertCrawledEvent(CreateEvent rawRequest) {
        CreateEvent request = withNormalizedUrl(rawRequest);
        boolean datesUnknown = request.startDate() == null;
        if (!datesUnknown) {
            validateDateRange(request.startDate(), request.endDate());
        }

        Optional<Event> optionalEvent = eventRepository.findByUrl(request.url());
        if (optionalEvent.isEmpty() && !request.url().equals(rawRequest.url())) {
            optionalEvent = eventRepository.findByUrl(rawRequest.url());
        }

        if (optionalEvent.isPresent()) {
            Event event = optionalEvent.get();

            if (event.isAdminLocked()) {
                return UpsertResult.UNCHANGED;
            }

            if (datesUnknown) {
                boolean contentChanged = event.updateContent(
                        request.title(),
                        request.description(),
                        request.url(),
                        request.img());
                boolean periodChanged = event.applyDefaultPeriod(
                        CreateEvent.DEFAULT_PERIOD_DAYS, request.isActive());
                return contentChanged || periodChanged ? UpsertResult.UPDATED : UpsertResult.UNCHANGED;
            }

            List<String> changedFields = event.changedFields(
                    request.title(), request.description(), request.url(), request.img(),
                    request.startDate(), request.endDate(), request.isActive());
            boolean changed = event.update(
                    request.title(),
                    request.description(),
                    request.url(),
                    request.img(),
                    request.startDate(),
                    request.endDate(),
                    request.isActive()
            );
            if (changed) {
                log.info("[EVENT UPDATED] url={} fields={}", request.url(), changedFields);
            }

            return changed ? UpsertResult.UPDATED : UpsertResult.UNCHANGED;
        }

        Brand brand = brandRepository.findById(request.brandId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        LocalDateTime startDate = datesUnknown ? LocalDateTime.now() : request.startDate();
        LocalDateTime endDate = datesUnknown ? startDate.plusDays(CreateEvent.DEFAULT_PERIOD_DAYS) : request.endDate();

        Event event = Event.builder()
                .title(request.title())
                .description(request.description())
                .url(request.url())
                .img(request.img())
                .startDate(startDate)
                .endDate(endDate)
                .brand(brand)
                .isActive(request.isActive())
                .eventCodes(request.eventCodes())
                .build();

        eventRepository.save(event);
        return UpsertResult.CREATED;
    }


    @Transactional(readOnly = true)
    @QueryTrace("searchEvents")
    public Page<GetEventListResponse> searchEvents(EventSearchRequest request) {
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
        User user = currentUserService.getOptionalUser();
        boolean isAdmin = user != null && user.getRole() == UserRole.ADMIN;
        Event event = (isAdmin
                ? eventRepository.findAnyById(eventId)
                : eventRepository.findVisibleById(eventId))
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        increaseViewCount(eventId);
        return toDetailResponse(user, event);
    }

    @Transactional
    @CudLogging("이벤트 수정")
    public void updateEvent(Long eventId, UpdateEvent request){
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        if (StringUtils.hasText(request.url()) && eventRepository.existsByUrlAndIdNot(request.url(), eventId)) {
            throw new BusinessException(ErrorCode.ALREADY_EXISTS);
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

        event.changeAdminLocked(true);
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
    @CudLogging("이벤트 비활성화")
    public void deactivateEvent(Long eventId){
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        event.deactivate();
        event.changeAdminLocked(true);
    }

    @Transactional
    @CudLogging("이벤트 숨김 변경")
    public void changeHidden(Long eventId, boolean hidden){
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        event.changeHidden(hidden);
    }

    @Transactional
    @CudLogging("이벤트 크롤러 잠금 변경")
    public void changeAdminLocked(Long eventId, boolean adminLocked){
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        event.changeAdminLocked(adminLocked);
    }

    @Transactional(readOnly = true)
    public List<GetEventCodeListResponse> getEventCodes(){
        return Arrays.stream(EventCode.values())
                .map(GetEventCodeListResponse::from)
                .toList();
    }

    public boolean isFirstView(Long eventId, String ip) {
        String key = "event:view:" + eventId + ":" + ip;

        Boolean result = false;

        try {
            result = redisTemplate.opsForValue().setIfAbsent(key, "1", Duration.ofHours(3));
        } catch (Exception e) {
            log.warn("Redis 장애 발생으로 조회수 증가를 처리하지 못했습니다: {}", e.getMessage());
        }

        return Boolean.TRUE.equals(result);
    }


    private void increaseViewCount(Long eventId){
        String ip = currentUserService.getClientIp();
        if (isFirstView(eventId, ip)) {
            eventRepository.increaseViewCount(eventId);
        }
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
                Set.copyOf(event.getEventCodes())
        );
    }

    private GetEventResponse toDetailResponse(User user, Event event) {
        Brand brand = event.getBrand();
        boolean isFavorite = false;
        if (user != null) {
            isFavorite = favoriteRepository.findByUserAndEvent(user, event).isPresent();
        }
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
                event.getEventCodes(),
                isFavorite,
                event.isHidden(),
                event.isAdminLocked()
        );
    }

    private CreateEvent withNormalizedUrl(CreateEvent request) {
        String normalizedUrl = EventUrlNormalizer.normalize(request.url());
        if (normalizedUrl == null || normalizedUrl.equals(request.url())) {
            return request;
        }
        return new CreateEvent(
                request.title(), request.description(), normalizedUrl, request.img(),
                request.startDate(), request.endDate(), request.brandId(),
                request.isActive(), request.eventCodes());
    }

    private void validateDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "시작일은 필수입니다.");
        }
        if (endDate != null && endDate.isBefore(startDate)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "종료일은 시작일보다 빠를 수 없습니다.");
        }
    }
}