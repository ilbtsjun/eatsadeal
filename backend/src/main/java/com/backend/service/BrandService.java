package com.backend.service;

import com.backend.dto.*;
import com.backend.entity.Brand;
import com.backend.entity.BrandCategory;
import com.backend.entity.Category;
import com.backend.repository.BrandCategoryRepository;
import com.backend.repository.BrandRepository;
import com.backend.repository.CategoryRepository;
import com.backend.repository.EventRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final EventService eventService;
    private final CategoryRepository categoryRepository;

    @Transactional
    public void createBrand(CreateBrand request){
        if(brandRepository.existsByName(request.name()) || brandRepository.existsByUrl(request.url())){
            throw new IllegalArgumentException("이미 존재하는 브랜드 입니다.");
        }
        Brand brand = Brand.builder()
                .name(request.name())
                .url(request.url())
                .img(request.img())
                .build();
        brandRepository.save(brand);
        for(Long categoryId : request.categoryIds()){
            Category category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new IllegalArgumentException("카테고리가 없습니다."));
            brandCategoryService.addCategory(brand, category);
        }
    }

    @Transactional(readOnly = true)
    public GetBrandResponse getBrand(Long brandID) {
        Brand brand = brandRepository.findById(brandID)
                .orElseThrow(() -> new IllegalArgumentException("브랜드가 없습니다."));
        List<BrandCategory> categories = brandCategoryRepository.findByBrand(brand);
        List<Long> categoryIds = new ArrayList<>();
        for(BrandCategory brandCategory : categories){
            categoryIds.add(brandCategory.getCategory().getId());
        }
        return new GetBrandResponse(brand.getName(), brand.getUrl(), brand.getImg(), categoryIds, brand.getLastCrawl());
    }

    @Transactional
    public void updateBrand(Long brandID, @Valid UpdateBrand request) {
        Brand brand = brandRepository.findById(brandID)
                .orElseThrow(() -> new IllegalArgumentException("브랜드가 없습니다."));
        String name = request.name();
        if(request.name().isBlank()){
            name = brand.getName();
        }
        String url = request.url();
        if(request.url().isBlank()){
            url = brand.getUrl();
        }
        String img = request.img();
        if(request.img().isBlank()){
            img = brand.getImg();
        }
        brand.updateBrand(name, url, img);
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
            categoriesToAdd.forEach(category -> brandCategoryService.addCategory(brand, category));
        }
    }

    @Transactional
    public void deleteBrand(Long brandID){
        Brand brand = brandRepository.findById(brandID)
                .orElseThrow(() -> new IllegalArgumentException("브랜드가 없습니다."));
        brandRepository.delete(brand);
    }

    @Transactional
    public void createEvent(Long brandID, CreateEvent request){
        Brand brand = brandRepository.findById(brandID)
                .orElseThrow(() -> new IllegalArgumentException("브랜드가 없습니다."));
        CreateEvent eventDto = new CreateEvent(
                request.title(),
                request.description(),
                request.url(),
                request.img(),
                request.startDate(),
                request.endDate(),
                brand.getName(),
                request.eventCodes());
        eventService.createEvent(eventDto);
    }

    @Transactional(readOnly = true)
    public List<GetBrandListResponse> getBrandList() {
        return brandRepository.findAll(Sort.by(Sort.Direction.ASC, "name"))
                .stream()
                .map(brand -> new GetBrandListResponse(brand.getId(), brand.getName(), brand.getImg()))
                .toList();
    }
}
