package com.example.cinema.modules.theater.data;

import com.example.cinema.modules.movie.business.MovieModuleApi;
import com.example.cinema.modules.theater.business.Auditorium;
import com.example.cinema.modules.theater.business.AuditoriumService;
import com.example.cinema.modules.theater.business.CreateAuditoriumCommand;
import com.example.cinema.modules.theater.business.CreateShowtimeCommand;
import com.example.cinema.modules.theater.business.SeatType;
import com.example.cinema.modules.theater.business.Showtime;
import com.example.cinema.modules.theater.business.ShowtimeModuleApi.ShowtimeState;
import com.example.cinema.modules.theater.business.ShowtimeService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
class TheaterBusinessFlowIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-10-12T10:00:00Z");

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired
    private AuditoriumJpaRepository auditoriumJpaRepository;

    @Autowired
    private SeatJpaRepository seatJpaRepository;

    @Autowired
    private ShowtimeJpaRepository showtimeJpaRepository;

    @Test
    void createsAndQueriesAuditoriumShowtimeAndBookingSnapshot() {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        AuditoriumRepositoryAdapter auditoriumRepository = new AuditoriumRepositoryAdapter(
                auditoriumJpaRepository,
                showtimeJpaRepository);
        SeatRepositoryAdapter seatRepository = new SeatRepositoryAdapter(seatJpaRepository);
        ShowtimeRepositoryAdapter showtimeRepository = new ShowtimeRepositoryAdapter(showtimeJpaRepository);
        AuditoriumService auditoriumService = new AuditoriumService(
                auditoriumRepository,
                seatRepository,
                showtimeRepository,
                clock);
        UUID movieId = UUID.randomUUID();
        MovieModuleApi movieModuleApi = mock(MovieModuleApi.class);
        when(movieModuleApi.findMovieForScheduling(movieId))
                .thenReturn(Optional.of(new MovieModuleApi.MovieSnapshot(movieId, 120)));

        ShowtimeService showtimeService = new ShowtimeService(
                showtimeRepository,
                auditoriumRepository,
                seatRepository,
                movieModuleApi,
                clock);

        Auditorium auditorium = auditoriumService.create(new CreateAuditoriumCommand(
                "Flow Room " + UUID.randomUUID(),
                List.of(
                        new CreateAuditoriumCommand.SeatDefinition("A", 1, 2, SeatType.STANDARD),
                        new CreateAuditoriumCommand.SeatDefinition("B", 1, 1, SeatType.VIP))));
        Instant startsAt = NOW.plusSeconds(2 * 60 * 60);
        Showtime created = showtimeService.create(new CreateShowtimeCommand(
                movieId,
                auditorium.id(),
                startsAt,
                new BigDecimal("90000.00")));

        assertThat(showtimeService.findById(created.id())).isEqualTo(created);
        assertThat(showtimeService.findByMovieIdAndStartsAtBetween(
                movieId,
                startsAt.minusSeconds(1),
                startsAt.plusSeconds(1)))
                .containsExactly(created);

        var snapshot = showtimeService.findShowtime(created.id()).orElseThrow();
        assertThat(snapshot.showtimeId()).isEqualTo(created.id());
        assertThat(snapshot.auditoriumId()).isEqualTo(auditorium.id());
        assertThat(snapshot.startsAt()).isEqualTo(startsAt);
        assertThat(snapshot.basePrice()).isEqualByComparingTo("90000.00");
        assertThat(snapshot.status()).isEqualTo(ShowtimeState.SCHEDULED);
        assertThat(snapshot.seats()).hasSize(3);
        assertThat(snapshot.seats()).extracting("type").containsExactly(
                com.example.cinema.modules.theater.business.ShowtimeModuleApi.SeatKind.STANDARD,
                com.example.cinema.modules.theater.business.ShowtimeModuleApi.SeatKind.STANDARD,
                com.example.cinema.modules.theater.business.ShowtimeModuleApi.SeatKind.VIP);
    }
}
