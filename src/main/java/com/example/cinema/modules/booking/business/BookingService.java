package com.example.cinema.modules.booking.business;

import com.example.cinema.modules.theater.business.SeatSnapshot;
import com.example.cinema.modules.theater.business.SeatType;
import com.example.cinema.modules.theater.business.ShowtimeModuleApi;
import com.example.cinema.modules.theater.business.ShowtimeSnapshot;
import com.example.cinema.modules.theater.business.ShowtimeStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

@Service
public class BookingService {

    private static final BigDecimal VIP_MULTIPLIER = new BigDecimal("1.20");

    private final BookingRepository bookingRepository;
    private final ShowtimeModuleApi showtimeModuleApi;
    private final Clock clock;

    @Autowired
    public BookingService(
            BookingRepository bookingRepository,
            ShowtimeModuleApi showtimeModuleApi
    ) {
        this(bookingRepository, showtimeModuleApi, Clock.systemUTC());
    }

    public BookingService(
            BookingRepository bookingRepository,
            ShowtimeModuleApi showtimeModuleApi,
            Clock clock
    ) {
        this.bookingRepository = bookingRepository;
        this.showtimeModuleApi = showtimeModuleApi;
        this.clock = clock;
    }

    @Transactional
    public Booking createBooking(UUID userId, UUID showtimeId, List<UUID> seatIds) {
        validateBookingRequest(userId, showtimeId, seatIds);

        ShowtimeSnapshot showtime = showtimeModuleApi.getShowtimeSnapshot(showtimeId);
        List<SeatSnapshot> seatSnapshots = showtimeModuleApi.getSeatSnapshots(seatIds);

        validateShowtime(showtime, showtimeId);
        validateSeats(seatSnapshots, seatIds, showtime.getAuditoriumId());
        validateSeatAvailable(seatIds, showtimeId);

        List<bookingSeat> bookingSeats = createBookingSeats(showtime, seatSnapshots);
        BigDecimal totalAmount = getTotalAmount(bookingSeats);

        Booking booking = new Booking(userId, showtimeId, bookingSeats, totalAmount);
        return bookingRepository.save(booking);
    }

    private void validateSeatAvailable(List<UUID> seatIds, UUID showtimeId) {
        for (UUID seatId : seatIds) {
            if (bookingRepository.query(seatId, showtimeId) != null) {
                throw new IllegalArgumentException("Seat has already been booked");
            }
        }
    }

    @Transactional
    public void deleteBooking(Booking booking) {
        bookingRepository.delete(booking);
    }

    private BigDecimal getTotalAmount(List<bookingSeat> seats) {
        BigDecimal total = BigDecimal.ZERO;
        for (bookingSeat seat : seats) {
            total = total.add(seat.getPrice());
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    private List<bookingSeat> createBookingSeats(
            ShowtimeSnapshot showtime,
            List<SeatSnapshot> seatSnapshots
    ) {
        List<bookingSeat> bookingSeats = new ArrayList<>();
        for (SeatSnapshot seatSnapshot : seatSnapshots) {
            BigDecimal price = calculateSeatPrice(showtime.getBasePrice(), seatSnapshot.getType());
            bookingSeats.add(new bookingSeat(seatSnapshot.getSeatId(), price));
        }
        return bookingSeats;
    }

    private void validateBookingRequest(
            UUID userId,
            UUID showtimeId,
            List<UUID> seatIds
    ) {
        if (userId == null) {
            throw new IllegalArgumentException("userId must not be null");
        }
        if (showtimeId == null) {
            throw new IllegalArgumentException("showtimeId must not be null");
        }
        if (seatIds == null || seatIds.isEmpty() || seatIds.size() > 8) {
            throw new IllegalArgumentException("Booking must contain from 1 to 8 seats");
        }
        if (seatIds.stream().anyMatch(java.util.Objects::isNull)) {
            throw new IllegalArgumentException("seatIds must not contain null");
        }
        if (new HashSet<>(seatIds).size() != seatIds.size()) {
            throw new IllegalArgumentException("seatIds must not contain duplicate IDs");
        }
    }

    private void validateShowtime(ShowtimeSnapshot showtime, UUID showtimeId) {
        if (showtime == null || !showtimeId.equals(showtime.getShowtimeId())) {
            throw new IllegalArgumentException("Showtime was not found");
        }
        if (showtime.getStatus() != ShowtimeStatus.SCHEDULED) {
            throw new IllegalArgumentException("Showtime is not available for booking");
        }
        if (showtime.getStartsAt() == null || !showtime.getStartsAt().isAfter(clock.instant())) {
            throw new IllegalArgumentException("Showtime has already started");
        }
        if (showtime.getAuditoriumId() == null) {
            throw new IllegalArgumentException("Showtime auditorium is missing");
        }
        if (showtime.getBasePrice() == null || showtime.getBasePrice().signum() <= 0) {
            throw new IllegalArgumentException("Showtime base price must be positive");
        }
    }

    private void validateSeats(
            List<SeatSnapshot> seatSnapshots,
            List<UUID> requestedSeatIds,
            UUID auditoriumId
    ) {
        if (seatSnapshots == null || seatSnapshots.size() != requestedSeatIds.size()) {
            throw new IllegalArgumentException("Theater did not return every requested seat");
        }

        HashSet<UUID> returnedSeatIds = new HashSet<>();
        for (SeatSnapshot seatSnapshot : seatSnapshots) {
            if (seatSnapshot == null
                    || seatSnapshot.getSeatId() == null
                    || !requestedSeatIds.contains(seatSnapshot.getSeatId())
                    || !returnedSeatIds.add(seatSnapshot.getSeatId())) {
                throw new IllegalArgumentException("Theater returned an invalid seat list");
            }
            if (!auditoriumId.equals(seatSnapshot.getAuditoriumId())) {
                throw new IllegalArgumentException("Seat does not belong to showtime auditorium");
            }
            if (seatSnapshot.getType() == null) {
                throw new IllegalArgumentException("Seat type is missing");
            }
        }
    }

    private BigDecimal calculateSeatPrice(BigDecimal basePrice, SeatType seatType) {
        BigDecimal price = seatType == SeatType.VIP
                ? basePrice.multiply(VIP_MULTIPLIER)
                : basePrice;
        return price.setScale(2, RoundingMode.HALF_UP);
    }
}