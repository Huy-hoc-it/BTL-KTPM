package com.example.cinema.config;

import com.example.cinema.modules.identity.business.AccountPolicy;
import com.example.cinema.modules.identity.business.User;
import com.example.cinema.modules.identity.business.UserRepository;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Profile("!prod & (local | test)")
public class DemoAdminSeeder implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(DemoAdminSeeder.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String configuredUsername;
    private final String configuredPassword;

    public DemoAdminSeeder(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${DEMO_ADMIN_USERNAME:}") String configuredUsername,
            @Value("${DEMO_ADMIN_PASSWORD:}") String configuredPassword
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.configuredUsername = configuredUsername;
        this.configuredPassword = configuredPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (configuredUsername == null || configuredUsername.isBlank()
                || configuredPassword == null || configuredPassword.isEmpty()) {
            return;
        }

        String username = AccountPolicy.normalizeUsername(configuredUsername);
        if (!AccountPolicy.isValidUsername(username)) {
            throw new IllegalStateException(
                    "DEMO_ADMIN_USERNAME must be 3-50 letters, digits, dots, underscores, or hyphens");
        }
        if (!AccountPolicy.isValidPassword(configuredPassword)) {
            throw new IllegalStateException("DEMO_ADMIN_PASSWORD must be 8-16 characters");
        }

        var existingUser = userRepository.findByUsername(username);
        if (existingUser.isPresent()) {
            if (existingUser.get().role() != User.Role.ADMIN) {
                throw new IllegalStateException(
                        "DEMO_ADMIN_USERNAME already belongs to a non-Admin user; choose another username");
            }
            return;
        }

        Instant now = Instant.now();
        userRepository.save(new User(
                UUID.randomUUID(), username, passwordEncoder.encode(configuredPassword), User.Role.ADMIN, now, now));
        log.info("Created demo Admin account username={}", username);
    }
}
