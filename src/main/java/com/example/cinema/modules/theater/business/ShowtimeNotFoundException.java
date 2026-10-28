package com.example.cinema.modules.theater.business;

import java.util.UUID;

public class ShowtimeNotFoundException extends TheaterBusinessException {

    public ShowtimeNotFoundException(UUID showtimeId) {
        super("Showtime not found: " + showtimeId);
    }
}
