package com.backend.category.controller;

import com.backend.category.service.CategoryService;
import com.backend.category.dto.CreateCategory;
import com.backend.category.dto.GetCategoryResponse;
import com.backend.category.dto.UpdateCategory;
import com.backend.common.dto.MsgResponse;
import io.swagger.v3.oas.annotations.Operation;
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
@RequestMapping("/api/categories")
public class CategoryController {
    private final CategoryService categoryService;

    @Operation(
            summary = "카테고리 리스트 조회",
            description = "카테고리의 리스트를 조회합니다."
    )
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<GetCategoryResponse> getCategoryList() {
        return categoryService.getCategoryList();
    }

    @Operation(
            summary = "카테고리 생성",
            description = "새로운 카테고리를 만듭니다."
    )
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public MsgResponse createCategory(@Valid @RequestBody CreateCategory request) {
        categoryService.createCategory(request);
        return new MsgResponse("새 카테고리가 생성되었습니다.", "201");
    }

    @Operation(
            summary = "카테고리 조회",
            description = "카테고리의 정보를 조회합니다."
    )
    @GetMapping("/{categoryID}")
    @ResponseStatus(HttpStatus.OK)
    public GetCategoryResponse getCategory(@PathVariable Long categoryID) {
        return categoryService.getCategory(categoryID);
    }

    @Operation(
            summary = "카테고리 수정",
            description = "카테고리를 수정합니다."
    )
    @PatchMapping("/{categoryID}")
    @PreAuthorize("hasRole('ADMIN')")
    public MsgResponse updateCategory(@PathVariable Long categoryID,
                                      @Valid @RequestBody UpdateCategory request) {
        categoryService.updateCategory(categoryID, request);
        return new MsgResponse("카테고리가 수정되었습니다.", "200");
    }

    @Operation(
            summary = "카테고리 삭제",
            description = "카테고리를 삭제합니다."
    )
    @DeleteMapping("/{categoryID}")
    @PreAuthorize("hasRole('ADMIN')")
    public MsgResponse deleteCategory(@PathVariable Long categoryID) {
        categoryService.deleteCategory(categoryID);
        return new MsgResponse("카테고리가 삭제되었습니다", "200");
    }
}
