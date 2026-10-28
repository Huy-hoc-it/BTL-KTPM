package com.example.cinema.modules.identity.business;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository {
    /** Saves a user; throws UsernameAlreadyExistsException if the username is taken. */
    User save(User user);

    Optional<User> findById(UUID id);

    Optional<User> findByUsername(String username);
}
