package com.backend.notification.service;

import com.backend.brand.repository.BrandSubscriptionRepository;
import com.backend.event.dto.GetEventListResponse;
import com.backend.event.entity.Event;
import com.backend.event.repository.EventRepository;
import com.backend.notification.dto.EventDigest;
import com.backend.notification.dto.NotificationTarget;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BrandNotificationService {
    private final EventRepository eventRepository;
    private final BrandSubscriptionRepository brandSubscriptionRepository;

    @Transactional(readOnly = true)
    public List<EventDigest> buildDigests(LocalDateTime runAt) {
        List<NotificationTarget> targets = brandSubscriptionRepository.findNotificationTargets();
        if (targets.isEmpty()) {
            return List.of();
        }
        List<Long> brandIds = targets.stream()
                .map(NotificationTarget::brandId)
                .distinct()
                .toList();
        LocalDateTime to = runAt.toLocalDate().atTime(9, 0);
        LocalDateTime from = to.minusDays(1);
        LocalDateTime soonFrom = to.toLocalDate().plusDays(1).atStartOfDay();
        LocalDateTime soonTo = soonFrom.plusDays(1);
        List<Event> events = eventRepository.findDigestEvents(brandIds, from, to, soonFrom, soonTo);

        Map<Long, List<GetEventListResponse>> newByBrand = new HashMap<>();
        Map<Long, List<GetEventListResponse>> soonByBrand = new HashMap<>();
        for (Event event : events) {
            boolean isNew = !event.getCreatedAt().isBefore(from) && event.getCreatedAt().isBefore(to);
            Map<Long, List<GetEventListResponse>> bucket = isNew ? newByBrand : soonByBrand;
            bucket.computeIfAbsent(event.getBrand().getId(), id -> new ArrayList<>())
                    .add(GetEventListResponse.from(event));
        }

        Map<Long, List<NotificationTarget>> targetsByUser = targets.stream()
                .collect(Collectors.groupingBy(NotificationTarget::userId, LinkedHashMap::new, Collectors.toList()));

        List<EventDigest> digests = new ArrayList<>();
        for (List<NotificationTarget> userTargets : targetsByUser.values()) {
            List<GetEventListResponse> newEvents = new ArrayList<>();
            List<GetEventListResponse> soonEvents = new ArrayList<>();
            for (NotificationTarget target : userTargets) {
                newEvents.addAll(newByBrand.getOrDefault(target.brandId(), List.of()));
                soonEvents.addAll(soonByBrand.getOrDefault(target.brandId(), List.of()));
            }
            NotificationTarget user = userTargets.get(0);
            EventDigest digest = new EventDigest(user.userId(), user.email(), user.nickname(), newEvents, soonEvents);
            if (!digest.isEmpty()) {
                digests.add(digest);
            }
        }
        return digests;
    }
}
