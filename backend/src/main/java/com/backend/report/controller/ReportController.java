package com.backend.report.controller;

import com.backend.common.dto.MsgResponse;
import com.backend.report.dto.ReportRequest;
import com.backend.report.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Tag(name = "Report", description = "제보 API")
public class ReportController {

    private final ReportService reportService;

    @Operation(
            summary = "제보 접수",
            description = "제보를 관리자 메일로 발송합니다. 10분에 5건까지 가능합니다."
    )
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MsgResponse create(@Valid @RequestBody ReportRequest request) {
        reportService.create(request);
        return new MsgResponse("제보가 접수되었습니다.", "201");
    }
}
