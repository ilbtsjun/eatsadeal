package com.backend.favorite.repository;

import com.backend.event.entity.Event;
import com.backend.favorite.entity.Favorite;
import com.backend.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
    Optional<Favorite> findByUserAndEvent(User user, Event event);
    List<Favorite> findByUser(User user);
    @Query("""
    SELECT e.id
    FROM Favorite f
    JOIN f.event e
    JOIN e.brand b
    WHERE f.user = :user
      AND b.isActive = true
    ORDER BY f.id
""")
    List<Long> findVisibleEventIdsByUser(@Param("user") User user);
}
