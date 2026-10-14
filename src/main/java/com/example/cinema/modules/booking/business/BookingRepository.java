package com.example.cinema.modules.booking.business;

import java.util.Optional;
import java.util.UUID;

public interface BookingRepository {
    Booking save(Booking booking);
    Optional<Booking> findById(UUID bookingId);
    boolean existsByShowtimeIdAndSeatId(UUID showtimeId, UUID seatId);
    void deleteById(UUID bookingId);
}
