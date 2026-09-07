package com.backend.brand.controller;

import com.backend.brand.dto.CreateBrand;
import com.backend.brand.dto.GetBrandListResponse;
import com.backend.brand.dto.GetBrandResponse;
import com.backend.brand.dto.UpdateBrand;
import com.backend.common.dto.MsgResponse;
import com.backend.brand.service.BrandService;
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
@RequestMapping("/api/brands")
public class BrandController {
    private final BrandService brandService;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<GetBrandListResponse> getBrandList() {
        return brandService.getBrandList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public MsgResponse createBrand(@Valid @RequestBody CreateBrand request) {
        brandService.createBrand(request);
        return new MsgResponse("새 브랜드가 생성되었습니다.", "201");
    }

    @GetMapping("/{brandID}")
    @ResponseStatus(HttpStatus.OK)
    public GetBrandResponse getBrand(@PathVariable Long brandID) {
        return brandService.getBrand(brandID);
    }

    @PatchMapping("/{brandID}")
    @PreAuthorize("hasRole('ADMIN')")
    public MsgResponse updateBrand(@PathVariable Long brandID,
                                      @Valid @RequestBody UpdateBrand request) {
        brandService.updateBrand(brandID, request);
        return new MsgResponse("브랜드가 수정되었습니다.", "200");
    }

    @DeleteMapping("/{brandID}")
    @PreAuthorize("hasRole('ADMIN')")
    public MsgResponse deleteBrand(@PathVariable Long brandID) {
        brandService.deleteBrand(brandID);
        return new MsgResponse("브랜드가 삭제되었습니다", "200");
    }
}
