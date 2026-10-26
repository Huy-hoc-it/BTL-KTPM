package com.example.cinema.unit.booking;

import com.example.cinema.modules.booking.business.Booking;
import com.example.cinema.modules.booking.business.BookingService;
import com.example.cinema.modules.theater.business.ShowtimeModuleApi.SeatKind;
import com.example.cinema.modules.theater.business.ShowtimeModuleApi.SeatSnapshot;
import com.example.cinema.modules.theater.business.ShowtimeModuleApi.ShowtimeSnapshot;
import com.example.cinema.modules.theater.business.ShowtimeModuleApi.ShowtimeState;
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
                new BigDecimal("100000.00"),
                ShowtimeState.SCHEDULED,
                List.of()
        ));
        theaterApi.setSeats(List.of(
                new SeatSnapshot(standardSeatId, auditoriumId, "A", 1, SeatKind.STANDARD),
                new SeatSnapshot(vipSeatId, auditoriumId, "A", 2, SeatKind.VIP)
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
                new BigDecimal("100000.00"),
                ShowtimeState.CANCELLED,
                List.of()
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
                new BigDecimal("100000.00"),
                ShowtimeState.SCHEDULED,
                List.of()
        ));

        assertThrows(IllegalArgumentException.class,
                () -> bookingService.createBooking(userId, showtimeId, List.of(standardSeatId)));
    }

    @Test
    void rejectsSeatThatDoesNotBelongToShowtime() {
        theaterApi.setSeats(List.of(
                new SeatSnapshot(UUID.randomUUID(), auditoriumId, "A", 1, SeatKind.STANDARD)
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

    @Test
    void rejectsSeatFromAnotherAuditoriumEvenWhenItAppearsInShowtimeSeatList() {
        theaterApi.setSeats(List.of(new SeatSnapshot(standardSeatId, UUID.randomUUID(), "A", 1, SeatKind.STANDARD)));
        assertThrows(IllegalArgumentException.class,
                () -> bookingService.createBooking(userId, showtimeId, List.of(standardSeatId)));
        assertEquals(0, bookingRepository.getBookingCount());
    }

    @Test
    void bookingGuardRejectsConfirmedBookingsAndAllowsShowtimesWithoutBookings() {
        var guard = new com.example.cinema.modules.booking.business.BookingShowtimeCancellationGuard(bookingRepository);
        assertDoesNotThrow(() -> guard.checkCancellationAllowed(showtimeId));
        Booking booking = bookingService.createBooking(userId, showtimeId, List.of(standardSeatId));
        assertThrows(com.example.cinema.modules.theater.business.ShowtimeHasBookingsException.class,
                () -> guard.checkCancellationAllowed(showtimeId));
        bookingService.deleteBooking(booking);
        assertDoesNotThrow(() -> guard.checkCancellationAllowed(showtimeId));
    }

    @Test
    void seatAvailabilityCombinesTheaterLayoutWithBookedSeats() {
        bookingService.createBooking(userId, showtimeId, List.of(standardSeatId));
        var seats = bookingService.getSeatAvailability(showtimeId);
        assertEquals(2, seats.size());
        assertEquals(standardSeatId, seats.get(0).seat().seatId());
        assertEquals(true, seats.get(0).booked());
        assertEquals(false, seats.get(1).booked());
        assertThrows(com.example.cinema.modules.theater.business.ShowtimeNotFoundException.class,
                () -> bookingService.getSeatAvailability(UUID.randomUUID()));
    }
}
