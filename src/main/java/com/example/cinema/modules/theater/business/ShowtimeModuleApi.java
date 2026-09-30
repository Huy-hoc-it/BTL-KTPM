package com.example.cinema.modules.theater.business;

import java.util.List;
import java.util.UUID;

/**
 * Public business contract used by Booking. Theater owns its implementation.
 */
public interface ShowtimeModuleApi {

    ShowtimeSnapshot getShowtimeSnapshot(UUID showtimeId);

    List<SeatSnapshot> getSeatSnapshots(List<UUID> seatIds);
}