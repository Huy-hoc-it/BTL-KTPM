package com.example.cinema.modules.theater.business;

import java.util.UUID;

public class AuditoriumHasFutureShowtimesException extends TheaterBusinessException {

    public AuditoriumHasFutureShowtimesException(UUID auditoriumId) {
        super("Auditorium has future showtimes: " + auditoriumId);
    }
}
