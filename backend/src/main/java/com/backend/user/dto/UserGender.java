package com.backend.user.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserGender {
    MALE,
    FEMALE,
    OTHER,
    UNSPECIFIED
}
