package com.backend.comment.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CommentStatus {
    ACTIVE("활성 상태", "원본 상태"),
    MODIFIED("수정 상태", "수정된 상태"),
    HIDDEN("숨김 상태","숨겨진 상태"),
    DELETED("삭제 상태", "삭제된 상태");

    private final String title;
    private final String description;
}
