package com.backend;

import com.backend.common.dto.UserRole;
import com.backend.user.dto.UserGender;
import com.backend.user.entity.User;
import com.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@Order(1)
@RequiredArgsConstructor
public class AdminInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.bootstrap-admin.email:}")
    private String adminEmail;

    @Value("${app.bootstrap-admin.password:}")
    private String adminPassword;


    @Override
    public void run(String... args) {
        if (adminEmail.isBlank() || adminPassword.isBlank()) {
            return;
        }

        if (userRepository.existsByEmail(adminEmail)) {
            return;
        }

        User admin = User.builder()
                .name("관리자")
                .email(adminEmail)
                .password(passwordEncoder.encode(adminPassword))
                .nickname("ADMIN")
                .phoneNumber("010-0000-0000")
                .gender(UserGender.MALE)
                .birth(LocalDate.of(1990, 1, 1))
                .build();
        admin.updateRole(UserRole.ADMIN);
        userRepository.save(admin);
    }
}