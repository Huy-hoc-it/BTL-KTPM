package com.example.cinema.modules.theater.business;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record Auditorium(
        UUID id,
        String name,
        AuditoriumStatus status,
        Instant deletedAt,
        Instant createdAt,
        Instant updatedAt
) {

    public Auditorium {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
    }

    public boolean isActive() {
        return status == AuditoriumStatus.ACTIVE && deletedAt == null;
    }
}
