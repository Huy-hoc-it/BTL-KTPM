package com.example.cinema.modules.theater.business;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record Showtime(
        UUID id,
        UUID movieId,
        UUID auditoriumId,
        Instant startsAt,
        Instant endsAt,
        BigDecimal basePrice,
        ShowtimeStatus status,
        Instant createdAt,
        Instant updatedAt
) {

    public Showtime {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(movieId, "movieId must not be null");
        Objects.requireNonNull(auditoriumId, "auditoriumId must not be null");
        Objects.requireNonNull(startsAt, "startsAt must not be null");
        Objects.requireNonNull(endsAt, "endsAt must not be null");
        Objects.requireNonNull(basePrice, "basePrice must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        if (!endsAt.isAfter(startsAt)) {
            throw new IllegalArgumentException("endsAt must be after startsAt");
        }
        if (basePrice.signum() < 0) {
            throw new IllegalArgumentException("basePrice must not be negative");
        }
    }
}
