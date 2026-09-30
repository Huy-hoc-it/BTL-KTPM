package com.example.cinema.modules.theater.business;

import java.util.UUID;

public class AuditoriumNotFoundException extends TheaterBusinessException {

    public AuditoriumNotFoundException(UUID auditoriumId) {
        super("Auditorium not found: " + auditoriumId);
    }
}
