package com.backend.favorite.service;

import com.backend.auth.service.CurrentUserService;
import com.backend.common.error.BusinessException;
import com.backend.common.error.ErrorCode;
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
    public void addFavorite(Long eventId) {
        User user = currentUserService.getRequiredUser();

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        Optional<Favorite> favoriteOptional = favoriteRepository.findByUserAndEvent(user, event);

        if (favoriteOptional.isPresent()) {
            throw new BusinessException(ErrorCode.ALREADY_EXISTS);
        }
        Favorite favorite = Favorite.builder()
                .user(user)
                .event(event)
                .build();

        favoriteRepository.save(favorite);
    }

    @Transactional
    public void deleteFavorite(Long eventId) {
        User user = currentUserService.getRequiredUser();

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        Optional<Favorite> favoriteOptional = favoriteRepository.findByUserAndEvent(user, event);

        if (!favoriteOptional.isPresent()) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }

        favoriteRepository.delete(favoriteOptional.get());
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
