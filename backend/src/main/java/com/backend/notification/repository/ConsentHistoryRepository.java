package com.backend.notification.repository;

import com.backend.notification.entity.ConsentHistory;
import com.backend.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ConsentHistoryRepository extends JpaRepository<ConsentHistory, Long> {
    @Modifying
    @Query("DELETE FROM ConsentHistory ch WHERE ch.user = :user")
    void deleteByUser(@Param("user") User user);
}
