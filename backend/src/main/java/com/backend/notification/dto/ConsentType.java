package com.backend.notification.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ConsentType {
    MARKETING_USE("마케팅 활용 동의", "개인정보를 맞춤 이벤트 안내에 활용하는 것에 대한 동의"),
    AD_EMAIL("광고성 이메일 수신 동의", "이벤트·마감 알림 메일 수신에 대한 동의");

    private final String title;
    private final String description;
}
