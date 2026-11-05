package com.backend.notification.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ConsentSource {
    SUBSCRIBE_POPUP("구독 팝업", "브랜드 구독 시 표시되는 동의 팝업"),
    MYPAGE("마이페이지", "마이페이지 알림 설정");

    private final String title;
    private final String description;
}
