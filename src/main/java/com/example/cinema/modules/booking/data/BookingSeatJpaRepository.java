package com.example.cinema.modules.booking.data;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface BookingSeatJpaRepository extends JpaRepository<BookingSeatEntity, UUID> {

    @Query("select s.seatId from BookingSeatEntity s where s.showtimeId = :showtimeId")
    List<UUID> findBookedSeatIds(UUID showtimeId);

    List<BookingSeatEntity> findAllByBookingId(UUID bookingId);

    boolean existsByShowtimeIdAndSeatId(UUID showtimeId, UUID seatId);

    void deleteAllByBookingId(UUID bookingId);
}
