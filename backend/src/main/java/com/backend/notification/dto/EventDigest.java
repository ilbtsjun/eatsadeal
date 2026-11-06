package com.backend.notification.dto;

import com.backend.event.dto.GetEventListResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "회원별 알림 메일 내용")
public record EventDigest(
        @Schema(description = "회원 ID", example = "1")
        Long userId,

        @Schema(description = "회원 이메일", example = "user_temp@gmail.com")
        String email,

        @Schema(description = "회원 닉네임", example = "김삿갓")
        String nickname,

        @Schema(description = "구독 브랜드의 새 이벤트 (어제 9시 ~ 오늘 9시 등록)")
        List<GetEventListResponse> newEvents,

        @Schema(description = "구독 브랜드의 마감 임박 이벤트 (내일 종료)")
        List<GetEventListResponse> endingSoonEvents,

        @Schema(description = "동의 해제용 토큰", example = "111111111111111111111")
        String unsubscribeToken
) {
    public EventDigest {
        newEvents = List.copyOf(newEvents);
        endingSoonEvents = List.copyOf(endingSoonEvents);
    }

    public int totalCount() {
        return newEvents.size() + endingSoonEvents.size();
    }

    public boolean isEmpty() {
        return totalCount() == 0;
    }
}
