package com.backend.user.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserStatus {
    STANDBY("가입 대기 상태", "가입시 아직 인증이 안된 상태"),
    ACTIVE("활성 상태", "인증이 끝난 상태"),
    SUSPEND("정지 상태", "일시적으로 정지된 상태"),
    WITHDRAWN("탈퇴 상태", "탈퇴 및 영구 정지 상태");

    private final String title;
    private final String description;
}
