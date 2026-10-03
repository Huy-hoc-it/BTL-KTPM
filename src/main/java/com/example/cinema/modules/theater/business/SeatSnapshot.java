package com.example.cinema.modules.theater.business;

import java.util.UUID;

/**
 * Immutable seat data needed by Booking to validate ownership and calculate price.
 */
public class SeatSnapshot {

    private final UUID seatId;
    private final UUID auditoriumId;
    private final SeatType type;

    public SeatSnapshot(UUID seatId, UUID auditoriumId, SeatType type) {
        this.seatId = seatId;
        this.auditoriumId = auditoriumId;
        this.type = type;
    }

    public UUID getSeatId() {
        return seatId;
    }

    public UUID getAuditoriumId() {
        return auditoriumId;
    }

    public SeatType getType() {
        return type;
    }
}