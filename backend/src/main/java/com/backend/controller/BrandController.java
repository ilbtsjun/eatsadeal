package com.backend.controller;

import com.backend.common.MsgResponse;
import com.backend.dto.*;
import com.backend.service.BrandService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Brand", description = "브랜드 API")
@RequestMapping("/brand")
public class BrandController {
    private final BrandService brandService;

    @Operation(
            summary = "브랜드 생성",
            description = "새로운 브랜드를 만듭니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "브랜드 생성 성공"
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
    public MsgResponse createBrand(@Valid @RequestBody CreateBrand request) {
        brandService.createBrand(request);
        return new MsgResponse("새 브랜드가 생성되었습니다.", "201");
    }

    @Operation(
            summary = "브랜드 조회",
            description = "브랜드의 정보를 조회합니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "브랜드 조회 성공"
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "토큰이 없거나 유효하지 않음"
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "인증 실패"
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "조회되는 데이터 없음"
                    )

            }
    )
    @GetMapping("/{brandID}")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasRole('ADMIN')")
    public GetBrandResponse getBrand(@PathVariable Long brandID) {
        return brandService.getBrand(brandID);
    }

    @Operation(
            summary = "브랜드 수정",
            description = "브랜드를 수정합니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "브랜드 수정 성공"
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
    @PutMapping("/{brandID}/update")
    @PreAuthorize("hasRole('ADMIN')")
    public MsgResponse updateBrand(@PathVariable Long brandID,
                                      @Valid @RequestBody UpdateBrand request) {
        brandService.updateBrand(brandID, request);
        return new MsgResponse("브랜드가 수정되었습니다.", "200");
    }

    @Operation(
            summary = "브랜드 삭제",
            description = "브랜드를 삭제합니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "카테고리 삭제 성공"
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "토큰이 없거나 유효하지 않음"
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "인증 실패"
                    )
            }
    )
    @DeleteMapping("/{brandID}/delete")
    @PreAuthorize("hasRole('ADMIN')")
    public MsgResponse deleteBrand(@PathVariable Long brandID) {
        brandService.deleteBrand(brandID);
        return new MsgResponse("브랜드가 삭제되었습니다", "200");
    }

    @Operation(
            summary = "브랜드 리스트 조회",
            description = "브랜드 리스트를 조회합니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "브랜드 리스트 조회 성공"
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "인증 실패"
                    )
            }
    )
    @GetMapping("/list")
    @ResponseStatus(HttpStatus.OK)
    public List<GetBrandListResponse> getBrandList() {
        return brandService.getBrandList();
    }
}
