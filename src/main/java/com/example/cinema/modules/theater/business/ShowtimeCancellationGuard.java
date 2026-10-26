package com.example.cinema.modules.theater.business;

import java.util.UUID;

/** Theater-owned port implemented by Booking; called under the showtime lock. */
public interface ShowtimeCancellationGuard {
    void checkCancellationAllowed(UUID showtimeId);
}
