package com.example.cinema.modules.theater.business;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Public business contract used by Booking to query showtime and seat data.
 */
public interface ShowtimeModuleApi {

    /**
     * Finds a showtime snapshot without exposing Theater persistence models.
     *
     * @param showtimeId showtime identifier
     * @return an empty result when the showtime does not exist
     */
    Optional<ShowtimeSnapshot> findShowtime(UUID showtimeId);

    /**
     * Immutable showtime data required by booking rules and seat availability.
     */
    record ShowtimeSnapshot(
            UUID showtimeId,
            UUID auditoriumId,
            Instant startsAt,
            BigDecimal basePrice,
            ShowtimeState status,
            List<SeatSnapshot> seats
    ) {

        public ShowtimeSnapshot {
            Objects.requireNonNull(showtimeId, "showtimeId must not be null");
            Objects.requireNonNull(auditoriumId, "auditoriumId must not be null");
            Objects.requireNonNull(startsAt, "startsAt must not be null");
            Objects.requireNonNull(basePrice, "basePrice must not be null");
            Objects.requireNonNull(status, "status must not be null");
            Objects.requireNonNull(seats, "seats must not be null");
            if (basePrice.signum() < 0) {
                throw new IllegalArgumentException("basePrice must not be negative");
            }
            seats = List.copyOf(seats);
        }
    }

    /**
     * Immutable seat data needed for price calculation and seat responses.
     */
    record SeatSnapshot(UUID seatId, String rowLabel, int seatNumber, SeatKind type) {

        public SeatSnapshot {
            Objects.requireNonNull(seatId, "seatId must not be null");
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

    enum ShowtimeState {
        SCHEDULED,
        CANCELLED,
        FINISHED
    }

    enum SeatKind {
        STANDARD,
        VIP
    }
}
