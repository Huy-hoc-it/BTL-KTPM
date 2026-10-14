package com.example.cinema.modules.booking.data;

import com.example.cinema.modules.booking.business.PaymentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "bookings")
class BookingEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "showtime_id", nullable = false)
    private UUID showtimeId;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 20)
    private PaymentStatus paymentStatus;

    protected BookingEntity() {
    }

    BookingEntity(UUID id, UUID userId, UUID showtimeId, BigDecimal totalAmount, Instant createdAt, PaymentStatus paymentStatus) {
        this.id = id;
        this.userId = userId;
        this.showtimeId = showtimeId;
        this.totalAmount = totalAmount;
        this.createdAt = createdAt;
        this.paymentStatus = paymentStatus;
    }

    UUID getId() { return id; }
    UUID getUserId() { return userId; }
    UUID getShowtimeId() { return showtimeId; }
    BigDecimal getTotalAmount() { return totalAmount; }
    Instant getCreatedAt() { return createdAt; }
    PaymentStatus getPaymentStatus() { return paymentStatus; }
}
