package com.example.cinema.unit.booking;

import com.example.cinema.modules.booking.business.Booking;
import com.example.cinema.modules.booking.business.BookingRepository;
import com.example.cinema.modules.booking.business.bookingSeat;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

class FakeBookingRepository implements BookingRepository {

    private final Map<UUID, Booking> bookingsById = new HashMap<>();
    private final Map<SeatKey, UUID> activeBookingBySeat = new HashMap<>();

    @Override
    public Booking save(Booking booking) {
        Booking previousBooking = bookingsById.put(booking.getBookingId(), booking);
        if (previousBooking != null) {
            removeActiveSeats(previousBooking);
        }

        for (bookingSeat seat : booking.getSeats()) {
            activeBookingBySeat.put(
                    new SeatKey(booking.getShowtimeId(), seat.getSeatId()),
                    booking.getBookingId()
            );
        }
        return booking;
    }

    @Override
    public Optional<Booking> findById(UUID bookingId) {
        return Optional.ofNullable(bookingsById.get(bookingId));
    }

    @Override
    public Optional<Booking> findByIdAndUserId(UUID bookingId, UUID userId) {
        return findById(bookingId).filter(booking -> booking.getUserId().equals(userId));
    }

    @Override
    public java.util.List<Booking> findAllByUserId(UUID userId) {
        return bookingsById.values().stream()
                .filter(booking -> booking.getUserId().equals(userId))
                .toList();
    }

    @Override
    public boolean existsByShowtimeIdAndSeatId(UUID showtimeId, UUID seatId) {
        return activeBookingBySeat.containsKey(new SeatKey(showtimeId, seatId));
    }

    @Override
    public void deleteById(UUID bookingId) {
        Booking booking = bookingsById.remove(bookingId);
        if (booking != null) {
            removeActiveSeats(booking);
        }
    }

    int getBookingCount() {
        return bookingsById.size();
    }

    private void removeActiveSeats(Booking booking) {
        for (bookingSeat seat : booking.getSeats()) {
            activeBookingBySeat.remove(new SeatKey(booking.getShowtimeId(), seat.getSeatId()));
        }
    }

    private static final class SeatKey {
        private final UUID showtimeId;
        private final UUID seatId;

        private SeatKey(UUID showtimeId, UUID seatId) {
            this.showtimeId = showtimeId;
            this.seatId = seatId;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof SeatKey)) {
                return false;
            }
            SeatKey that = (SeatKey) other;
            return Objects.equals(showtimeId, that.showtimeId)
                    && Objects.equals(seatId, that.seatId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(showtimeId, seatId);
        }
    }
}
