package com.backend.notification.entity;

import com.backend.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@Getter
@Entity
@Table(name = "notification_setting")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationSetting {
    private static final SecureRandom TOKEN_RANDOM = new SecureRandom();

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false)
    private Boolean marketingAgreed = false;

    private LocalDateTime marketingAgreedAt;

    @Column(nullable = false)
    private Boolean emailAgreed = false;

    private LocalDateTime emailAgreedAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(nullable = false, unique = true, length = 64)
    private String unsubscribeToken;

    private LocalDateTime consentNotifiedAt;

    @Builder
    public NotificationSetting(User user) {
        this.user = user;
        this.updatedAt = LocalDateTime.now();
        this.unsubscribeToken = newUnsubscribeToken();
    }

    public static String newUnsubscribeToken() {
        byte[] bytes = new byte[32];
        TOKEN_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public void agreeMarketing(boolean agreed) {
        this.marketingAgreed = agreed;
        this.marketingAgreedAt = agreed ? LocalDateTime.now() : null;
        this.updatedAt = LocalDateTime.now();
    }

    public void agreeEmail(boolean agreed) {
        this.emailAgreed = agreed;
        this.emailAgreedAt = agreed ? LocalDateTime.now() : null;
        this.updatedAt = LocalDateTime.now();
    }

    public void markConsentNotified(LocalDateTime notifiedAt) {
        this.consentNotifiedAt = notifiedAt;
    }
}
