package com.backend.notification.entity;

import com.backend.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "notification_setting")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationSetting {
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

    @Builder
    public NotificationSetting(User user) {
        this.user = user;
        this.updatedAt = LocalDateTime.now();
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
}
