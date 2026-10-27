package com.backend.admin;

import com.backend.admin.dto.AdminDashboardResponse;
import com.backend.admin.dto.AdminDashboardResponse.BrandEventCount;
import com.backend.admin.dto.AdminDashboardResponse.CrawlStats;
import com.backend.admin.dto.AdminDashboardResponse.EventStats;
import com.backend.admin.dto.AdminDashboardResponse.MemberStats;
import com.backend.admin.dto.AdminDashboardResponse.TopEvent;
import com.backend.admin.service.AdminDashboardService;
import com.backend.brand.entity.Brand;
import com.backend.comment.entity.CommentStatus;
import com.backend.comment.repository.CommentRepository;
import com.backend.crawler.dto.CrawlRunResponse;
import com.backend.crawler.entity.CrawlStatus;
import com.backend.crawler.service.CrawlerService;
import com.backend.event.entity.Event;
import com.backend.event.repository.EventRepository;
import com.backend.event.service.AnthropicEventCodeAi;
import com.backend.user.dto.UserStatus;
import com.backend.user.repository.UserRepository;
import jakarta.persistence.Tuple;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminDashboardServiceTests {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private CrawlerService crawlerService;

    @Mock
    private AnthropicEventCodeAi eventCodeAi;

    @InjectMocks
    private AdminDashboardService adminDashboardService;

    private CrawlRunResponse run(String brandKey, CrawlStatus status, LocalDateTime finishedAt) {
        return new CrawlRunResponse(null, "run-1", brandKey, status, 0, 0, null, null, null, null,
                null, finishedAt, 0L);
    }

    private Event mockEvent(Long id, String title, String brandName, Long viewCount) {
        Brand brand = mock(Brand.class);
        when(brand.getName()).thenReturn(brandName);
        Event event = mock(Event.class);
        when(event.getId()).thenReturn(id);
        when(event.getTitle()).thenReturn(title);
        when(event.getBrand()).thenReturn(brand);
        when(event.getViewCount()).thenReturn(viewCount);
        return event;
    }

    private Tuple brandRow(String brandName, Long eventCount) {
        Tuple row = mock(Tuple.class);
        when(row.get("brandName", String.class)).thenReturn(brandName);
        when(row.get("eventCount", Long.class)).thenReturn(eventCount);
        return row;
    }

    @Nested
    @DisplayName("이벤트 지표")
    class EventStatsTests {

        @Test
        @DisplayName("repository 집계값을 그대로 담는다")
        void countsAreMapped() {
            when(eventRepository.countVisible(any())).thenReturn(120L);
            when(eventRepository.countByIsActiveTrueAndHiddenTrue()).thenReturn(4L);
            when(eventRepository.countByIsActiveTrueAndAdminLockedTrue()).thenReturn(2L);
            when(eventRepository.countVisibleStartedSince(any(), any())).thenReturn(15L);
            when(eventRepository.countVisibleEndingBetween(any(), any())).thenReturn(7L);
            when(eventRepository.countActiveWithoutEventCode()).thenReturn(9L);
            when(eventRepository.countActiveWithoutDescription()).thenReturn(3L);

            EventStats stats = adminDashboardService.getDashboard().events();

            assertEquals(new EventStats(120L, 4L, 2L, 15L, 7L, 9L, 3L), stats);
        }

        @Test
        @DisplayName("최근 시작은 7일 전부터, 마감 임박은 지금부터 3일 뒤까지로 조회한다")
        void dateRanges() {
            adminDashboardService.getDashboard();

            ArgumentCaptor<LocalDateTime> since = ArgumentCaptor.forClass(LocalDateTime.class);
            ArgumentCaptor<LocalDateTime> startedNow = ArgumentCaptor.forClass(LocalDateTime.class);
            verify(eventRepository).countVisibleStartedSince(since.capture(), startedNow.capture());
            assertEquals(Duration.ofDays(7), Duration.between(since.getValue(), startedNow.getValue()));

            ArgumentCaptor<LocalDateTime> from = ArgumentCaptor.forClass(LocalDateTime.class);
            ArgumentCaptor<LocalDateTime> until = ArgumentCaptor.forClass(LocalDateTime.class);
            verify(eventRepository).countVisibleEndingBetween(from.capture(), until.capture());
            assertEquals(Duration.ofDays(3), Duration.between(from.getValue(), until.getValue()));
        }
    }

    @Nested
    @DisplayName("크롤링 지표")
    class CrawlStatsTests {

        @Test
        @DisplayName("FAILED·EMPTY는 실패, SUCCESS·PARTIAL은 성공으로 세고 SKIPPED_INACTIVE는 세지 않는다")
        void statusesAreCounted() {
            LocalDateTime time = LocalDateTime.of(2026, 10, 27, 6, 0);
            when(crawlerService.getLatestRuns()).thenReturn(List.of(
                    run("bhc", CrawlStatus.SUCCESS, time),
                    run("bbq", CrawlStatus.PARTIAL, time),
                    run("kyochon", CrawlStatus.FAILED, time),
                    run("pizzahut", CrawlStatus.EMPTY, time),
                    run("lotteria", CrawlStatus.SKIPPED_INACTIVE, time)));

            CrawlStats stats = adminDashboardService.getDashboard().crawl();

            assertEquals(2, stats.successCount());
            assertEquals(2, stats.failedCount());
            assertEquals(List.of("kyochon", "pizzahut"), stats.failedBrands());
        }

        @Test
        @DisplayName("마지막 크롤링 시각은 끝난 시각 중 가장 늦은 값이고, 끝나지 않은 실행은 건너뛴다")
        void lastRunAtIsLatestFinishedAt() {
            LocalDateTime earlier = LocalDateTime.of(2026, 10, 27, 6, 0);
            LocalDateTime later = LocalDateTime.of(2026, 10, 27, 6, 5);
            when(crawlerService.getLatestRuns()).thenReturn(List.of(
                    run("bhc", CrawlStatus.SUCCESS, earlier),
                    run("bbq", CrawlStatus.FAILED, null),
                    run("kyochon", CrawlStatus.SUCCESS, later)));

            assertEquals(later, adminDashboardService.getDashboard().crawl().lastRunAt());
        }

        @Test
        @DisplayName("크롤링 기록이 없으면 마지막 시각은 null, 개수는 0이다")
        void noRuns() {
            when(crawlerService.getLatestRuns()).thenReturn(List.of());

            CrawlStats stats = adminDashboardService.getDashboard().crawl();

            assertNull(stats.lastRunAt());
            assertEquals(0, stats.successCount());
            assertEquals(0, stats.failedCount());
            assertTrue(stats.failedBrands().isEmpty());
        }

        @Test
        @DisplayName("오늘 AI 사용량과 하루 상한을 담는다")
        void aiUsage() {
            when(eventCodeAi.usedToday()).thenReturn(37);
            when(eventCodeAi.dailyLimit()).thenReturn(200);

            CrawlStats stats = adminDashboardService.getDashboard().crawl();

            assertEquals(37, stats.aiCallsToday());
            assertEquals(200, stats.aiDailyLimit());
        }
    }

    @Nested
    @DisplayName("회원·댓글 지표")
    class MemberStatsTests {

        @Test
        @DisplayName("회원 수, 정지 회원 수, 삭제되지 않은 댓글 수를 담는다")
        void countsAreMapped() {
            when(userRepository.count()).thenReturn(500L);
            when(userRepository.countByCreatedAtGreaterThanEqual(any())).thenReturn(12L);
            when(userRepository.countByUserStatus(UserStatus.SUSPEND)).thenReturn(3L);
            when(commentRepository.countByCommentStatusNot(CommentStatus.DELETED)).thenReturn(800L);
            when(commentRepository.countByCommentStatusNotAndCreatedAtGreaterThanEqual(eq(CommentStatus.DELETED), any()))
                    .thenReturn(40L);

            MemberStats stats = adminDashboardService.getDashboard().members();

            assertEquals(new MemberStats(500L, 12L, 3L, 800L, 40L), stats);
        }
    }

    @Nested
    @DisplayName("조회수 상위 이벤트와 브랜드별 이벤트 수")
    class RankingTests {

        @Test
        @DisplayName("조회수 상위 이벤트는 5개만 요청하고 응답으로 바꾼다")
        void topEvents() {
            Event event = mockEvent(10L, "치킨 반값", "BHC", 321L);
            when(eventRepository.findTopViewed(any(), any())).thenReturn(List.of(event));

            List<TopEvent> topEvents = adminDashboardService.getDashboard().topEvents();

            ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
            verify(eventRepository).findTopViewed(any(), pageable.capture());
            assertEquals(0, pageable.getValue().getPageNumber());
            assertEquals(5, pageable.getValue().getPageSize());
            assertEquals(List.of(new TopEvent(10L, "치킨 반값", "BHC", 321L)), topEvents);
        }

        @Test
        @DisplayName("브랜드별 이벤트 수는 조회 순서를 유지해 응답으로 바꾼다")
        void brandEventCounts() {
            List<Tuple> rows = List.of(brandRow("BHC", 8L), brandRow("BBQ", 5L));
            when(eventRepository.countVisibleByBrand(any())).thenReturn(rows);

            AdminDashboardResponse response = adminDashboardService.getDashboard();

            assertEquals(List.of(new BrandEventCount("BHC", 8L), new BrandEventCount("BBQ", 5L)),
                    response.brandEventCounts());
        }
    }
}
