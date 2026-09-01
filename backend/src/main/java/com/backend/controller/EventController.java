package com.backend.controller;

import com.backend.dto.GetEventListResponse;
import com.backend.dto.GetEventResponse;
import com.backend.dto.GetSearch;
import com.backend.common.EventCode;
import com.backend.service.EventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Tag(name = "Event", description = "이벤트 API")
@RequestMapping("/event")
public class EventController {
    private final EventService eventService;

    @Operation(
            summary = "이벤트 검색",
            description = "이벤트를 검색합니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "검색 성공"
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "요청 값 검증 실패 또는 중복 데이터"
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "인증 실패"
                    )
                    ,
                    @ApiResponse(
                            responseCode = "404",
                            description = "데이터 없음"
                    )
            }
    )
    @GetMapping("/events")
    @ResponseStatus(HttpStatus.OK)
    public Page<GetEventListResponse> searchEvents(
            @RequestParam(required = false) Long brandId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) EventCode eventCode,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "latest") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        GetSearch request = new GetSearch(brandId, categoryId, eventCode, keyword, sort, page, size);
        return eventService.searchEvents(request);
    }

    @Operation(
            summary = "이벤트 조회",
            description = "이벤트를 조회합니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "검색 성공"
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "요청 값 검증 실패 또는 중복 데이터"
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "인증 실패"
                    )
                    ,
                    @ApiResponse(
                            responseCode = "404",
                            description = "데이터 없음"
                    )
            }
    )
    @GetMapping("/{eventId}")
    public GetEventResponse getEvent(@PathVariable Long eventId) {
        return eventService.getEvent(eventId);
    }
}
