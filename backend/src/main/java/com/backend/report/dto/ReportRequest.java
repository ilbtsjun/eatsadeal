package com.backend.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "제보 요청")
public record ReportRequest(
        @Schema(description = "제보 제목", example = "잘못된 이벤트 정보 제보")
        @NotBlank(message = "제목은 필수입니다.")
        @Size(max = 100, message = "제목은 100자 이하로 입력해주세요.")
        String title,

        @Schema(description = "제보 내용", example = "BBQ 이벤트 종료일이 실제와 다릅니다.")
        @NotBlank(message = "내용은 필수입니다.")
        @Size(max = 2000, message = "내용은 2000자 이하로 입력해주세요.")
        String content
) {
}
