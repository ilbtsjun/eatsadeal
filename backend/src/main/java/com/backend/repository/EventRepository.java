package com.backend.repository;

import com.backend.entity.Event;
import com.backend.entity.EventCode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {
    boolean existsByUrl(String url);
    @Query("""
        SELECT e
        FROM Event e
        WHERE e.isActive = true
          AND (
              e.endDate IS NULL
              OR e.endDate >= :now
          )
          AND (
              :brandId IS NULL
              OR e.brand.id = :brandId
          )
          AND (
              :categoryId IS NULL
              OR EXISTS (
                  SELECT bc
                  FROM BrandCategory bc
                  WHERE bc.brand = e.brand
                    AND bc.category.id = :categoryId
              )
          )
          AND (
              :eventCode IS NULL
              OR :eventCode MEMBER OF e.eventCodes
          )
          AND (
              :keyword IS NULL
              OR LOWER(e.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
              OR LOWER(e.description) LIKE LOWER(CONCAT('%', :keyword, '%'))
              OR LOWER(e.brand.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
          )
    """)
    Page<Event> searchEvents(
            @Param("brandId") Long brandId,
            @Param("categoryId") Long categoryId,
            @Param("eventCode") EventCode eventCode,
            @Param("keyword") String keyword,
            @Param("now") LocalDateTime now,
            Pageable pageable
    );

    @Modifying
    @Query("""
        UPDATE Event e
        SET e.viewCount = e.viewCount + 1
        WHERE e.id = :eventId
    """)
    Long increaseViewCount(@Param("eventId") Long eventId);

    @Modifying
    @Query("""
        UPDATE Event e
        SET e.isActive = false
        WHERE e.isActive = true
          AND e.endDate IS NOT NULL
          AND e.endDate < :now
    """)
    Long deactivateExpiredEvents(@Param("now") LocalDateTime now);
}
