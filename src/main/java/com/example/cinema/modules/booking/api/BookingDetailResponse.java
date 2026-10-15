package com.example.cinema.modules.booking.api;

import com.example.cinema.modules.booking.business.Booking;
import com.example.cinema.modules.booking.business.PaymentStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class BookingDetailResponse {

    private final UUID bookingId;
    private final UUID showtimeId;
    private final List<BookingSeatResponse> seats;
    private final BigDecimal totalAmount;
    private final PaymentStatus paymentStatus;
    private final Instant createdAt;

    public BookingDetailResponse(
            UUID bookingId,
            UUID showtimeId,
            List<BookingSeatResponse> seats,
            BigDecimal totalAmount,
            PaymentStatus paymentStatus,
            Instant createdAt
    ) {
        this.bookingId = bookingId;
        this.showtimeId = showtimeId;
        this.seats = List.copyOf(seats);
        this.totalAmount = totalAmount;
        this.paymentStatus = paymentStatus;
        this.createdAt = createdAt;
    }

    public static BookingDetailResponse from(Booking booking) {
        return new BookingDetailResponse(
                booking.getBookingId(), booking.getShowtimeId(),
                booking.getSeats().stream().map(BookingSeatResponse::from).toList(),
                booking.getTotalAmount(), booking.getPaymentStatus(), booking.getCreatedAt());
    }

    public UUID getBookingId() { return bookingId; }
    public UUID getShowtimeId() { return showtimeId; }
    public List<BookingSeatResponse> getSeats() { return seats; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public PaymentStatus getPaymentStatus() { return paymentStatus; }
    public Instant getCreatedAt() { return createdAt; }
}
