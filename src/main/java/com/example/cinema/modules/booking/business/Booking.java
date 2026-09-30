package com.example.cinema.modules.booking.business;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class Booking {

    private final UUID bookingId;
    private final UUID userId;
    private final UUID showtimeId;
    private final List<bookingSeat> seats;
    private final BigDecimal totalAmount;

    public Booking(
            UUID userId,
            UUID showtimeId,
            List<bookingSeat> seats,
            BigDecimal totalAmount
    ) {
        this.bookingId = UUID.randomUUID();
        this.userId = userId;
        this.showtimeId = showtimeId;
        this.seats = List.copyOf(seats);
        this.totalAmount = totalAmount;
    }

    public UUID getBookingId() {
        return bookingId;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getShowtimeId() {
        return showtimeId;
    }

    public List<bookingSeat> getSeats() {
        return seats;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }
}