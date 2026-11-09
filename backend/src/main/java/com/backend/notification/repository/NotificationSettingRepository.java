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

import java.time.LocalDateTime;
import java.util.List;
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

    Optional<NotificationSetting> findByUnsubscribeToken(String token);

    @Query("""
            SELECT ns FROM NotificationSetting ns
            JOIN FETCH ns.user u
            WHERE ns.emailAgreed = true
              AND ns.emailAgreedAt <= :threshold
              AND (ns.consentNotifiedAt IS NULL OR ns.consentNotifiedAt <= :threshold)
              AND u.userStatus = com.backend.user.dto.UserStatus.ACTIVE
            """)
    List<NotificationSetting> findReconfirmTargets(@Param("threshold") LocalDateTime threshold);
}
