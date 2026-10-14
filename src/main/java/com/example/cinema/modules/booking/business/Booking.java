package com.example.cinema.modules.booking.business;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class Booking {

    private final UUID bookingId;
    private final UUID userId;
    private final UUID showtimeId;
    private final List<bookingSeat> seats;
    private final BigDecimal totalAmount;
    private final Instant createdAt;
    private final PaymentStatus paymentStatus;

    public Booking(
            UUID userId,
            UUID showtimeId,
            List<bookingSeat> seats,
            BigDecimal totalAmount
    ) {
        this(UUID.randomUUID(), userId, showtimeId, seats, totalAmount, Instant.now(), PaymentStatus.PENDING);
    }

    public Booking(
            UUID bookingId,
            UUID userId,
            UUID showtimeId,
            List<bookingSeat> seats,
            BigDecimal totalAmount
    ) {
        this(bookingId, userId, showtimeId, seats, totalAmount, Instant.now(), PaymentStatus.PENDING);
    }

    public Booking(
            UUID bookingId,
            UUID userId,
            UUID showtimeId,
            List<bookingSeat> seats,
            BigDecimal totalAmount,
            Instant createdAt,
            PaymentStatus paymentStatus
    ) {
        this.bookingId = bookingId;
        this.userId = userId;
        this.showtimeId = showtimeId;
        this.seats = List.copyOf(seats);
        this.totalAmount = totalAmount;
        this.createdAt = createdAt;
        this.paymentStatus = paymentStatus;
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

    public Instant getCreatedAt() { return createdAt; }

    public PaymentStatus getPaymentStatus() { return paymentStatus; }
}
