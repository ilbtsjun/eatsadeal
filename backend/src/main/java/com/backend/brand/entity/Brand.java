package com.backend.brand.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "brand")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Brand {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name;

    @Column(unique = true , nullable = false)
    private String url;

    @Column(nullable = false)
    private String img;

    @Column(nullable = false)
    private Boolean isActive = true;

    @Builder
    public Brand(String name, String url, String img){
        this.name = name;
        this.url = url;
        this.img = img;
    }

    public void updateBrand(String name, String url, String img){
        this.name = name;
        this.url = url;
        this.img = img;
    }

    public void deactive(){
        this.isActive = false;
    }

    public void active(){
        this.isActive = true;
    }
}
