package com.backend.brand.service;

import com.backend.brand.dto.CreateBrand;
import com.backend.brand.dto.GetBrandListResponse;
import com.backend.brand.dto.GetBrandResponse;
import com.backend.brand.dto.UpdateBrand;
import com.backend.brand.entity.Brand;
import com.backend.brand.entity.BrandCategory;
import com.backend.category.entity.Category;
import com.backend.brand.repository.BrandCategoryRepository;
import com.backend.brand.repository.BrandRepository;
import com.backend.category.repository.CategoryRepository;
import com.backend.common.error.BusinessException;
import com.backend.common.error.ErrorCode;
import com.backend.common.log.CudLogging;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BrandService {
    private final BrandRepository brandRepository;
    private final BrandCategoryRepository brandCategoryRepository;
    private final BrandCategoryService brandCategoryService;
    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public List<GetBrandListResponse> getBrandList() {
        return brandRepository.findAll(Sort.by(Sort.Direction.ASC, "name"))
                .stream()
                .map(brand -> new GetBrandListResponse(brand.getId(), brand.getName(), brand.getImg(), brand.getIsActive()))
                .toList();
    }

    @Transactional
    @CudLogging("브랜드 생성")
    public void createBrand(CreateBrand request){
        if(brandRepository.existsByName(request.name()) || brandRepository.existsByUrl(request.url())){
            throw new BusinessException(ErrorCode.ALREADY_EXISTS);
        }
        Brand brand = Brand.builder()
                .name(request.name())
                .url(request.url())
                .img(request.img())
                .build();
        List<Category> categories = request.categoryIds().stream()
                .distinct()
                .map(id -> categoryRepository.findById(id)
                        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND)))
                .toList();
        if(categories == null || categories.isEmpty()){
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "카테고리는 비어있을 수 없습니다.");
        }
        brandRepository.save(brand);
        categories.forEach(category -> brandCategoryService.addCategory(brand, category));
    }

    @Transactional(readOnly = true)
    public GetBrandResponse getBrand(Long brandID) {
        Brand brand = brandRepository.findById(brandID)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        List<BrandCategory> categories = brandCategoryRepository.findByBrand(brand);
        List<Long> categoryIds = new ArrayList<>();
        for(BrandCategory brandCategory : categories){
            categoryIds.add(brandCategory.getCategory().getId());
        }
        return new GetBrandResponse(brand.getId(), brand.getName(), brand.getUrl(), brand.getImg(), categoryIds);
    }

    @Transactional
    @CudLogging("브랜드 수정")
    public void updateBrand(Long brandID, UpdateBrand request) {
        Brand brand = brandRepository.findById(brandID)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        if (StringUtils.hasText(request.name()) && brandRepository.existsByNameAndIdNot(request.name(), brandID)) {
            throw new BusinessException(ErrorCode.ALREADY_EXISTS);
        }
        if (StringUtils.hasText(request.url()) && brandRepository.existsByUrlAndIdNot(request.url(), brandID)) {
            throw new BusinessException(ErrorCode.ALREADY_EXISTS);
        }

        String name = StringUtils.hasText(request.name())
                ? request.name()
                : brand.getName();
        String url = StringUtils.hasText(request.url())
                ? request.url()
                : brand.getUrl();
        String img = StringUtils.hasText(request.img())
                ? request.img()
                : brand.getImg();

        List<Long> categoryIds = request.categoryIds();
        if (categoryIds != null && !categoryIds.isEmpty()) {
            Set<Long> requestIds = new HashSet<>(request.categoryIds());
            List<BrandCategory> existingBrandCategories = brandCategoryRepository.findByBrand(brand);

            Set<Long> existingIds = existingBrandCategories.stream()
                    .map(bc -> bc.getCategory().getId())
                    .collect(Collectors.toSet());

            existingBrandCategories.stream()
                    .filter(bc -> !requestIds.contains(bc.getCategory().getId()))
                    .forEach(bc -> brandCategoryService.deleteCategory(brand, bc.getCategory()));

            List<Long> idsToAdd = requestIds.stream()
                    .filter(id -> !existingIds.contains(id))
                    .toList();

            if (!idsToAdd.isEmpty()) {
                List<Category> categoriesToAdd = categoryRepository.findAllById(idsToAdd);
                if (categoriesToAdd.size() != idsToAdd.size()) {
                    throw new BusinessException(ErrorCode.NOT_FOUND);
                }
                categoriesToAdd.forEach(category -> brandCategoryService.addCategory(brand, category));
            }
        }

        brand.updateBrand(name, url, img);
    }

    @Transactional
    @CudLogging("브랜드 삭제")
    public void deleteBrand(Long brandID){
        Brand brand = brandRepository.findById(brandID)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        brand.deactive();
    }

    @Transactional
    @CudLogging("브랜드 활성화")
    public void activeBrand(Long brandID){
        Brand brand = brandRepository.findById(brandID)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        brand.active();
    }
}
