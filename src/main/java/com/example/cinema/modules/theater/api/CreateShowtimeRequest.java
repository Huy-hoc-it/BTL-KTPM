package com.example.cinema.modules.theater.api;

import com.example.cinema.modules.theater.business.CreateShowtimeCommand;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CreateShowtimeRequest(
        @NotNull UUID movieId,
        @NotNull UUID auditoriumId,
        @NotNull @Future Instant startsAt,
        @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal basePrice
) {

    public CreateShowtimeCommand toCommand() {
        return new CreateShowtimeCommand(movieId, auditoriumId, startsAt, basePrice);
    }
}
