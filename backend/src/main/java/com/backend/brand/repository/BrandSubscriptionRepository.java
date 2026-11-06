package com.backend.brand.repository;

import com.backend.brand.entity.Brand;
import com.backend.brand.entity.BrandSubscription;
import com.backend.notification.dto.NotificationTarget;
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

    @Query("""
    SELECT new com.backend.notification.dto.NotificationTarget(u.id, u.email, u.nickname, b.id, ns.unsubscribeToken)
    FROM BrandSubscription bs
    JOIN bs.user u
    JOIN bs.brand b
    JOIN NotificationSetting ns ON ns.user = u
    WHERE ns.marketingAgreed = true
      AND ns.emailAgreed = true
      AND u.userStatus = com.backend.user.dto.UserStatus.ACTIVE
      AND b.isActive = true
    ORDER BY u.id, b.id
""")
    List<NotificationTarget> findNotificationTargets();
}
