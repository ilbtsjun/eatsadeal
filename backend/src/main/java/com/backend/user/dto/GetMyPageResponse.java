package com.backend.user.dto;

import com.backend.common.UserRole;
import com.backend.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "마이 페이지 요청 응답")
public record GetMyPageResponse(
        @Schema(description = "고유 ID", example = "2")
        Long id,

        @Schema(description = "이름", example = "김철수")
        String name,

        @Schema(description = "메일", example = "user_temp@gmail.com")
        String email,

        @Schema(description = "닉네임", example = "김삿갓")
        String nickname,

        @Schema(description = "전화번호", example = "010-1234-5678")
        String phoneNumber,

        @Schema(description = "성별", example = "UNSPECIFIED")
        UserGender gender,

        @Schema(description = "생년월일", example = "2000-01-01T00:00:00")
        LocalDate birth,

        @Schema(description = "권한 여부", example = "USER")
        UserRole role) {
    public static GetMyPageResponse from(User user) {
        return new GetMyPageResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getNickname(),
                user.getPhoneNumber(),
                user.getGender(),
                user.getBirth(),
                user.getRole()
        );
    }
}
