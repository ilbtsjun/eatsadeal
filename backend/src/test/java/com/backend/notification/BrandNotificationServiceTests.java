package com.backend.notification;

import com.backend.brand.entity.Brand;
import com.backend.brand.repository.BrandSubscriptionRepository;
import com.backend.event.dto.GetEventListResponse;
import com.backend.event.entity.Event;
import com.backend.event.repository.EventRepository;
import com.backend.notification.dto.EventDigest;
import com.backend.notification.dto.NotificationTarget;
import com.backend.notification.service.BrandNotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BrandNotificationServiceTests {

    private static final LocalDateTime RUN_AT = LocalDateTime.of(2026, 11, 6, 9, 0, 3);
    private static final LocalDateTime FROM = LocalDateTime.of(2026, 11, 5, 9, 0);
    private static final LocalDateTime TO = LocalDateTime.of(2026, 11, 6, 9, 0);
    private static final LocalDateTime SOON_FROM = LocalDateTime.of(2026, 11, 7, 0, 0);
    private static final LocalDateTime SOON_TO = LocalDateTime.of(2026, 11, 8, 0, 0);

    @Mock
    private EventRepository eventRepository;

    @Mock
    private BrandSubscriptionRepository brandSubscriptionRepository;

    @InjectMocks
    private BrandNotificationService brandNotificationService;

    private Brand buildBrand(Long id, String name) {
        Brand brand = Brand.builder()
                .name(name)
                .url("https://www." + name.toLowerCase() + ".co.kr")
                .img("https://image.url/" + name.toLowerCase() + ".png")
                .build();
        ReflectionTestUtils.setField(brand, "id", id);
        return brand;
    }

    private Event buildEvent(Long id, Brand brand, LocalDateTime createdAt, LocalDateTime endDate) {
        Event event = Event.builder()
                .title("이벤트 " + id)
                .url("https://event.url/" + id)
                .img("https://image.url/event" + id + ".png")
                .startDate(createdAt)
                .endDate(endDate)
                .brand(brand)
                .isActive(true)
                .build();
        ReflectionTestUtils.setField(event, "id", id);
        ReflectionTestUtils.setField(event, "createdAt", createdAt);
        return event;
    }

    private NotificationTarget target(Long userId, Long brandId) {
        return new NotificationTarget(userId, "user" + userId + "@test.com", "회원" + userId, brandId);
    }

    private void givenTargets(NotificationTarget... targets) {
        when(brandSubscriptionRepository.findNotificationTargets()).thenReturn(List.of(targets));
    }

    private void givenEvents(List<Long> brandIds, Event... events) {
        when(eventRepository.findDigestEvents(brandIds, FROM, TO, SOON_FROM, SOON_TO)).thenReturn(List.of(events));
    }

    private List<Long> idsOf(List<GetEventListResponse> events) {
        return events.stream().map(GetEventListResponse::id).toList();
    }

    @Nested
    @DisplayName("buildDigests")
    class BuildDigestsTests {

        @Test
        @DisplayName("대상이 없으면 빈 목록을 반환하고 이벤트는 조회하지 않는다")
        void noTargets() {
            givenTargets();

            List<EventDigest> digests = brandNotificationService.buildDigests(RUN_AT);

            assertTrue(digests.isEmpty());
            verify(eventRepository, never()).findDigestEvents(anyList(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("오늘 9시 기준 시간 범위와 중복 없는 브랜드 ID로 이벤트를 조회한다")
        void queriesWithTimeRangeAndDistinctBrandIds() {
            givenTargets(target(1L, 1L), target(1L, 2L), target(2L, 1L));
            givenEvents(List.of(1L, 2L));

            List<EventDigest> digests = brandNotificationService.buildDigests(RUN_AT);

            assertTrue(digests.isEmpty());
            verify(eventRepository, times(1)).findDigestEvents(List.of(1L, 2L), FROM, TO, SOON_FROM, SOON_TO);
        }

        @Test
        @DisplayName("회원별로 묶어 각자 구독한 브랜드의 이벤트만 담는다")
        void groupsByUser() {
            Brand bhc = buildBrand(1L, "BHC");
            Brand domino = buildBrand(2L, "Domino");
            givenTargets(target(1L, 1L), target(1L, 2L), target(2L, 1L));
            givenEvents(List.of(1L, 2L),
                    buildEvent(10L, bhc, FROM.plusHours(1), null),
                    buildEvent(20L, domino, FROM.plusHours(2), null));

            List<EventDigest> digests = brandNotificationService.buildDigests(RUN_AT);

            assertEquals(2, digests.size());
            EventDigest first = digests.get(0);
            assertEquals(1L, first.userId());
            assertEquals("user1@test.com", first.email());
            assertEquals("회원1", first.nickname());
            assertEquals(List.of(10L, 20L), idsOf(first.newEvents()));
            EventDigest second = digests.get(1);
            assertEquals(2L, second.userId());
            assertEquals(List.of(10L), idsOf(second.newEvents()));
        }

        @Test
        @DisplayName("createdAt이 어제 9시 이상 오늘 9시 미만이면 새 이벤트, 아니면 마감 임박으로 나눈다")
        void splitsNewAndEndingSoon() {
            Brand bhc = buildBrand(1L, "BHC");
            LocalDateTime tomorrowEnd = SOON_FROM.plusHours(23);
            givenTargets(target(1L, 1L));
            givenEvents(List.of(1L),
                    buildEvent(10L, bhc, FROM, tomorrowEnd),
                    buildEvent(20L, bhc, FROM.minusSeconds(1), tomorrowEnd),
                    buildEvent(30L, bhc, TO, tomorrowEnd));

            EventDigest digest = brandNotificationService.buildDigests(RUN_AT).get(0);

            assertEquals(List.of(10L), idsOf(digest.newEvents()));
            assertEquals(List.of(20L, 30L), idsOf(digest.endingSoonEvents()));
            assertEquals(3, digest.totalCount());
        }

        @Test
        @DisplayName("보낼 이벤트가 없는 회원은 결과에서 뺀다")
        void excludesUserWithoutEvents() {
            Brand bhc = buildBrand(1L, "BHC");
            givenTargets(target(1L, 1L), target(2L, 2L));
            givenEvents(List.of(1L, 2L), buildEvent(10L, bhc, FROM.plusHours(1), null));

            List<EventDigest> digests = brandNotificationService.buildDigests(RUN_AT);

            assertEquals(1, digests.size());
            assertEquals(1L, digests.get(0).userId());
        }
    }
}
