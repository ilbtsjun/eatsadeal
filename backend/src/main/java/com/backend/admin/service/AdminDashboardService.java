package com.backend.admin.service;

import com.backend.admin.dto.AdminDashboardResponse;
import com.backend.admin.dto.AdminDashboardResponse.BrandEventCount;
import com.backend.admin.dto.AdminDashboardResponse.CrawlStats;
import com.backend.admin.dto.AdminDashboardResponse.EventStats;
import com.backend.admin.dto.AdminDashboardResponse.MemberStats;
import com.backend.admin.dto.AdminDashboardResponse.TopEvent;
import com.backend.comment.entity.CommentStatus;
import com.backend.comment.repository.CommentRepository;
import com.backend.crawler.dto.CrawlRunResponse;
import com.backend.crawler.entity.CrawlStatus;
import com.backend.crawler.service.CrawlerService;
import com.backend.event.repository.EventRepository;
import com.backend.event.service.AnthropicEventCodeAi;
import com.backend.user.dto.UserStatus;
import com.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AdminDashboardService {
    private static final int TOP_EVENT_LIMIT = 5;
    private static final Set<CrawlStatus> FAILED_STATUSES = Set.of(CrawlStatus.FAILED, CrawlStatus.EMPTY);

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final CommentRepository commentRepository;
    private final CrawlerService crawlerService;
    private final AnthropicEventCodeAi eventCodeAi;

    @Transactional(readOnly = true)
    public AdminDashboardResponse getDashboard() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime weekAgo = now.minusDays(7);

        return new AdminDashboardResponse(
                getEventStats(now, weekAgo),
                getCrawlStats(),
                getMemberStats(weekAgo),
                getTopEvents(now),
                getBrandEventCounts(now));
    }

    private EventStats getEventStats(LocalDateTime now, LocalDateTime weekAgo) {
        return new EventStats(
                eventRepository.countVisible(now),
                eventRepository.countByIsActiveTrueAndHiddenTrue(),
                eventRepository.countByIsActiveTrueAndAdminLockedTrue(),
                eventRepository.countVisibleStartedSince(weekAgo, now),
                eventRepository.countVisibleEndingBetween(now, now.plusDays(3)),
                eventRepository.countActiveWithoutEventCode(),
                eventRepository.countActiveWithoutDescription());
    }

    private CrawlStats getCrawlStats() {
        List<CrawlRunResponse> runs = crawlerService.getLatestRuns();
        LocalDateTime lastRunAt = runs.stream()
                .map(CrawlRunResponse::finishedAt)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(null);
        List<String> failedBrands = runs.stream()
                .filter(run -> FAILED_STATUSES.contains(run.status()))
                .map(CrawlRunResponse::brandKey)
                .toList();
        int successCount = (int) runs.stream()
                .filter(run -> run.status() == CrawlStatus.SUCCESS || run.status() == CrawlStatus.PARTIAL)
                .count();

        return new CrawlStats(lastRunAt, successCount, failedBrands.size(), failedBrands,
                eventCodeAi.usedToday(), eventCodeAi.dailyLimit());
    }

    private MemberStats getMemberStats(LocalDateTime weekAgo) {
        return new MemberStats(
                userRepository.count(),
                userRepository.countByCreatedAtGreaterThanEqual(weekAgo),
                userRepository.countByUserStatus(UserStatus.SUSPEND),
                commentRepository.countByCommentStatusNot(CommentStatus.DELETED),
                commentRepository.countByCommentStatusNotAndCreatedAtGreaterThanEqual(CommentStatus.DELETED, weekAgo));
    }

    private List<TopEvent> getTopEvents(LocalDateTime now) {
        return eventRepository.findTopViewed(now, PageRequest.of(0, TOP_EVENT_LIMIT)).stream()
                .map(event -> new TopEvent(event.getId(), event.getTitle(),
                        event.getBrand().getName(), event.getViewCount()))
                .toList();
    }

    private List<BrandEventCount> getBrandEventCounts(LocalDateTime now) {
        return eventRepository.countVisibleByBrand(now).stream()
                .map(row -> new BrandEventCount(row.get("brandName", String.class), row.get("eventCount", Long.class)))
                .toList();
    }
}
