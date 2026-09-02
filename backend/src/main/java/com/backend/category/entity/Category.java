package com.backend.category.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name="category")
@NoArgsConstructor
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private String img;

    @Builder
    public Category(String name, String img){
        this.name = name;
        this.img = img;
    }

    public void updateCategory(String img){
        this.img = img;
    }
}
