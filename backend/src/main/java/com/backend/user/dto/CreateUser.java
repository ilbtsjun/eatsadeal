package com.backend.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

@Schema(description = "유저 회원가입 요청")
public record CreateUser(
        @Schema(description = "유저 이름", example = "김철수")
        String name,

        @Schema(description = "유저 이메일", example = "user_temp@gmail.com")
        @NotBlank(message = "유저 이메일은 필수입니다.")
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        String email,

        @Schema(description = "비밀번호", example = "password1234")
        @NotBlank(message = "비밀번호는 필수입니다.")
        String password,

        @Schema(description = "유저 닉네임", example = "김삿갓")
        @NotBlank(message = "닉네임은 필수입니다.")
        String nickName,

        @Schema(description = "유저 전화번호", example = "010-1234-5678")
        String phoneNumber,

        @Schema(description = "유저 성별", example = "UNSPECIFIED")
        UserGender userGender,

        @Schema(description = "유저 생일", example = "2000-01-01T00:00:00")
        LocalDate birth
) {
}
