package com.backend.brand.repository;

import com.backend.brand.entity.Brand;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BrandRepository extends JpaRepository<Brand, Long> {
    boolean existsByUrl(String url);
    boolean existsByName(String name);
    Optional<Brand> findByName(String name);
    boolean existsByNameAndIdNot(String name, Long id);
    boolean existsByUrlAndIdNot(String url, Long id);
}
