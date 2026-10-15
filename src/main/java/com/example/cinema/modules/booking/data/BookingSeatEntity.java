package com.example.cinema.modules.booking.data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "booking_seats")
class BookingSeatEntity {

    @Id
    private UUID id;

    @Column(name = "booking_id", nullable = false)
    private UUID bookingId;

    @Column(name = "showtime_id", nullable = false)
    private UUID showtimeId;

    @Column(name = "seat_id", nullable = false)
    private UUID seatId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    protected BookingSeatEntity() {
    }

    BookingSeatEntity(UUID id, UUID bookingId, UUID showtimeId, UUID seatId, BigDecimal price) {
        this.id = id;
        this.bookingId = bookingId;
        this.showtimeId = showtimeId;
        this.seatId = seatId;
        this.price = price;
    }

    UUID getSeatId() { return seatId; }
    BigDecimal getPrice() { return price; }
}
