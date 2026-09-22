package com.backend.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "마이페이지 수정 요청")
public record UpdateMyPage(
        @Schema(description = "변경할 유저 닉네임", example = "김기환")
        String nickname,

        @Schema(description = "변경할 전화번호", example = "010-1111-2222")
        String phoneNumber,

        @Schema(description = "변경할 이름", example = "김실장")
        String name,

        @Schema(description = "생년월일", example = "2000-01-01")
        LocalDate birth
) {
}
