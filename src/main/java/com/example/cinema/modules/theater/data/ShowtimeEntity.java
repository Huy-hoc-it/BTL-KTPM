package com.example.cinema.modules.theater.data;

import com.example.cinema.modules.theater.business.ShowtimeStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "showtimes")
public class ShowtimeEntity {

    @Id
    private UUID id;

    @Column(name = "movie_id", nullable = false)
    private UUID movieId;

    @Column(name = "auditorium_id", nullable = false)
    private UUID auditoriumId;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "ends_at", nullable = false)
    private Instant endsAt;

    @Column(name = "base_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal basePrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ShowtimeStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ShowtimeEntity() {
    }

    public ShowtimeEntity(
            UUID id,
            UUID movieId,
            UUID auditoriumId,
            Instant startsAt,
            Instant endsAt,
            BigDecimal basePrice,
            ShowtimeStatus status,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = id;
        this.movieId = movieId;
        this.auditoriumId = auditoriumId;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.basePrice = basePrice;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getMovieId() {
        return movieId;
    }

    public UUID getAuditoriumId() {
        return auditoriumId;
    }

    public Instant getStartsAt() {
        return startsAt;
    }

    public Instant getEndsAt() {
        return endsAt;
    }

    public BigDecimal getBasePrice() {
        return basePrice;
    }

    public ShowtimeStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
