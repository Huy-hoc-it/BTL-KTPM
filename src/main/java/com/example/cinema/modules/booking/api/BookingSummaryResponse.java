package com.example.cinema.modules.booking.api;

import com.example.cinema.modules.booking.business.Booking;
import java.time.Instant;
import java.util.UUID;

public class BookingSummaryResponse {

    private final UUID bookingId;
    private final Instant createdAt;

    public BookingSummaryResponse(UUID bookingId, Instant createdAt) {
        this.bookingId = bookingId;
        this.createdAt = createdAt;
    }

    public static BookingSummaryResponse from(Booking booking) {
        return new BookingSummaryResponse(booking.getBookingId(), booking.getCreatedAt());
    }

    public UUID getBookingId() { return bookingId; }
    public Instant getCreatedAt() { return createdAt; }
}
