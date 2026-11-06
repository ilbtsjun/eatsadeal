package com.backend.notification.repository;

import com.backend.notification.entity.NotificationSetting;
import com.backend.user.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NotificationSettingRepository extends JpaRepository<NotificationSetting, Long> {
    Optional<NotificationSetting> findByUser(User user);

    @Modifying
    @Query("DELETE FROM NotificationSetting ns WHERE ns.user = :user")
    void deleteByUser(@Param("user") User user);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ns FROM NotificationSetting ns WHERE ns.user = :user")
    Optional<NotificationSetting> findByUserForUpdate(@Param("user") User user);

    @Modifying
    @Query(value = """
            INSERT INTO notification_setting (user_id, marketing_agreed, email_agreed, updated_at, unsubscribe_token)
            VALUES (:userId, 0, 0, NOW(6), :token)
            ON DUPLICATE KEY UPDATE id = id
            """, nativeQuery = true)
    void insertIfAbsent(@Param("userId") Long userId, @Param("token") String token);
}
