package com.backend.seo.controller;

import com.backend.seo.service.SeoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
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
    private static final MediaType HTML_UTF8 = new MediaType(MediaType.TEXT_HTML, StandardCharsets.UTF_8);

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
            summary = "이벤트 상세 페이지 HTML",
            description = "빌드된 index.html에 이벤트별 title·description·canonical·og 태그와 요약 본문을 채운 HTML입니다. "
                    + "nginx가 /events/{id} 요청을 모두 이곳으로 넘깁니다. 없는 이벤트는 404와 noindex, "
                    + "index.html을 가져오지 못하면 503을 주고 nginx가 기본 index.html로 대신 응답합니다."
    )
    @GetMapping(value = "/events/{eventId}", produces = "text/html;charset=UTF-8")
    public ResponseEntity<String> eventPage(@PathVariable Long eventId) {
        return seoService.buildEventPage(eventId)
                .map(page -> ResponseEntity.status(page.found() ? HttpStatus.OK : HttpStatus.NOT_FOUND)
                        .contentType(HTML_UTF8)
                        .cacheControl(CacheControl.noCache())
                        .body(page.html()))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build());
    }
}
