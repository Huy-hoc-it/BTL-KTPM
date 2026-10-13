package com.example.cinema.modules.theater.api;

import com.example.cinema.modules.theater.business.ShowtimeStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ShowtimeResponse(
        UUID id,
        UUID movieId,
        UUID auditoriumId,
        Instant startsAt,
        Instant endsAt,
        BigDecimal basePrice,
        ShowtimeStatus status
) {
}
