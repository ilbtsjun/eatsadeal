package com.backend.favorite;

import com.backend.auth.service.CurrentUserService;
import com.backend.common.error.BusinessException;
import com.backend.common.error.ErrorCode;
import com.backend.event.entity.Event;
import com.backend.event.repository.EventRepository;
import com.backend.favorite.entity.Favorite;
import com.backend.favorite.repository.FavoriteRepository;
import com.backend.favorite.service.FavoriteService;
import com.backend.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FavoriteServiceTests {

    @Mock
    private FavoriteRepository favoriteRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private FavoriteService favoriteService;

    private User mockUser() {
        return mock(User.class);
    }

    private Event mockEvent(Long id) {
        Event event = mock(Event.class);
        lenient().when(event.getId()).thenReturn(id);
        return event;
    }

    private Favorite buildFavorite(User user, Event event) {
        return Favorite.builder().user(user).event(event).build();
    }

    private BusinessException assertBusinessException(Runnable runnable, ErrorCode expected) {
        BusinessException exception = assertThrows(BusinessException.class, runnable::run);
        assertEquals(expected, exception.getErrorCode());
        return exception;
    }

    private BusinessException givenNotLoggedIn() {
        BusinessException authException = new BusinessException(ErrorCode.UNAUTHORIZED);
        when(currentUserService.getRequiredUser()).thenThrow(authException);
        return authException;
    }

    @Nested
    @DisplayName("addFavorite")
    class AddFavoriteTests {

        @Test
        @DisplayName("성공: 아직 즐겨찾기하지 않은 이벤트면 (현재 유저, 이벤트)로 Favorite이 저장된다")
        void success() {
            User user = mockUser();
            Event event = mockEvent(1L);
            when(currentUserService.getRequiredUser()).thenReturn(user);
            when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
            when(favoriteRepository.findByUserAndEvent(user, event)).thenReturn(Optional.empty());

            favoriteService.addFavorite(1L);

            ArgumentCaptor<Favorite> captor = ArgumentCaptor.forClass(Favorite.class);
            verify(favoriteRepository, times(1)).save(captor.capture());
            assertSame(user, captor.getValue().getUser());
            assertSame(event, captor.getValue().getEvent());
        }

        @Test
        @DisplayName("실패: 로그인하지 않았으면 예외가 그대로 전파되고 이벤트/즐겨찾기 조회는 하지 않는다")
        void notLoggedIn() {
            BusinessException authException = givenNotLoggedIn();

            BusinessException thrown = assertThrows(BusinessException.class, () -> favoriteService.addFavorite(1L));

            assertSame(authException, thrown);
            verifyNoInteractions(eventRepository, favoriteRepository);
        }

        @Test
        @DisplayName("실패: 존재하지 않는 이벤트면 NOT_FOUND, 즐겨찾기 조회/저장은 하지 않는다")
        void eventNotFound() {
            when(currentUserService.getRequiredUser()).thenReturn(mockUser());
            when(eventRepository.findById(999L)).thenReturn(Optional.empty());

            assertBusinessException(() -> favoriteService.addFavorite(999L), ErrorCode.NOT_FOUND);

            verifyNoInteractions(favoriteRepository);
        }

        @Test
        @DisplayName("실패: 이미 즐겨찾기한 이벤트면 ALREADY_EXISTS, 저장하지 않는다")
        void alreadyFavorite() {
            User user = mockUser();
            Event event = mockEvent(1L);
            when(currentUserService.getRequiredUser()).thenReturn(user);
            when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
            when(favoriteRepository.findByUserAndEvent(user, event))
                    .thenReturn(Optional.of(buildFavorite(user, event)));

            assertBusinessException(() -> favoriteService.addFavorite(1L), ErrorCode.ALREADY_EXISTS);

            verify(favoriteRepository, never()).save(any(Favorite.class));
        }

        @Test
        @DisplayName("실패: 저장 시 unique 제약 위반(동시 요청)이 나면 ALREADY_EXISTS로 변환된다")
        void duplicateDetectedOnSave() {
            User user = mockUser();
            Event event = mockEvent(1L);
            when(currentUserService.getRequiredUser()).thenReturn(user);
            when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
            when(favoriteRepository.findByUserAndEvent(user, event)).thenReturn(Optional.empty());
            when(favoriteRepository.save(any(Favorite.class)))
                    .thenThrow(new DataIntegrityViolationException("uk_user_event"));

            assertBusinessException(() -> favoriteService.addFavorite(1L), ErrorCode.ALREADY_EXISTS);
        }
    }

    @Nested
    @DisplayName("deleteFavorite")
    class DeleteFavoriteTests {

        @Test
        @DisplayName("성공: 즐겨찾기가 존재하면 해당 Favorite이 삭제된다")
        void success() {
            User user = mockUser();
            Event event = mockEvent(1L);
            Favorite favorite = buildFavorite(user, event);
            when(currentUserService.getRequiredUser()).thenReturn(user);
            when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
            when(favoriteRepository.findByUserAndEvent(user, event)).thenReturn(Optional.of(favorite));

            favoriteService.deleteFavorite(1L);

            verify(favoriteRepository, times(1)).delete(favorite);
        }

        @Test
        @DisplayName("실패: 로그인하지 않았으면 예외가 그대로 전파되고 이벤트/즐겨찾기 조회는 하지 않는다")
        void notLoggedIn() {
            BusinessException authException = givenNotLoggedIn();

            BusinessException thrown = assertThrows(BusinessException.class, () -> favoriteService.deleteFavorite(1L));

            assertSame(authException, thrown);
            verifyNoInteractions(eventRepository, favoriteRepository);
        }

        @Test
        @DisplayName("실패: 존재하지 않는 이벤트면 NOT_FOUND, 즐겨찾기 조회/삭제는 하지 않는다")
        void eventNotFound() {
            when(currentUserService.getRequiredUser()).thenReturn(mockUser());
            when(eventRepository.findById(999L)).thenReturn(Optional.empty());

            assertBusinessException(() -> favoriteService.deleteFavorite(999L), ErrorCode.NOT_FOUND);

            verifyNoInteractions(favoriteRepository);
        }

        @Test
        @DisplayName("실패: 즐겨찾기하지 않은 이벤트면 NOT_FOUND, 삭제하지 않는다")
        void favoriteNotFound() {
            User user = mockUser();
            Event event = mockEvent(1L);
            when(currentUserService.getRequiredUser()).thenReturn(user);
            when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
            when(favoriteRepository.findByUserAndEvent(user, event)).thenReturn(Optional.empty());

            assertBusinessException(() -> favoriteService.deleteFavorite(1L), ErrorCode.NOT_FOUND);

            verify(favoriteRepository, never()).delete(any(Favorite.class));
        }
    }

    @Nested
    @DisplayName("getFavoriteList")
    class GetFavoriteListTests {

        @Test
        @DisplayName("성공: 현재 유저의 즐겨찾기에서 이벤트 ID만 뽑아 repository 순서대로 반환한다")
        void success() {
            User user = mockUser();
            when(currentUserService.getRequiredUser()).thenReturn(user);
            Favorite fav1 = buildFavorite(user, mockEvent(30L));
            Favorite fav2 = buildFavorite(user, mockEvent(10L));
            Favorite fav3 = buildFavorite(user, mockEvent(20L));
            when(favoriteRepository.findByUser(user)).thenReturn(List.of(
                    fav1,
                    fav2,
                    fav3
            ));

            List<Long> result = favoriteService.getFavoriteList();

            assertEquals(List.of(30L, 10L, 20L), result);
            verify(favoriteRepository).findByUser(user);
        }

        @Test
        @DisplayName("성공: 즐겨찾기가 없으면 빈 리스트 (null 아님)")
        void empty() {
            User user = mockUser();
            when(currentUserService.getRequiredUser()).thenReturn(user);
            when(favoriteRepository.findByUser(user)).thenReturn(List.of());

            List<Long> result = favoriteService.getFavoriteList();

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("실패: 로그인하지 않았으면 예외가 그대로 전파되고 조회하지 않는다")
        void notLoggedIn() {
            BusinessException authException = givenNotLoggedIn();

            BusinessException thrown = assertThrows(BusinessException.class, () -> favoriteService.getFavoriteList());

            assertSame(authException, thrown);
            verifyNoInteractions(favoriteRepository);
        }
    }
}