package com.example.cinema.modules.booking.data;

import com.example.cinema.modules.booking.business.Booking;
import com.example.cinema.modules.booking.business.BookingRepository;
import com.example.cinema.modules.booking.business.bookingSeat;
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
                booking.getTotalAmount(), booking.getCreatedAt(), booking.getPaymentStatus()));

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
        return bookingJpaRepository.findById(bookingId).map(this::toBooking);
    }

    @Override
    public Optional<Booking> findByIdAndUserId(UUID bookingId, UUID userId) {
        return bookingJpaRepository.findByIdAndUserId(bookingId, userId).map(this::toBooking);
    }

    @Override
    public List<Booking> findAllByUserId(UUID userId) {
        return bookingJpaRepository.findAllByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toBooking)
                .toList();
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

    private Booking toBooking(BookingEntity entity) {
        List<bookingSeat> seats = bookingSeatJpaRepository.findAllByBookingId(entity.getId()).stream()
                .map(seat -> new bookingSeat(seat.getSeatId(), seat.getPrice()))
                .toList();
        return new Booking(entity.getId(), entity.getUserId(), entity.getShowtimeId(), seats,
                entity.getTotalAmount(), entity.getCreatedAt(), entity.getPaymentStatus());
    }
}
