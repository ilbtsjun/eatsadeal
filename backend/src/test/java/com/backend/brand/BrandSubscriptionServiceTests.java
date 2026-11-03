package com.backend.brand;

import com.backend.auth.service.CurrentUserService;
import com.backend.brand.entity.Brand;
import com.backend.brand.entity.BrandSubscription;
import com.backend.brand.repository.BrandRepository;
import com.backend.brand.repository.BrandSubscriptionRepository;
import com.backend.brand.service.BrandSubscriptionService;
import com.backend.common.error.BusinessException;
import com.backend.common.error.ErrorCode;
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
class BrandSubscriptionServiceTests {

    @Mock
    private BrandSubscriptionRepository brandSubscriptionRepository;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private BrandRepository brandRepository;

    @InjectMocks
    private BrandSubscriptionService brandSubscriptionService;

    private User mockUser() {
        return mock(User.class);
    }

    private Brand mockBrand() {
        return mock(Brand.class);
    }

    private Brand mockBrand(boolean active) {
        Brand brand = mock(Brand.class);
        when(brand.getIsActive()).thenReturn(active);
        return brand;
    }

    private BrandSubscription buildSubscription(User user, Brand brand) {
        return BrandSubscription.builder().user(user).brand(brand).build();
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
    @DisplayName("subscribe")
    class SubscribeTests {

        @Test
        @DisplayName("성공: 활성 브랜드를 아직 구독하지 않았으면 (현재 유저, 브랜드)로 BrandSubscription이 저장된다")
        void success() {
            User user = mockUser();
            Brand brand = mockBrand(true);
            when(currentUserService.getRequiredUser()).thenReturn(user);
            when(brandRepository.findById(1L)).thenReturn(Optional.of(brand));
            when(brandSubscriptionRepository.findByUserAndBrand(user, brand)).thenReturn(Optional.empty());

            brandSubscriptionService.subscribe(1L);

            ArgumentCaptor<BrandSubscription> captor = ArgumentCaptor.forClass(BrandSubscription.class);
            verify(brandSubscriptionRepository, times(1)).save(captor.capture());
            assertSame(user, captor.getValue().getUser());
            assertSame(brand, captor.getValue().getBrand());
            assertNotNull(captor.getValue().getCreatedAt());
        }

        @Test
        @DisplayName("실패: 로그인하지 않았으면 예외가 그대로 전파되고 브랜드/구독 조회는 하지 않는다")
        void notLoggedIn() {
            BusinessException authException = givenNotLoggedIn();

            BusinessException thrown = assertThrows(BusinessException.class, () -> brandSubscriptionService.subscribe(1L));

            assertSame(authException, thrown);
            verifyNoInteractions(brandRepository, brandSubscriptionRepository);
        }

        @Test
        @DisplayName("실패: 존재하지 않는 브랜드면 NOT_FOUND, 구독 조회/저장은 하지 않는다")
        void brandNotFound() {
            when(currentUserService.getRequiredUser()).thenReturn(mockUser());
            when(brandRepository.findById(999L)).thenReturn(Optional.empty());

            assertBusinessException(() -> brandSubscriptionService.subscribe(999L), ErrorCode.NOT_FOUND);

            verifyNoInteractions(brandSubscriptionRepository);
        }

        @Test
        @DisplayName("실패: 비활성 브랜드면 NOT_FOUND, 구독 조회/저장은 하지 않는다")
        void inactiveBrand() {
            Brand brand = mockBrand(false);
            when(currentUserService.getRequiredUser()).thenReturn(mockUser());
            when(brandRepository.findById(1L)).thenReturn(Optional.of(brand));

            assertBusinessException(() -> brandSubscriptionService.subscribe(1L), ErrorCode.NOT_FOUND);

            verifyNoInteractions(brandSubscriptionRepository);
        }

        @Test
        @DisplayName("실패: 이미 구독한 브랜드면 ALREADY_EXISTS, 저장하지 않는다")
        void alreadySubscribed() {
            User user = mockUser();
            Brand brand = mockBrand(true);
            when(currentUserService.getRequiredUser()).thenReturn(user);
            when(brandRepository.findById(1L)).thenReturn(Optional.of(brand));
            when(brandSubscriptionRepository.findByUserAndBrand(user, brand))
                    .thenReturn(Optional.of(buildSubscription(user, brand)));

            assertBusinessException(() -> brandSubscriptionService.subscribe(1L), ErrorCode.ALREADY_EXISTS);

            verify(brandSubscriptionRepository, never()).save(any(BrandSubscription.class));
        }

        @Test
        @DisplayName("실패: 저장 시 unique 제약 위반(동시 요청)이 나면 ALREADY_EXISTS로 변환된다")
        void duplicateDetectedOnSave() {
            User user = mockUser();
            Brand brand = mockBrand(true);
            when(currentUserService.getRequiredUser()).thenReturn(user);
            when(brandRepository.findById(1L)).thenReturn(Optional.of(brand));
            when(brandSubscriptionRepository.findByUserAndBrand(user, brand)).thenReturn(Optional.empty());
            when(brandSubscriptionRepository.save(any(BrandSubscription.class)))
                    .thenThrow(new DataIntegrityViolationException("uk_brand_subscription_user_brand"));

            assertBusinessException(() -> brandSubscriptionService.subscribe(1L), ErrorCode.ALREADY_EXISTS);
        }
    }

    @Nested
    @DisplayName("unsubscribe")
    class UnsubscribeTests {

        @Test
        @DisplayName("성공: 구독이 존재하면 해당 BrandSubscription이 삭제된다")
        void success() {
            User user = mockUser();
            Brand brand = mockBrand();
            BrandSubscription subscription = buildSubscription(user, brand);
            when(currentUserService.getRequiredUser()).thenReturn(user);
            when(brandRepository.findById(1L)).thenReturn(Optional.of(brand));
            when(brandSubscriptionRepository.findByUserAndBrand(user, brand)).thenReturn(Optional.of(subscription));

            brandSubscriptionService.unsubscribe(1L);

            verify(brandSubscriptionRepository, times(1)).delete(subscription);
        }

        @Test
        @DisplayName("성공: 비활성 브랜드여도 구독이 존재하면 해제할 수 있다")
        void inactiveBrandCanUnsubscribe() {
            User user = mockUser();
            Brand brand = mockBrand();
            BrandSubscription subscription = buildSubscription(user, brand);
            when(currentUserService.getRequiredUser()).thenReturn(user);
            when(brandRepository.findById(1L)).thenReturn(Optional.of(brand));
            when(brandSubscriptionRepository.findByUserAndBrand(user, brand)).thenReturn(Optional.of(subscription));

            brandSubscriptionService.unsubscribe(1L);

            verify(brand, never()).getIsActive();
            verify(brandSubscriptionRepository, times(1)).delete(subscription);
        }

        @Test
        @DisplayName("실패: 로그인하지 않았으면 예외가 그대로 전파되고 브랜드/구독 조회는 하지 않는다")
        void notLoggedIn() {
            BusinessException authException = givenNotLoggedIn();

            BusinessException thrown = assertThrows(BusinessException.class, () -> brandSubscriptionService.unsubscribe(1L));

            assertSame(authException, thrown);
            verifyNoInteractions(brandRepository, brandSubscriptionRepository);
        }

        @Test
        @DisplayName("실패: 존재하지 않는 브랜드면 NOT_FOUND, 구독 조회/삭제는 하지 않는다")
        void brandNotFound() {
            when(currentUserService.getRequiredUser()).thenReturn(mockUser());
            when(brandRepository.findById(999L)).thenReturn(Optional.empty());

            assertBusinessException(() -> brandSubscriptionService.unsubscribe(999L), ErrorCode.NOT_FOUND);

            verifyNoInteractions(brandSubscriptionRepository);
        }

        @Test
        @DisplayName("실패: 구독하지 않은 브랜드면 NOT_FOUND, 삭제하지 않는다")
        void subscriptionNotFound() {
            User user = mockUser();
            Brand brand = mockBrand();
            when(currentUserService.getRequiredUser()).thenReturn(user);
            when(brandRepository.findById(1L)).thenReturn(Optional.of(brand));
            when(brandSubscriptionRepository.findByUserAndBrand(user, brand)).thenReturn(Optional.empty());

            assertBusinessException(() -> brandSubscriptionService.unsubscribe(1L), ErrorCode.NOT_FOUND);

            verify(brandSubscriptionRepository, never()).delete(any(BrandSubscription.class));
        }
    }

    @Nested
    @DisplayName("getMySubscription")
    class GetMySubscriptionTests {

        @Test
        @DisplayName("성공: 활성 브랜드 ID를 repository 순서대로 반환한다")
        void success() {
            User user = mockUser();
            when(currentUserService.getRequiredUser()).thenReturn(user);
            when(brandSubscriptionRepository.findActiveBrandIdsByUser(user)).thenReturn(List.of(1L, 3L, 7L));

            List<Long> result = brandSubscriptionService.getMySubscription();

            assertEquals(List.of(1L, 3L, 7L), result);
            verify(brandSubscriptionRepository).findActiveBrandIdsByUser(user);
        }

        @Test
        @DisplayName("성공: 구독한 활성 브랜드가 없으면 빈 리스트 (null 아님)")
        void empty() {
            User user = mockUser();
            when(currentUserService.getRequiredUser()).thenReturn(user);
            when(brandSubscriptionRepository.findActiveBrandIdsByUser(user)).thenReturn(List.of());

            List<Long> result = brandSubscriptionService.getMySubscription();

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("실패: 로그인하지 않았으면 예외가 그대로 전파되고 조회하지 않는다")
        void notLoggedIn() {
            BusinessException authException = givenNotLoggedIn();

            BusinessException thrown = assertThrows(BusinessException.class, () -> brandSubscriptionService.getMySubscription());

            assertSame(authException, thrown);
            verifyNoInteractions(brandSubscriptionRepository);
        }
    }
}
