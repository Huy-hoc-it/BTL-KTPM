package com.example.cinema.modules.identity.business;

import java.time.Instant;
import java.util.UUID;

public record User(
        UUID id,
        String username,
        String passwordHash,
        Role role,
        Instant createdAt,
        Instant updatedAt
) {
    @Override
    public String toString() {
        return "User[id=" + id + ", username=" + username + ", role=" + role + "]";
    }

    public enum Role {
        CUSTOMER,
        ADMIN
    }
}
