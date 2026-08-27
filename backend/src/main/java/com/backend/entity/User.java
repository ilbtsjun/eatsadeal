package com.backend.entity;

import com.backend.config.UserRole;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@Entity
@Table(name="users")
@NoArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    @NotBlank
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
    @NotBlank
    private String phoneNumber;

    @Column
    @Enumerated(EnumType.STRING)
    private UserRole role;

    @Column
    private Boolean gender;

    @Column
    private LocalDate birth;

    @Builder
    public User(String name, String email, String password, String nickname, String phoneNumber,
                Boolean gender, LocalDate birth){
        this.name = name;
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.phoneNumber = phoneNumber;
        this.role = UserRole.USER;
        this.gender = gender;
        this.birth = birth;
    }

    public void updateRole(UserRole role){
        this.role = role;
    }
}
