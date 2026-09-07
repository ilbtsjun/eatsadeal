package com.backend.category.service;

import com.backend.category.dto.CreateCategory;
import com.backend.category.dto.GetCategoryResponse;
import com.backend.category.dto.UpdateCategory;
import com.backend.category.entity.Category;
import com.backend.category.repository.CategoryRepository;
import com.backend.common.error.BusinessException;
import com.backend.common.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryService {
    private final CategoryRepository categoryRepository;

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
    public void createCategory(CreateCategory request){
        if(categoryRepository.existsByName(request.name())){
            throw new BusinessException(ErrorCode.CATEGORY_ALREADY_EXISTS);
        }
        Category category = Category.builder()
                .name(request.name())
                .img(request.img())
                .build();
        categoryRepository.save(category);
    }

    @Transactional(readOnly = true)
    public GetCategoryResponse getCategory(Long categoryID){
        Category category = categoryRepository.findById(categoryID)
                .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));
        return new GetCategoryResponse(category.getId(), category.getName(), category.getImg());
    }

    @Transactional
    public void updateCategory(Long categoryID, UpdateCategory request){
        Category category = categoryRepository.findById(categoryID)
                .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));
        if(!StringUtils.hasText(request.img())){
            throw new BusinessException(ErrorCode.IMG_NOT_FOUND);
        }
        category.updateCategory(request.img());
    }

    @Transactional
    public void deleteCategory(Long categoryID){
        Category category = categoryRepository.findById(categoryID)
                .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));
        categoryRepository.delete(category);
    }
}
