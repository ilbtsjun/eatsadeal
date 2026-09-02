package com.backend.event.controller;

import com.backend.common.MsgResponse;
import com.backend.config.JwtAuthenticationFilter;
import com.backend.event.dto.*;
import com.backend.event.service.EventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Event", description = "이벤트 API")
@RequestMapping("/event")
public class EventController {
    private final EventService eventService;

    @Operation(
            summary = "이벤트 수동 생성",
            description = "이벤트를 수동으로 만듭니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "이벤트 생성 성공"
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "요청 값 검증 실패 또는 중복 데이터"
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "인증 실패"
                    )
            }
    )
    @PostMapping("/create")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public MsgResponse createEvent(@Valid @RequestBody CreateEvent createEvent) {
        eventService.createEvent(createEvent);
        return new MsgResponse("새 이벤트가 생성되었습니다.","201");
    }

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
    public GetEventResponse getEvent(@RequestHeader(value = JwtAuthenticationFilter.TOKEN_HEADER, required = false) String token,
            @PathVariable Long eventId) {
        return eventService.getEvent(token, eventId);
    }

    @Operation(
            summary = "이벤트 수정",
            description = "이벤트를 수정합니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "수정 성공"
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "요청 값 검증 실패 또는 중복 데이터"
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "인증 실패"
                    )
            }
    )
    @PatchMapping("/{eventID}/update")
    @PreAuthorize("hasRole('ADMIN')")
    public MsgResponse updateEvent(@PathVariable Long eventID,
                                   @Valid @RequestBody UpdateEvent request){
        eventService.updateEvent(eventID, request);
        return new MsgResponse("수정이 성공적으로 완료되었습니다.","200");
    }

    @Operation(
            summary = "이벤트 종료",
            description = "이벤트를 종료합니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "종료 성공"
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "요청 값 검증 실패 또는 중복 데이터"
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "인증 실패"
                    )
            }
    )
    @PatchMapping("/{eventId}/inactive")
    @PreAuthorize("hasRole('ADMIN')")
    public MsgResponse deactivateEvent(@PathVariable Long eventId) {
        eventService.deactivateEvent(eventId);
        return new MsgResponse("이벤트가 비활성화되었습니다.", "200");
    }

    @Operation(
            summary = "이벤트 코드 목록 조회",
            description = "이벤트 코드 목록을 반환합니다..",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "반환 성공"
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
    @GetMapping("/eventCodes")
    @PreAuthorize("hasRole('ADMIN')")
    public List<GetEventCodeListResponse> getEventCodes(){
        return eventService.getEventCodes();
    }
}
