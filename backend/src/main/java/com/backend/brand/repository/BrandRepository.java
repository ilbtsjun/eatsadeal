package com.backend.brand.repository;

import com.backend.brand.entity.Brand;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BrandRepository extends JpaRepository<Brand, Long> {
    boolean existsByUrl(String url);
    boolean existsByName(String name);
    Brand findByName(String name);
}
