package com.backend.notification.controller;

import com.backend.common.dto.MsgResponse;
import com.backend.notification.dto.UnsubscribeRequest;
import com.backend.notification.service.NotificationSettingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Tag(name = "Notification", description = "알림 수신 API")
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationSettingService notificationSettingService;

    @Operation(
            summary = "수신거부",
            description = "메일의 수신거부 링크에 담긴 토큰으로 광고성 이메일 수신 동의를 철회합니다. 로그인이 필요 없습니다."
    )
    @PostMapping("/unsubscribe")
    @ResponseStatus(HttpStatus.OK)
    public MsgResponse unsubscribe(@Valid @RequestBody UnsubscribeRequest request) {
        notificationSettingService.unsubscribe(request.token());
        return new MsgResponse("수신거부가 처리되었습니다.", "200");
    }

    @Operation(
            summary = "원클릭 수신거부",
            description = "메일 앱의 구독 취소 버튼(List-Unsubscribe-Post)이 호출합니다. 로그인이 필요 없습니다."
    )
    @PostMapping("/unsubscribe/one-click")
    @ResponseStatus(HttpStatus.OK)
    public void unsubscribeOneClick(@RequestParam String token) {
        notificationSettingService.unsubscribe(token);
    }
}
