package com.example.cinema.modules.theater.business;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record CreateShowtimeCommand(
        UUID movieId,
        UUID auditoriumId,
        Instant startsAt,
        BigDecimal basePrice
) {

    public CreateShowtimeCommand {
        Objects.requireNonNull(movieId, "movieId must not be null");
        Objects.requireNonNull(auditoriumId, "auditoriumId must not be null");
        Objects.requireNonNull(startsAt, "startsAt must not be null");
        Objects.requireNonNull(basePrice, "basePrice must not be null");
        if (basePrice.signum() < 0) {
            throw new IllegalArgumentException("basePrice must not be negative");
        }
    }
}
