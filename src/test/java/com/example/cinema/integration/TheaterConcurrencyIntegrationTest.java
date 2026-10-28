package com.example.cinema.integration;

import com.example.cinema.modules.booking.business.BookingService;
import com.example.cinema.modules.theater.business.*;
import com.example.cinema.modules.theater.data.ShowtimeEntity;
import com.example.cinema.modules.theater.data.ShowtimeJpaRepository;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.*;

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
class TheaterConcurrencyIntegrationTest {
    @Container @ServiceConnection
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");
    @Autowired AuditoriumService auditoriums;
    @Autowired ShowtimeService showtimes;
    @Autowired BookingService bookings;
    @Autowired ShowtimeJpaRepository showtimeRows;
    @Autowired JdbcTemplate jdbc;
    private UUID movie;
    private UUID user;
    private Auditorium room;
    private Instant start;

    @BeforeEach
    void seed() {
        movie = UUID.randomUUID();
        user = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO movies (id,title,description,duration_minutes,age_rating,release_date,status,created_at,updated_at)
                VALUES (?, 'Concurrent Movie', 'Test fixture', 120, 'P', CURRENT_DATE, 'NOW_SHOWING', now(), now())
                """, movie);
        jdbc.update("INSERT INTO users (id,username,password_hash,role,created_at,updated_at) VALUES (?,?,'test','CUSTOMER',now(),now())",
                user, "test-" + user);
        room = auditoriums.create(new CreateAuditoriumCommand("Concurrency " + UUID.randomUUID(),
                List.of(new CreateAuditoriumCommand.SeatDefinition("A", 1, 2, SeatType.STANDARD))));
        start = Instant.now().plus(Duration.ofDays(2)).truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
    }

    @Test
    void overlappingConcurrentSchedulesOnlyAllowOneSuccess() throws Exception {
        assertThat(race(() -> create(start), () -> create(start.plusSeconds(60))))
                .containsExactlyInAnyOrder("OK", "ScheduleOverlapException");
    }

    @Test
    void databaseRejectsOverlapEvenWhenBusinessCheckIsBypassed() {
        Showtime first = create(start);
        assertThatThrownBy(() -> showtimeRows.saveAndFlush(new ShowtimeEntity(UUID.randomUUID(), movie, room.id(),
                start.plusSeconds(60), first.endsAt(), first.basePrice(), ShowtimeStatus.SCHEDULED, Instant.now(), Instant.now())))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ex_showtimes_no_overlap");
        assertThat(create(first.endsAt()).startsAt()).isEqualTo(first.endsAt());
    }

    @Test
    void cancelledScheduleFreesItsIntervalAndRoomCanBeSoftDeletedAfterCancellation() {
        Showtime first = create(start);
        assertThatThrownBy(() -> auditoriums.softDelete(room.id())).isInstanceOf(AuditoriumHasFutureShowtimesException.class);
        showtimes.cancel(first.id());
        assertThat(showtimes.findById(first.id()).status()).isEqualTo(ShowtimeStatus.CANCELLED);
        Showtime replacement = create(start);
        showtimes.cancel(replacement.id());
        auditoriums.softDelete(room.id());
        assertThat(auditoriums.findActive()).extracting(Auditorium::id).doesNotContain(room.id());
        assertThat(auditoriums.findSeats(room.id())).hasSize(2);
        assertThatThrownBy(() -> create(start.plusSeconds(20000))).isInstanceOf(TheaterBusinessException.class);
    }

    @Test
    void bookingPreventsCancellationRegardlessOfPaymentStatus() {
        Showtime showtime = create(start);
        UUID seat = auditoriums.findSeats(room.id()).getFirst().id();
        bookings.createBooking(user, showtime.id(), List.of(seat));
        assertThatThrownBy(() -> showtimes.cancel(showtime.id())).isInstanceOf(ShowtimeHasBookingsException.class);
        assertThat(showtimes.findById(showtime.id()).status()).isEqualTo(ShowtimeStatus.SCHEDULED);
        assertThat(bookings.getSeatAvailability(showtime.id()).getFirst().booked()).isTrue();
    }

    @Test
    void bookingAndCancellationCannotBothSucceed() throws Exception {
        Showtime showtime = create(start);
        UUID seat = auditoriums.findSeats(room.id()).getFirst().id();
        List<String> outcomes = race(() -> bookings.createBooking(user, showtime.id(), List.of(seat)),
                () -> { showtimes.cancel(showtime.id()); return null; });
        assertThat(outcomes).containsOnlyOnce("OK");
        if (showtimes.findById(showtime.id()).status() == ShowtimeStatus.CANCELLED) {
            assertThat(outcomes).containsExactlyInAnyOrder("IllegalArgumentException", "OK");
            assertThat(bookings.getBookingsByUserId(user)).isEmpty();
        } else {
            assertThat(outcomes).containsExactlyInAnyOrder("OK", "ShowtimeHasBookingsException");
            assertThat(bookings.getBookingsByUserId(user)).hasSize(1);
        }
    }

    @Test
    void roomDeletionAndSchedulingCannotBothSucceed() throws Exception {
        List<String> outcomes = race(() -> create(start), () -> { auditoriums.softDelete(room.id()); return null; });
        assertThat(outcomes).containsOnlyOnce("OK");
        if (auditoriums.findActive().stream().anyMatch(item -> item.id().equals(room.id()))) {
            assertThat(outcomes).containsExactlyInAnyOrder("OK", "AuditoriumHasFutureShowtimesException");
        } else {
            assertThat(outcomes).containsExactlyInAnyOrder("TheaterBusinessException", "OK");
        }
    }

    @Test
    void queryExcludesNextMidnightAndSortsStartsAscending() {
        Instant midnight = start.atOffset(java.time.ZoneOffset.UTC).toLocalDate().atStartOfDay().toInstant(java.time.ZoneOffset.UTC);
        Showtime late = create(midnight.plusSeconds(20 * 3600));
        Showtime early = create(midnight);
        create(midnight.plusSeconds(24 * 3600));
        assertThat(showtimes.findByMovieIdAndStartsAtBetween(movie, midnight, midnight.plusSeconds(86400)))
                .extracting(Showtime::id).containsExactly(early.id(), late.id());
    }

    private Showtime create(Instant startsAt) {
        return showtimes.create(new CreateShowtimeCommand(movie, room.id(), startsAt, new BigDecimal("90000.00")));
    }

    private List<String> race(Callable<?> first, Callable<?> second) throws Exception {
        var ready = new CountDownLatch(2);
        var go = new CountDownLatch(1);
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var futures = List.of(first, second).stream().map(action -> executor.submit(() -> {
                ready.countDown();
                if (!go.await(10, TimeUnit.SECONDS)) { throw new AssertionError("Race did not start"); }
                try {
                    action.call();
                    return "OK";
                } catch (RuntimeException failure) {
                    return failure.getClass().getSimpleName();
                }
            })).toList();
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            go.countDown();
            return List.of(futures.get(0).get(20, TimeUnit.SECONDS), futures.get(1).get(20, TimeUnit.SECONDS));
        }
    }
}
