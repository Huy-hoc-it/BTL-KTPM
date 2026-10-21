package com.example.cinema.modules.booking.data;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface BookingSeatJpaRepository extends JpaRepository<BookingSeatEntity, UUID> {

    List<BookingSeatEntity> findAllByBookingId(UUID bookingId);

    boolean existsByShowtimeIdAndSeatId(UUID showtimeId, UUID seatId);

    void deleteAllByBookingId(UUID bookingId);
}
