package com.backend.brand.service;

import com.backend.brand.entity.Brand;
import com.backend.brand.entity.BrandCategory;
import com.backend.category.entity.Category;
import com.backend.brand.repository.BrandCategoryRepository;
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
            throw new IllegalArgumentException("존재하지 않는 브랜드 또는 카테고리입니다.");
        }
        if(brandCategoryRepository.findByBrandAndCategory(brand, category) != null){
            throw new IllegalArgumentException("브랜드에 이미 카테고리가 추가되어있습니다.");
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
            throw new IllegalArgumentException("브랜드 또는 카테고리가 없습니다.");
        }
        BrandCategory brandCategory = brandCategoryRepository.findByBrandAndCategory(brand, category);
        if(brandCategory == null){
            throw new IllegalArgumentException("브랜드에 해당 카테고리가 없습니다.");
        }
        brandCategoryRepository.delete(brandCategory);
    }
}
