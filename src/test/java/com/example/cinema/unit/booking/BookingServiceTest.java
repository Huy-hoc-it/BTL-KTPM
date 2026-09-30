package com.example.cinema.unit.booking;

import com.example.cinema.modules.booking.business.Booking;
import com.example.cinema.modules.booking.business.BookingService;
import com.example.cinema.modules.theater.business.SeatSnapshot;
import com.example.cinema.modules.theater.business.SeatType;
import com.example.cinema.modules.theater.business.ShowtimeSnapshot;
import com.example.cinema.modules.theater.business.ShowtimeStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BookingServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-30T10:00:00Z");

    private final UUID userId = UUID.randomUUID();
    private final UUID showtimeId = UUID.randomUUID();
    private final UUID auditoriumId = UUID.randomUUID();
    private final UUID standardSeatId = UUID.randomUUID();
    private final UUID vipSeatId = UUID.randomUUID();

    private FakeBookingRepository bookingRepository;
    private FakeShowtimeModuleApi theaterApi;
    private BookingService bookingService;

    @BeforeEach
    void setUp() {
        bookingRepository = new FakeBookingRepository();
        theaterApi = new FakeShowtimeModuleApi();
        theaterApi.setShowtimeSnapshot(new ShowtimeSnapshot(
                showtimeId,
                auditoriumId,
                NOW.plusSeconds(3_600),
                ShowtimeStatus.SCHEDULED,
                new BigDecimal("100000.00")
        ));
        theaterApi.setSeats(List.of(
                new SeatSnapshot(standardSeatId, auditoriumId, SeatType.STANDARD),
                new SeatSnapshot(vipSeatId, auditoriumId, SeatType.VIP)
        ));
        bookingService = new BookingService(
                bookingRepository,
                theaterApi,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void createsBookingWithCorrectSeatPricesAndTotal() {
        Booking booking = bookingService.createBooking(
                userId,
                showtimeId,
                List.of(standardSeatId, vipSeatId)
        );

        assertNotNull(booking.getBookingId());
        assertEquals(userId, booking.getUserId());
        assertEquals(showtimeId, booking.getShowtimeId());
        assertEquals(2, booking.getSeats().size());
        assertEquals(new BigDecimal("100000.00"), booking.getSeats().get(0).getPrice());
        assertEquals(new BigDecimal("120000.00"), booking.getSeats().get(1).getPrice());
        assertEquals(new BigDecimal("220000.00"), booking.getTotalAmount());
        assertEquals(1, bookingRepository.getBookingCount());
    }

    @Test
    void rejectsEmptySeatList() {
        assertThrows(IllegalArgumentException.class,
                () -> bookingService.createBooking(userId, showtimeId, List.of()));
    }

    @Test
    void rejectsMoreThanEightSeats() {
        List<UUID> nineSeatIds = List.of(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()
        );

        assertThrows(IllegalArgumentException.class,
                () -> bookingService.createBooking(userId, showtimeId, nineSeatIds));
    }

    @Test
    void rejectsDuplicateSeatIds() {
        assertThrows(IllegalArgumentException.class,
                () -> bookingService.createBooking(
                        userId,
                        showtimeId,
                        List.of(standardSeatId, standardSeatId)
                ));
    }

    @Test
    void rejectsCancelledShowtime() {
        theaterApi.setShowtimeSnapshot(new ShowtimeSnapshot(
                showtimeId,
                auditoriumId,
                NOW.plusSeconds(3_600),
                ShowtimeStatus.CANCELLED,
                new BigDecimal("100000.00")
        ));

        assertThrows(IllegalArgumentException.class,
                () -> bookingService.createBooking(userId, showtimeId, List.of(standardSeatId)));
    }

    @Test
    void rejectsShowtimeThatHasStarted() {
        theaterApi.setShowtimeSnapshot(new ShowtimeSnapshot(
                showtimeId,
                auditoriumId,
                NOW,
                ShowtimeStatus.SCHEDULED,
                new BigDecimal("100000.00")
        ));

        assertThrows(IllegalArgumentException.class,
                () -> bookingService.createBooking(userId, showtimeId, List.of(standardSeatId)));
    }

    @Test
    void rejectsSeatFromAnotherAuditorium() {
        theaterApi.setSeats(List.of(
                new SeatSnapshot(standardSeatId, UUID.randomUUID(), SeatType.STANDARD)
        ));

        assertThrows(IllegalArgumentException.class,
                () -> bookingService.createBooking(userId, showtimeId, List.of(standardSeatId)));
    }

    @Test
    void rejectsSeatThatTheaterDoesNotReturn() {
        UUID unknownSeatId = UUID.randomUUID();

        assertThrows(IllegalArgumentException.class,
                () -> bookingService.createBooking(userId, showtimeId, List.of(unknownSeatId)));
    }

    @Test
    void rejectsSeatAlreadyBookedForSameShowtime() {
        bookingService.createBooking(userId, showtimeId, List.of(standardSeatId));

        assertThrows(IllegalArgumentException.class,
                () -> bookingService.createBooking(UUID.randomUUID(), showtimeId, List.of(standardSeatId)));
    }

    @Test
    void canBookSeatAgainAfterDeletingPreviousBooking() {
        Booking firstBooking = bookingService.createBooking(
                userId,
                showtimeId,
                List.of(standardSeatId)
        );

        bookingService.deleteBooking(firstBooking);

        Booking newBooking = assertDoesNotThrow(() -> bookingService.createBooking(
                UUID.randomUUID(),
                showtimeId,
                List.of(standardSeatId)
        ));

        assertNotNull(newBooking.getBookingId());
        assertEquals(1, bookingRepository.getBookingCount());
    }
}