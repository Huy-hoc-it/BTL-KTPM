package com.example.cinema.modules.booking.business;

import java.util.UUID;

public interface BookingRepository {
    Booking save(Booking booking);
    void delete(Booking booking);
    void query(Booking booking);
    UUID query(UUID seatID, UUID showTimeID);
}
