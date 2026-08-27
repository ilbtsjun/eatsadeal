package com.backend;

import com.backend.entity.User;
import com.backend.config.UserRole;
import com.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class AdminInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        String adminEmail = "admin@example.com";

        if (!userRepository.existsByEmail(adminEmail)) {
            User admin = User.builder()
                    .name("관리자")
                    .email(adminEmail)
                    .password(passwordEncoder.encode("temp"))
                    .nickname("ADMIN")
                    .phoneNumber("010-0000-0000")
                    .gender(true)
                    .birth(LocalDate.of(1990, 1, 1))
                    .build();
            admin.updateRole(UserRole.ADMIN);
            userRepository.save(admin);
        }
    }
}