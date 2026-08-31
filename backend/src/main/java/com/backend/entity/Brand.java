package com.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@Entity
@Table(name = "brand")
@NoArgsConstructor
public class Brand {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    @NotBlank
    private String name;

    @Column(unique = true)
    @NotBlank
    private String url;

    @Column
    @NotBlank
    private String img;

    @Column
    private LocalDate lastCrawl;

    @Builder
    public Brand(String name, String url, String img){
        this.name = name;
        this.url = url;
        this.img = img;
        this.lastCrawl = null;
    }

    public void updateBrand(String name, String url, String img){
        this.name = name;
        this.url = url;
        this.img = img;
    }
}
