package com.example.cinema.modules.theater.business;

public class DuplicateSeatPositionException extends TheaterBusinessException {

    public DuplicateSeatPositionException(String rowLabel, int seatNumber) {
        super("Seat position already exists: " + rowLabel + seatNumber);
    }
}
