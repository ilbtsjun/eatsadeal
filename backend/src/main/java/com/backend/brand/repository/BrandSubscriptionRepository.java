package com.backend.brand.repository;

import com.backend.brand.entity.Brand;
import com.backend.brand.entity.BrandSubscription;
import com.backend.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BrandSubscriptionRepository extends JpaRepository<BrandSubscription, Long> {
    Optional<BrandSubscription> findByUserAndBrand(User user, Brand brand);
    @Query("""
    SELECT b.id
    FROM BrandSubscription bs
    JOIN bs.brand b
    WHERE bs.user = :user
      AND b.isActive = true
    ORDER BY b.id
""")
    List<Long> findActiveBrandIdsByUser(@Param("user") User user);

    @Modifying
    @Query("DELETE FROM BrandSubscription bs WHERE bs.user = :user")
    void deleteByUser(@Param("user") User user);
}
