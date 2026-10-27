package com.backend.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "관리자 대시보드 지표")
public record AdminDashboardResponse(
        EventStats events,
        CrawlStats crawl,
        MemberStats members,
        List<TopEvent> topEvents,
        List<BrandEventCount> brandEventCounts
) {
    public record EventStats(
            @Schema(description = "노출 중인 이벤트 수") long visible,
            @Schema(description = "숨긴 이벤트 수") long hidden,
            @Schema(description = "잠긴 이벤트 수") long locked,
            @Schema(description = "최근 7일 안에 시작한 이벤트 수") long startedThisWeek,
            @Schema(description = "3일 안에 마감되는 이벤트 수") long endingSoon,
            @Schema(description = "혜택 코드가 없는 이벤트 수") long withoutEventCode,
            @Schema(description = "설명이 없는 이벤트 수") long withoutDescription
    ) {
    }

    public record CrawlStats(
            @Schema(description = "마지막 크롤링 시각 (없으면 null)") LocalDateTime lastRunAt,
            @Schema(description = "성공한 브랜드 수") int successCount,
            @Schema(description = "실패하거나 0건인 브랜드 수") int failedCount,
            @Schema(description = "실패하거나 0건인 브랜드") List<String> failedBrands,
            @Schema(description = "오늘 사용한 AI 호출 수") int aiCallsToday,
            @Schema(description = "하루 AI 호출 상한") int aiDailyLimit
    ) {
    }

    public record MemberStats(
            @Schema(description = "전체 회원 수") long totalUsers,
            @Schema(description = "최근 7일 가입자 수") long newUsersThisWeek,
            @Schema(description = "정지된 회원 수") long suspendedUsers,
            @Schema(description = "삭제되지 않은 댓글 수") long totalComments,
            @Schema(description = "최근 7일 댓글 수") long newCommentsThisWeek
    ) {
    }

    public record TopEvent(Long id, String title, String brandName, long viewCount) {
    }

    public record BrandEventCount(String brandName, long eventCount) {
    }
}
