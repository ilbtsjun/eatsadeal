package com.backend.brand.repository;

import com.backend.brand.entity.Brand;
import com.backend.brand.entity.BrandCategory;
import com.backend.category.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BrandCategoryRepository extends JpaRepository<BrandCategory, Long> {
    BrandCategory findByBrandAndCategory(Brand brand, Category category);
    List<BrandCategory> findByBrand(Brand brand);
}
