package com.example.cinema.unit.booking;

import com.example.cinema.modules.theater.business.ShowtimeModuleApi;
import com.example.cinema.modules.theater.business.ShowtimeModuleApi.SeatSnapshot;
import com.example.cinema.modules.theater.business.ShowtimeModuleApi.ShowtimeSnapshot;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

class FakeShowtimeModuleApi implements ShowtimeModuleApi {

    private ShowtimeSnapshot showtimeSnapshot;
    private List<SeatSnapshot> seats = List.of();

    void setShowtimeSnapshot(ShowtimeSnapshot showtimeSnapshot) {
        this.showtimeSnapshot = showtimeSnapshot;
    }

    void setSeats(List<SeatSnapshot> seats) {
        this.seats = List.copyOf(seats);
    }

    @Override
    public ShowtimeSnapshot getShowtimeSnapshot(UUID showtimeId) {
        if (showtimeSnapshot == null || !showtimeId.equals(showtimeSnapshot.showtimeId())) {
            return null;
        }
        return new ShowtimeSnapshot(
                showtimeSnapshot.showtimeId(), showtimeSnapshot.auditoriumId(),
                showtimeSnapshot.startsAt(), showtimeSnapshot.basePrice(),
                showtimeSnapshot.status(), seats);
    }

    @Override
    public ShowtimeSnapshot lockShowtimeForBooking(UUID showtimeId) {
        return getShowtimeSnapshot(showtimeId);
    }

    @Override
    public List<SeatSnapshot> getSeatSnapshots(List<UUID> seatIds) {
        return seats.stream().filter(seat -> seatIds.contains(seat.seatId())).toList();
    }

    @Override
    public Optional<ShowtimeSnapshot> findShowtime(UUID showtimeId) {
        return Optional.ofNullable(getShowtimeSnapshot(showtimeId));
    }
}
