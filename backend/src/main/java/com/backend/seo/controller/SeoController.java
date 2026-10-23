package com.backend.seo.controller;

import com.backend.seo.service.SeoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

@RestController
@RequiredArgsConstructor
@Tag(name = "SEO", description = "검색엔진·링크 미리보기용 API")
@RequestMapping("/api/seo")
public class SeoController {
    private final SeoService seoService;

    @Operation(
            summary = "사이트맵",
            description = "메인·약관 페이지와 노출 중인 이벤트 상세 주소 목록입니다. nginx가 /sitemap.xml 요청을 이곳으로 넘깁니다."
    )
    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public String sitemap() {
        return seoService.buildSitemap();
    }

    @Operation(
            summary = "이벤트 링크 미리보기 HTML",
            description = "카카오톡·네이버 등 미리보기 봇에게 줄 og 태그 HTML입니다. nginx가 봇의 /events/{id} 요청만 이곳으로 넘깁니다."
    )
    @GetMapping(value = "/events/{eventId}", produces = "text/html;charset=UTF-8")
    public ResponseEntity<String> eventPreview(@PathVariable Long eventId) {
        SeoService.Preview preview = seoService.buildEventPreview(eventId);
        return ResponseEntity.status(preview.found() ? HttpStatus.OK : HttpStatus.NOT_FOUND)
                .contentType(new MediaType(MediaType.TEXT_HTML, StandardCharsets.UTF_8))
                .body(preview.html());
    }
}
