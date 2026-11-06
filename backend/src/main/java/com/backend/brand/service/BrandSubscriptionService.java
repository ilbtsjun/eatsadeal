package com.backend.brand.service;

import com.backend.auth.service.CurrentUserService;
import com.backend.brand.entity.Brand;
import com.backend.brand.entity.BrandSubscription;
import com.backend.brand.repository.BrandRepository;
import com.backend.brand.repository.BrandSubscriptionRepository;
import com.backend.common.error.BusinessException;
import com.backend.common.error.ErrorCode;
import com.backend.common.log.CudLogging;
import com.backend.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BrandSubscriptionService {
    private final BrandSubscriptionRepository brandSubscriptionRepository;
    private final CurrentUserService currentUserService;
    private final BrandRepository brandRepository;

    @Transactional
    @CudLogging("브랜드 구독")
    public void subscribe(Long brandId) {
        User user = currentUserService.getRequiredUser();

        Brand brand = brandRepository.findById(brandId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        if (!brand.getIsActive()) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }

        Optional<BrandSubscription> brandSubscriptionOptional
                = brandSubscriptionRepository.findByUserAndBrand(user, brand);

        if (brandSubscriptionOptional.isPresent()) {
            throw new BusinessException(ErrorCode.ALREADY_EXISTS);
        }

        BrandSubscription brandSubscription = BrandSubscription.builder()
                .user(user)
                .brand(brand)
                .build();

        try {
            brandSubscriptionRepository.save(brandSubscription);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.ALREADY_EXISTS);
        }
    }

    @Transactional
    @CudLogging("브랜드 구독 해제")
    public void unsubscribe(Long brandId) {
        User user = currentUserService.getRequiredUser();

        Brand brand = brandRepository.findById(brandId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        Optional<BrandSubscription> brandSubscriptionOptional
                = brandSubscriptionRepository.findByUserAndBrand(user, brand);

        if (brandSubscriptionOptional.isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }

        brandSubscriptionRepository.delete(brandSubscriptionOptional.get());
    }

    @Transactional(readOnly = true)
    public List<Long> getMySubscription() {
        User user = currentUserService.getRequiredUser();
        return brandSubscriptionRepository.findActiveBrandIdsByUser(user);
    }
}
