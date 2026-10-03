package com.example.cinema.unit.booking;

import com.example.cinema.modules.theater.business.SeatSnapshot;
import com.example.cinema.modules.theater.business.ShowtimeModuleApi;
import com.example.cinema.modules.theater.business.ShowtimeSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

class FakeShowtimeModuleApi implements ShowtimeModuleApi {

    private ShowtimeSnapshot showtimeSnapshot;
    private final Map<UUID, SeatSnapshot> seatsById = new HashMap<>();

    void setShowtimeSnapshot(ShowtimeSnapshot showtimeSnapshot) {
        this.showtimeSnapshot = showtimeSnapshot;
    }

    void setSeats(List<SeatSnapshot> seats) {
        seatsById.clear();
        for (SeatSnapshot seat : seats) {
            seatsById.put(seat.getSeatId(), seat);
        }
    }

    @Override
    public ShowtimeSnapshot getShowtimeSnapshot(UUID showtimeId) {
        if (showtimeSnapshot == null || !showtimeId.equals(showtimeSnapshot.getShowtimeId())) {
            return null;
        }
        return showtimeSnapshot;
    }

    @Override
    public List<SeatSnapshot> getSeatSnapshots(List<UUID> seatIds) {
        List<SeatSnapshot> result = new ArrayList<>();
        for (UUID seatId : seatIds) {
            SeatSnapshot seat = seatsById.get(seatId);
            if (seat != null) {
                result.add(seat);
            }
        }
        return result;
    }
}