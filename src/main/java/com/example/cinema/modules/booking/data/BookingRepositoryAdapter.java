package com.example.cinema.modules.booking.data;

import com.example.cinema.modules.booking.business.Booking;
import com.example.cinema.modules.booking.business.BookingRepository;
import com.example.cinema.modules.booking.business.bookingSeat;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
class BookingRepositoryAdapter implements BookingRepository {

    private final BookingJpaRepository bookingJpaRepository;
    private final BookingSeatJpaRepository bookingSeatJpaRepository;

    BookingRepositoryAdapter(
            BookingJpaRepository bookingJpaRepository,
            BookingSeatJpaRepository bookingSeatJpaRepository
    ) {
        this.bookingJpaRepository = bookingJpaRepository;
        this.bookingSeatJpaRepository = bookingSeatJpaRepository;
    }

    @Override
    public Booking save(Booking booking) {
        bookingJpaRepository.save(new BookingEntity(
                booking.getBookingId(), booking.getUserId(), booking.getShowtimeId(),
                booking.getTotalAmount(), Instant.now()));

        List<BookingSeatEntity> seatEntities = booking.getSeats().stream()
                .map(seat -> new BookingSeatEntity(
                        UUID.randomUUID(), booking.getBookingId(), booking.getShowtimeId(),
                        seat.getSeatId(), seat.getPrice()))
                .toList();
        bookingSeatJpaRepository.saveAll(seatEntities);
        return booking;
    }

    @Override
    public Optional<Booking> findById(UUID bookingId) {
        return bookingJpaRepository.findById(bookingId).map(entity -> {
            List<bookingSeat> seats = bookingSeatJpaRepository.findAllByBookingId(bookingId).stream()
                    .map(seat -> new bookingSeat(seat.getSeatId(), seat.getPrice()))
                    .toList();
            return new Booking(
                    entity.getId(), entity.getUserId(), entity.getShowtimeId(),
                    seats, entity.getTotalAmount());
        });
    }

    @Override
    public boolean existsByShowtimeIdAndSeatId(UUID showtimeId, UUID seatId) {
        return bookingSeatJpaRepository.existsByShowtimeIdAndSeatId(showtimeId, seatId);
    }

    @Override
    @Transactional
    public void deleteById(UUID bookingId) {
        bookingSeatJpaRepository.deleteAllByBookingId(bookingId);
        bookingJpaRepository.deleteById(bookingId);
    }
}
