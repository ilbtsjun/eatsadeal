package com.backend.favorite.service;

import com.backend.config.JwtTokenProvider;
import com.backend.config.TokenBlacklist;
import com.backend.event.entity.Event;
import com.backend.event.repository.EventRepository;
import com.backend.favorite.entity.Favorite;
import com.backend.favorite.repository.FavoriteRepository;
import com.backend.user.dto.UserStatus;
import com.backend.user.entity.User;
import com.backend.user.repository.UserRepository;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class FavoriteService {
    private final FavoriteRepository favoriteRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenBlacklist tokenBlacklist;

    @Transactional
    public boolean toggleFavorite(String token, Long eventId) {
        User user = findUserByToken(token);

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
    public List<Long> getFavoriteList(String token) {
        User user = findUserByToken(token);
        List<Favorite> favoriteList = favoriteRepository.findByUser(user);
        List<Long> eventList = new ArrayList<>();
        for(Favorite favorite : favoriteList){
            eventList.add(favorite.getEvent().getId());
        }
        return eventList;
    }

    private User findUserByToken(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("토큰은 필수입니다.");
        }
        if (tokenBlacklist.contains(token)) {
            throw new IllegalArgumentException("이미 로그아웃된 토큰입니다.");
        }
        try {
            if (!jwtTokenProvider.validateToken(token)) {
                throw new IllegalArgumentException("유효하지 않은 토큰입니다.");
            }
            Long userId = Long.valueOf(jwtTokenProvider.getSubject(token));
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
            user.releaseIfExpired(LocalDateTime.now());
            if(user.getUserStatus().equals(UserStatus.ACTIVE)){
                return user;
            }
            throw new IllegalArgumentException("정지되거나 탈퇴한 사용자입니다.");
        } catch (JwtException | NumberFormatException ex) {
            throw new IllegalArgumentException("유효하지 않은 토큰입니다.");
        }
    }
}
