package com.backend.notification.entity;

import com.backend.notification.dto.ConsentSource;
import com.backend.notification.dto.ConsentType;
import com.backend.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "consent_history")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ConsentHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private ConsentType consentType;

    @Column(nullable = false)
    private Boolean agreed;

    @Column(nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private ConsentSource source;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Builder
    public ConsentHistory(User user, ConsentType consentType, Boolean agreed, ConsentSource source) {
        this.user = user;
        this.consentType = consentType;
        this.agreed = agreed;
        this.source = source;
        this.createdAt = LocalDateTime.now();
    }
}
