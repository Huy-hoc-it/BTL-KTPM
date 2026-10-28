package com.example.cinema.modules.theater.business;

import java.util.UUID;

public class ScheduleOverlapException extends TheaterBusinessException {

    public ScheduleOverlapException(UUID auditoriumId) {
        super("Showtime overlaps an existing schedule in auditorium: " + auditoriumId);
    }
}
