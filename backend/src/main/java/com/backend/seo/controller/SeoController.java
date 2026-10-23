package com.backend.seo.controller;

import com.backend.seo.service.SeoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
