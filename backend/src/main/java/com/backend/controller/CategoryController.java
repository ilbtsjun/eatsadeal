package com.backend.controller;

import com.backend.common.MsgResponse;
import com.backend.dto.CreateCategory;
import com.backend.dto.GetCategoryResponse;
import com.backend.dto.UpdateCategory;
import com.backend.service.CategoryService;
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
@Tag(name = "Category", description = "카테고리 API")
@RequestMapping("/category")
public class CategoryController {
    private final CategoryService categoryService;

    @Operation(
            summary = "카테고리 생성",
            description = "새로운 카테고리를 만듭니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "카테고리 생성 성공"
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
    public MsgResponse createCategory(@Valid @RequestBody CreateCategory request) {
        categoryService.createCategory(request);
        return new MsgResponse("새 카테고리가 생성되었습니다.", "201");
    }

    @Operation(
            summary = "카테고리 조회",
            description = "카테고리의 정보를 조회합니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "카테고리 조회 성공"
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
    @GetMapping("/{categoryID}")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasRole('ADMIN')")
    public GetCategoryResponse getCategory(@PathVariable Long categoryID) {
        return categoryService.getCategory(categoryID);
    }

    @Operation(
            summary = "카테고리 수정",
            description = "카테고리를 수정합니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "카테고리 수정 성공"
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
    @PutMapping("/{categoryID}/update")
    @PreAuthorize("hasRole('ADMIN')")
    public MsgResponse updateCategory(@PathVariable Long categoryID,
                                      @Valid @RequestBody UpdateCategory request) {
        categoryService.updateCategory(categoryID, request);
        return new MsgResponse("카테고리가 수정되었습니다.", "200");
    }

    @Operation(
            summary = "카테고리 삭제",
            description = "카테고리를 삭제합니다.",
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
    @DeleteMapping("/{categoryID}/delete")
    @PreAuthorize("hasRole('ADMIN')")
    public MsgResponse deleteCategory(@PathVariable Long categoryID) {
        categoryService.deleteCategory(categoryID);
        return new MsgResponse("카테고리가 삭제되었습니다", "200");
    }

    @Operation(
            summary = "카테고리 리스트 조회",
            description = "카테고리의 리스트를 조회합니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "카테고리 조회 성공"
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
    @GetMapping("/list")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasRole('ADMIN')")
    public List<GetCategoryResponse> getCategoryList() {
        return categoryService.getCategoryList();
    }
}
