package com.backend.entity;

import com.backend.category.entity.Category;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(uniqueConstraints = {@UniqueConstraint(
                name = "uk_brand_category",
                columnNames = {"brand_id", "category_id"}
        )
    }
)
@NoArgsConstructor
public class BrandCategory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand", nullable = false)
    private Brand brand;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category", nullable = false)
    private Category category;

    @Builder
    public BrandCategory(Brand brand, Category category){
        this.brand = brand;
        this.category = category;
    }
}
