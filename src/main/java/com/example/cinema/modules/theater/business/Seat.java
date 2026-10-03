package com.example.cinema.modules.theater.business;

import java.util.Objects;
import java.util.UUID;

public record Seat(
        UUID id,
        UUID auditoriumId,
        String rowLabel,
        int seatNumber,
        SeatType type
) {

    public Seat {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(auditoriumId, "auditoriumId must not be null");
        Objects.requireNonNull(rowLabel, "rowLabel must not be null");
        Objects.requireNonNull(type, "type must not be null");
        if (rowLabel.isBlank()) {
            throw new IllegalArgumentException("rowLabel must not be blank");
        }
        if (seatNumber <= 0) {
            throw new IllegalArgumentException("seatNumber must be greater than zero");
        }
    }
}
