package com.backend.brand.entity;

import com.backend.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "brand_subscription",
        uniqueConstraints = {@UniqueConstraint(
        name = "uk_brand_subscription_user_brand",
        columnNames = {"user_id", "brand_id"})}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BrandSubscription {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id", nullable = false)
    private Brand brand;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Builder
    public BrandSubscription(User user, Brand brand) {
        this.user = user;
        this.brand = brand;
        this.createdAt = LocalDateTime.now();
    }
}