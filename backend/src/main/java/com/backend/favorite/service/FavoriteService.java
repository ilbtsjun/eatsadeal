package com.backend.favorite.service;

import com.backend.auth.service.CurrentUserService;
import com.backend.event.entity.Event;
import com.backend.event.repository.EventRepository;
import com.backend.favorite.entity.Favorite;
import com.backend.favorite.repository.FavoriteRepository;
import com.backend.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class FavoriteService {
    private final FavoriteRepository favoriteRepository;
    private final EventRepository eventRepository;
    private final CurrentUserService currentUserService;

    @Transactional
    public boolean toggleFavorite(Long eventId) {
        User user = currentUserService.getRequiredUser();

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 이벤트입니다."));

        Optional<Favorite> favoriteOptional = favoriteRepository.findByUserAndEvent(user, event);

        if (favoriteOptional.isPresent()) {
            favoriteRepository.delete(favoriteOptional.get());
            return false;
        } else {
            Favorite favorite = Favorite.builder()
                    .user(user)
                    .event(event)
                    .build();

            favoriteRepository.save(favorite);
            return true;
        }
    }

    @Transactional(readOnly = true)
    public List<Long> getFavoriteList() {
        User user = currentUserService.getRequiredUser();
        List<Favorite> favoriteList = favoriteRepository.findByUser(user);
        List<Long> eventList = new ArrayList<>();
        for(Favorite favorite : favoriteList){
            eventList.add(favorite.getEvent().getId());
        }
        return eventList;
    }
}
