package com.example.cinema.modules.identity.business;

import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class IdentityService {
    private static final Logger log = LoggerFactory.getLogger(IdentityService.class);
    // ponytail: cost 10 matches the current BCrypt encoder; update this hash if its strength changes.
    private static final String DUMMY_PASSWORD_HASH = "$2a$10$m37lChME3c62UozdROlHWORxEtta9UZ/NVzhsh5Rsuv4hKQa81tPe";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public IdentityService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User register(String rawUsername, String password) {
        String username = AccountPolicy.normalizeUsername(rawUsername);
        if (!AccountPolicy.isValidUsername(username)) {
            throw new InvalidRegistrationException();
        }
        if (!AccountPolicy.isValidPassword(password)) {
            throw new InvalidRegistrationException();
        }
        if (userRepository.findByUsername(username).isPresent()) {
            throw new UsernameAlreadyExistsException();
        }

        Instant now = Instant.now();
        return userRepository.save(new User(
                UUID.randomUUID(), username, passwordEncoder.encode(password), User.Role.CUSTOMER, now, now));
    }

    public AuthenticatedUser login(String rawUsername, String password) {
        if (password == null) {
            log.info("Login failed");
            throw new InvalidCredentialsException();
        }

        String username = AccountPolicy.normalizeUsername(rawUsername);
        User user = AccountPolicy.isValidUsername(username) ? userRepository.findByUsername(username).orElse(null) : null;
        String hash = user == null ? DUMMY_PASSWORD_HASH : user.passwordHash();
        if (!passwordEncoder.matches(password, hash) || user == null) {
            log.info("Login failed");
            throw new InvalidCredentialsException();
        }
        log.info("Login succeeded userId={}", user.id());
        return new AuthenticatedUser(user.id(), user.role());
    }
}
