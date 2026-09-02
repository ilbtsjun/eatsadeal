package com.backend.favorite.repository;

import com.backend.event.entity.Event;
import com.backend.favorite.entity.Favorite;
import com.backend.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
    Optional<Favorite> findByUserAndEvent(User user, Event event);
    List<Favorite> findByUser(User user);
}
