package com.backend.category.service;

import com.backend.category.dto.CreateCategory;
import com.backend.category.dto.GetCategoryResponse;
import com.backend.category.dto.UpdateCategory;
import com.backend.category.entity.Category;
import com.backend.category.repository.CategoryRepository;
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

    @Transactional
    public void createCategory(CreateCategory request){
        if(categoryRepository.existsByName(request.name())){
            throw new IllegalArgumentException("이미 만들어진 카테고리입니다.");
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
                .orElseThrow(() -> new IllegalArgumentException("카테고리가 없습니다."));
        return new GetCategoryResponse(category.getId(), category.getName(), category.getImg());
    }

    @Transactional
    public void updateCategory(Long categoryID, UpdateCategory request){
        Category category = categoryRepository.findById(categoryID)
                .orElseThrow(() -> new IllegalArgumentException("카테고리가 없습니다."));
        if(!StringUtils.hasText(request.img())){
            throw new IllegalArgumentException("이미지가 비어있을 수 없습니다.");
        }
        category.updateCategory(request.img());
    }

    @Transactional
    public void deleteCategory(Long categoryID){
        Category category = categoryRepository.findById(categoryID)
                .orElseThrow(() -> new IllegalArgumentException("카테고리가 없습니다."));
        categoryRepository.delete(category);
    }

    @Transactional(readOnly = true)
    public List<GetCategoryResponse> getCategoryList(){
        List<Category> categories = categoryRepository.findAll();
        List<GetCategoryResponse> categoryList = new ArrayList<>();
        for(Category category : categories){
            categoryList.add(new GetCategoryResponse(category.getId(), category.getName(), category.getImg()));
        }
        return categoryList;
    }
}
