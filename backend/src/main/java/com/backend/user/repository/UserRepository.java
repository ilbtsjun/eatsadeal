package com.backend.user.repository;

import com.backend.user.dto.UserStatus;
import com.backend.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByEmail(String email);
    boolean existsByNickname(String nickName);
    @Query("SELECT u FROM User u WHERE u.email = :id OR u.nickname = :id")
    Optional<User> findByEmailOrNickname(String id);


    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
    UPDATE User u
       SET u.userStatus = :activeStatus,
           u.suspendedAt = NULL,
           u.suspendedUntil = NULL,
           u.suspendingReason = NULL,
           u.updatedAt = :now
     WHERE u.userStatus = :suspendStatus
       AND u.suspendedUntil IS NOT NULL
       AND u.suspendedUntil <= :now
    """)
    int releaseExpiredSuspensions(
            @Param("now") LocalDateTime now,
            @Param("activeStatus") UserStatus activeStatus,
            @Param("suspendStatus") UserStatus suspendStatus
    );
}
