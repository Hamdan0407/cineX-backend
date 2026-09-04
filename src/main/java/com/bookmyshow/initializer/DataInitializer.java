package com.bookmyshow.initializer;

import com.bookmyshow.entity.User;
import com.bookmyshow.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @PostConstruct
    @Transactional
    public void initData() {
        if (!userRepository.existsByEmail("admin@cinex.com")) {
            User admin = new User();
            admin.setName("CineX Admin");
            admin.setEmail("admin@cinex.com");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setPhone("9999999999");
            admin.setRole("ADMIN");
            userRepository.save(admin);
            log.info("Default Admin User created: admin@cinex.com / admin123");
        }

        if (!userRepository.existsByEmail("user@cinex.com")) {
            User user = new User();
            user.setName("CineX User");
            user.setEmail("user@cinex.com");
            user.setPassword(passwordEncoder.encode("user123"));
            user.setPhone("8888888888");
            user.setRole("USER");
            userRepository.save(user);
            log.info("Default Test User created: user@cinex.com / user123");
        }

        log.info("Default users ready. Movie/theatre/show catalogue is managed by CinemaCatalogUpgrade.");
    }
}
