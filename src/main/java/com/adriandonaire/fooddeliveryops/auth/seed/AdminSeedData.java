package com.adriandonaire.fooddeliveryops.auth.seed;

import com.adriandonaire.fooddeliveryops.auth.AppUser;
import com.adriandonaire.fooddeliveryops.auth.UserRole;
import com.adriandonaire.fooddeliveryops.auth.repository.AppUserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AdminSeedData {

    @Bean
    CommandLineRunner seedAdminUser(AppUserRepository appUserRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            if (appUserRepository.existsByUsername("admin")) {
                return;
            }

            AppUser admin = new AppUser("admin", passwordEncoder.encode("admin123"), UserRole.ADMIN);
            appUserRepository.save(admin);
        };
    }
}