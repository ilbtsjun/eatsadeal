package com.backend.brand.service;

import com.backend.brand.entity.Brand;
import com.backend.brand.entity.BrandCategory;
import com.backend.category.entity.Category;
import com.backend.brand.repository.BrandCategoryRepository;
import com.backend.common.error.BusinessException;
import com.backend.common.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BrandCategoryService {
    private final BrandCategoryRepository brandCategoryRepository;

    @Transactional
    public void addCategory(Brand brand, Category category){
        if(brand == null || category == null){
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        if(brandCategoryRepository.findByBrandAndCategory(brand, category) != null){
            String message = brand.getName() + "에는 이미 " + category.getName() + "이(가) 있습니다.";
            throw new BusinessException(ErrorCode.ALREADY_EXISTS, message);
        }
        BrandCategory brandCategory = BrandCategory.builder()
                .brand(brand)
                .category(category)
                .build();
        brandCategoryRepository.save(brandCategory);
    }

    @Transactional
    public void deleteCategory(Brand brand, Category category){
        if(brand == null || category == null){
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        BrandCategory brandCategory = brandCategoryRepository.findByBrandAndCategory(brand, category);
        if(brandCategory == null){
            String message = brand.getName() + "에는 " + category.getName() + "이(가) 없습니다.";
            throw new BusinessException(ErrorCode.NOT_FOUND, message);
        }
        brandCategoryRepository.delete(brandCategory);
    }
}
