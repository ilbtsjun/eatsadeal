package com.backend.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

@Schema(description = "마이페이지 수정 요청")
public record UpdateMyPage(
        @Schema(description = "변경할 유저 닉네임", example = "김기환")
        @Size(max = 255, message = "닉네임은 255자 이하여야 합니다.")
        String nickname,

        @Schema(description = "변경할 전화번호", example = "010-1111-2222")
        @Size(max = 255, message = "전화번호는 255자 이하여야 합니다.")
        String phoneNumber,

        @Schema(description = "변경할 이름", example = "김실장")
        @Size(max = 255, message = "이름은 255자 이하여야 합니다.")
        String name,

        @Schema(description = "생년월일", example = "2000-01-01")
        LocalDate birth
) {
}
