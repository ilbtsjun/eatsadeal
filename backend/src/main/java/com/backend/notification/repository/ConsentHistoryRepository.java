package com.backend.notification.repository;

import com.backend.notification.entity.ConsentHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConsentHistoryRepository extends JpaRepository<ConsentHistory, Long> {

}
