package com.backend.category.service;

import com.backend.brand.entity.BrandCategory;
import com.backend.brand.repository.BrandCategoryRepository;
import com.backend.category.dto.CreateCategory;
import com.backend.category.dto.GetCategoryResponse;
import com.backend.category.dto.UpdateCategory;
import com.backend.category.entity.Category;
import com.backend.category.repository.CategoryRepository;
import com.backend.common.error.BusinessException;
import com.backend.common.error.ErrorCode;
import com.backend.common.log.CudLogging;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryService {
    private final CategoryRepository categoryRepository;
    private final BrandCategoryRepository brandCategoryRepository;

    @Transactional(readOnly = true)
    public List<GetCategoryResponse> getCategoryList(){
        List<Category> categories = categoryRepository.findAll();
        List<GetCategoryResponse> categoryList = new ArrayList<>();
        for(Category category : categories){
            categoryList.add(new GetCategoryResponse(category.getId(), category.getName(), category.getImg()));
        }
        return categoryList;
    }

    @Transactional
    @CudLogging("카테고리 생성")
    public void createCategory(CreateCategory request){
        if(categoryRepository.existsByName(request.name())){
            throw new BusinessException(ErrorCode.ALREADY_EXISTS);
        }
        Category category = Category.builder()
                .name(request.name().trim())
                .img(request.img())
                .build();
        categoryRepository.save(category);
    }

    @Transactional(readOnly = true)
    public GetCategoryResponse getCategory(Long categoryID){
        Category category = categoryRepository.findById(categoryID)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        return new GetCategoryResponse(category.getId(), category.getName(), category.getImg());
    }

    @Transactional
    @CudLogging("카테고리 수정")
    public void updateCategory(Long categoryID, UpdateCategory request){
        Category category = categoryRepository.findById(categoryID)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        if(!StringUtils.hasText(request.img())){
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        category.updateCategory(request.img());
    }

    @Transactional
    @CudLogging("카테고리 삭제")
    public void deleteCategory(Long categoryID){
        Category category = categoryRepository.findById(categoryID)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        List<BrandCategory> brandCategoryList = brandCategoryRepository.findByCategory(category);
        if(brandCategoryList.isEmpty()) {
            categoryRepository.delete(category);
            return;
        }
        String names = brandCategoryList.stream()
                .map(bc -> bc.getBrand().getName())
                .collect(Collectors.joining(", "));
        throw new BusinessException(ErrorCode.CATEGORY_CANNOT_DELETE, names);
    }
}
