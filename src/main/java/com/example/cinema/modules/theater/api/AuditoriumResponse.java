package com.example.cinema.modules.theater.api;

import com.example.cinema.modules.theater.business.AuditoriumStatus;
import com.example.cinema.modules.theater.business.SeatType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AuditoriumResponse(
        UUID id,
        String name,
        AuditoriumStatus status,
        int capacity,
        Instant createdAt,
        Instant updatedAt,
        List<SeatResponse> seats
) {

    public AuditoriumResponse {
        seats = List.copyOf(seats);
    }

    public record SeatResponse(UUID id, String row, int number, SeatType type) {
    }
}
