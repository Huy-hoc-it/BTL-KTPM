package com.example.cinema.modules.booking.business;

import com.example.cinema.modules.theater.business.ShowtimeModuleApi;
import com.example.cinema.modules.theater.business.ShowtimeModuleApi.SeatKind;
import com.example.cinema.modules.theater.business.ShowtimeModuleApi.SeatSnapshot;
import com.example.cinema.modules.theater.business.ShowtimeModuleApi.ShowtimeSnapshot;
import com.example.cinema.modules.theater.business.ShowtimeModuleApi.ShowtimeState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
@ConditionalOnProperty(prefix = "cinema.booking.persistence", name = "enabled", havingValue = "true", matchIfMissing = true)
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
        validateSeats(showtime, seatSnapshots, seatIds);
        validateSeatAvailable(seatIds, showtimeId);

        List<bookingSeat> bookingSeats = createBookingSeats(showtime, seatSnapshots);
        BigDecimal totalAmount = getTotalAmount(bookingSeats);

        Booking booking = new Booking(userId, showtimeId, bookingSeats, totalAmount);
        return bookingRepository.save(booking);
    }

    private void validateSeatAvailable(List<UUID> seatIds, UUID showtimeId) {
        for (UUID seatId : seatIds) {
            if (bookingRepository.existsByShowtimeIdAndSeatId(showtimeId, seatId)) {
                throw new IllegalArgumentException("Seat has already been booked");
            }
        }
    }

    @Transactional
    public void deleteBooking(Booking booking) {
        bookingRepository.deleteById(booking.getBookingId());
    }

    public List<Booking> getBookingsByUserId(UUID userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId must not be null");
        }
        return bookingRepository.findAllByUserId(userId);
    }

    public Booking getBookingByUserId(UUID userId, UUID bookingId) {
        if (userId == null || bookingId == null) {
            throw new IllegalArgumentException("userId and bookingId must not be null");
        }
        return bookingRepository.findByIdAndUserId(bookingId, userId)
                .orElseThrow(() -> new BookingNotFoundException(bookingId));
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
            BigDecimal price = calculateSeatPrice(showtime.basePrice(), seatSnapshot.type());
            bookingSeats.add(new bookingSeat(seatSnapshot.seatId(), price));
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
        if (showtime == null || !showtimeId.equals(showtime.showtimeId())) {
            throw new IllegalArgumentException("Showtime was not found");
        }
        if (showtime.status() != ShowtimeState.SCHEDULED) {
            throw new IllegalArgumentException("Showtime is not available for booking");
        }
        if (showtime.startsAt() == null || !showtime.startsAt().isAfter(clock.instant())) {
            throw new IllegalArgumentException("Showtime has already started");
        }
        if (showtime.auditoriumId() == null) {
            throw new IllegalArgumentException("Showtime auditorium is missing");
        }
        if (showtime.basePrice() == null || showtime.basePrice().signum() <= 0) {
            throw new IllegalArgumentException("Showtime base price must be positive");
        }
    }

    private void validateSeats(
            ShowtimeSnapshot showtime,
            List<SeatSnapshot> seatSnapshots,
            List<UUID> requestedSeatIds
    ) {
        if (seatSnapshots == null || seatSnapshots.size() != requestedSeatIds.size()) {
            throw new IllegalArgumentException("Theater did not return every requested seat");
        }

        HashSet<UUID> returnedSeatIds = new HashSet<>();
        HashSet<UUID> showtimeSeatIds = new HashSet<>();
        for (SeatSnapshot seat : showtime.seats()) {
            showtimeSeatIds.add(seat.seatId());
        }
        for (SeatSnapshot seatSnapshot : seatSnapshots) {
            if (seatSnapshot == null
                    || seatSnapshot.seatId() == null
                    || !requestedSeatIds.contains(seatSnapshot.seatId())
                    || !returnedSeatIds.add(seatSnapshot.seatId())) {
                throw new IllegalArgumentException("Theater returned an invalid seat list");
            }
            if (!showtimeSeatIds.contains(seatSnapshot.seatId())) {
                throw new IllegalArgumentException("Seat does not belong to showtime auditorium");
            }
            if (seatSnapshot.type() == null) {
                throw new IllegalArgumentException("Seat type is missing");
            }
        }
    }

    private BigDecimal calculateSeatPrice(BigDecimal basePrice, SeatKind seatType) {
        BigDecimal price = seatType == SeatKind.VIP
                ? basePrice.multiply(VIP_MULTIPLIER)
                : basePrice;
        return price.setScale(2, RoundingMode.HALF_UP);
    }
}
