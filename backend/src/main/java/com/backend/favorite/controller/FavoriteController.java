package com.backend.favorite.controller;

import com.backend.config.JwtAuthenticationFilter;
import com.backend.favorite.service.FavoriteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Favorite", description = "즐겨찾기 API")
@RequestMapping("/favorite")
public class FavoriteController {
    private final FavoriteService favoriteService;

    @Operation(
            summary = "즐겨찾기를 토글합니다.",
            description = "JWT 토큰으로 로그인한 유저를 찾고, 이벤트 ID로 토글합니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "토글 성공"
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "토큰이 없거나 유효하지 않음, 입력값 검증 실패"
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "인증 실패"
                    )
            }
    )
    @PostMapping("/{eventId}/favorite")
    @PreAuthorize("isAuthenticated()")
    public Boolean toggleFavorite(@RequestHeader(JwtAuthenticationFilter.TOKEN_HEADER) String token,
                                  @PathVariable Long eventId) {
        return favoriteService.toggleFavorite(token, eventId);
    }

    @Operation(
            summary = "즐겨찾기 목록을 반환합니다.",
            description = "JWT 토큰으로 로그인한 유저를 찾고, 유저의 즐겨찾기 목록을 반환합니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "반환 성공"
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "토큰이 없거나 유효하지 않음, 입력값 검증 실패"
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "인증 실패"
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "자료 없음"
                    )
            }
    )
    @GetMapping("/list")
    @PreAuthorize("isAuthenticated()")
    public List<Long> toggleFavorite(@RequestHeader(JwtAuthenticationFilter.TOKEN_HEADER) String token) {
        return favoriteService.getFavoriteList(token);
    }
}
