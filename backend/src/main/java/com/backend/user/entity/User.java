package com.backend.user.entity;

import com.backend.user.dto.UserGender;
import com.backend.user.dto.UserStatus;
import com.backend.common.UserRole;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Getter
@Entity
@Table(name="users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private String name;

    @Column(unique = true)
    @NotBlank
    private String email;

    @Column
    @NotBlank
    private String password;

    @Column(unique = true)
    @NotBlank
    private String nickname;

    @Column
    private String phoneNumber;

    @Column
    @Enumerated(EnumType.STRING)
    private UserRole role;

    @Column
    @Enumerated(EnumType.STRING)
    private UserGender gender;

    @Column
    private LocalDate birth;

    @Column
    @Enumerated(EnumType.STRING)
    private UserStatus userStatus;

    @Column
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime updatedAt = null;

    @Column
    private LocalDateTime lastLoginAt = null;

    @Column
    private LocalDateTime suspendedAt = null;

    @Column
    private LocalDateTime suspendedUntil = null;

    @Column
    private String suspendingReason;

    @Builder
    public User(String name, String email, String password, String nickname, String phoneNumber,
                UserGender gender, LocalDate birth){
        this.name = name;
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.phoneNumber = phoneNumber;
        this.role = UserRole.USER;
        this.gender = gender;
        this.birth = birth;
        this.userStatus = UserStatus.ACTIVE;
        this.createdAt = LocalDateTime.now();
    }

    public void updateRole(UserRole role){
        this.role = role;
    }

    public void login(){
        this.lastLoginAt = LocalDateTime.now();
    }

    public void update(String name, String nickname, String phoneNumber, LocalDate birth){
        this.name = name;
        this.nickname = nickname;
        this.birth = birth;
        this.phoneNumber = phoneNumber;
        this.updatedAt = LocalDateTime.now();
    }

    public void updatePassword(String password){
        this.password = password;
        this.updatedAt = LocalDateTime.now();
    }

    public void withdrawn(){
        this.userStatus = UserStatus.WITHDRAWN;
        this.email = "withdrawn_" + this.id + "@deleted.local";
        this.nickname = "탈퇴한 사용자";
        this.phoneNumber = null;
        this.password = null;
    }

    public void suspend(Long suspendTime, String suspendingReason){
        this.suspendedAt = LocalDateTime.now();
        this.suspendedUntil = LocalDateTime.now().plus(suspendTime, ChronoUnit.DAYS);
        this.suspendingReason = suspendingReason.trim();
        this.userStatus = UserStatus.SUSPEND;
    }

    public boolean hasActiveSuspension(LocalDateTime now) {
        if (userStatus != UserStatus.SUSPEND) {
            return false;
        }
        return suspendedUntil == null || suspendedUntil.isAfter(now);
    }

    public void releaseSuspend(){
        this.suspendedAt = null;
        this.suspendedUntil = null;
        this.suspendingReason = null;
        this.userStatus = UserStatus.ACTIVE;
    }

    public boolean releaseIfExpired(LocalDateTime now) {
        if (userStatus == UserStatus.SUSPEND && suspendedUntil != null && !suspendedUntil.isAfter(now)) {
            this.releaseSuspend();
            return true;
        }

        return false;
    }
}
