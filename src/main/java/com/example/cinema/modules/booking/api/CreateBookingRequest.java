package com.example.cinema.modules.booking.api;

import java.util.List;
import java.util.UUID;

public class CreateBookingRequest {

    private UUID showtimeId;
    private List<UUID> seatIds;

    public UUID getShowtimeId() { return showtimeId; }
    public List<UUID> getSeatIds() { return seatIds; }

    public void setShowtimeId(UUID showtimeId) { this.showtimeId = showtimeId; }
    public void setSeatIds(List<UUID> seatIds) { this.seatIds = seatIds; }
}
