package com.example.cinema.modules.theater.business;

public class AuditoriumNameAlreadyExistsException extends TheaterBusinessException {

    public AuditoriumNameAlreadyExistsException(String name) {
        super("Auditorium name already exists: " + name);
    }
}
