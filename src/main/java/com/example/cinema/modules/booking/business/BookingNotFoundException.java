package com.example.cinema.modules.booking.business;

import java.util.UUID;

public class BookingNotFoundException extends RuntimeException {

    public BookingNotFoundException(UUID bookingId) {
        super("Booking was not found: " + bookingId);
    }
}
