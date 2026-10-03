package com.example.cinema.modules.theater.business;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Immutable showtime data supplied by Theater to Booking.
 */
public class ShowtimeSnapshot {

    private final UUID showtimeId;
    private final UUID auditoriumId;
    private final Instant startsAt;
    private final ShowtimeStatus status;
    private final BigDecimal basePrice;

    public ShowtimeSnapshot(
            UUID showtimeId,
            UUID auditoriumId,
            Instant startsAt,
            ShowtimeStatus status,
            BigDecimal basePrice
    ) {
        this.showtimeId = showtimeId;
        this.auditoriumId = auditoriumId;
        this.startsAt = startsAt;
        this.status = status;
        this.basePrice = basePrice;
    }

    public UUID getShowtimeId() {
        return showtimeId;
    }

    public UUID getAuditoriumId() {
        return auditoriumId;
    }

    public Instant getStartsAt() {
        return startsAt;
    }

    public ShowtimeStatus getStatus() {
        return status;
    }

    public BigDecimal getBasePrice() {
        return basePrice;
    }
}