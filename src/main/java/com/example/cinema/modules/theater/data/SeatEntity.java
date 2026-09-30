package com.example.cinema.modules.theater.data;

import com.example.cinema.modules.theater.business.SeatType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;

@Entity
@Table(
        name = "seats",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_seat_position",
                columnNames = {"auditorium_id", "row_label", "seat_number"}
        )
)
public class SeatEntity {

    @Id
    private UUID id;

    @Column(name = "auditorium_id", nullable = false)
    private UUID auditoriumId;

    @Column(name = "row_label", nullable = false, length = 10)
    private String rowLabel;

    @Column(name = "seat_number", nullable = false)
    private int seatNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SeatType type;

    protected SeatEntity() {
    }

    public SeatEntity(UUID id, UUID auditoriumId, String rowLabel, int seatNumber, SeatType type) {
        this.id = id;
        this.auditoriumId = auditoriumId;
        this.rowLabel = rowLabel;
        this.seatNumber = seatNumber;
        this.type = type;
    }

    public UUID getId() {
        return id;
    }

    public UUID getAuditoriumId() {
        return auditoriumId;
    }

    public String getRowLabel() {
        return rowLabel;
    }

    public int getSeatNumber() {
        return seatNumber;
    }

    public SeatType getType() {
        return type;
    }
}
