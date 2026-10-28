package com.example.cinema.modules.theater.business;

import java.util.UUID;

public class ShowtimeHasBookingsException extends TheaterBusinessException {
    public ShowtimeHasBookingsException(UUID showtimeId) {
        super("Showtime has confirmed bookings: " + showtimeId);
    }
}
