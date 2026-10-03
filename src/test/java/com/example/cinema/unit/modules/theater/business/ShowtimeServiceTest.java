package com.example.cinema.unit.modules.theater.business;

import com.example.cinema.modules.movie.business.MovieModuleApi;
import com.example.cinema.modules.theater.business.Auditorium;
import com.example.cinema.modules.theater.business.AuditoriumRepository;
import com.example.cinema.modules.theater.business.AuditoriumStatus;
import com.example.cinema.modules.theater.business.CreateShowtimeCommand;
import com.example.cinema.modules.theater.business.ScheduleOverlapException;
import com.example.cinema.modules.theater.business.SeatRepository;
import com.example.cinema.modules.theater.business.Showtime;
import com.example.cinema.modules.theater.business.ShowtimeRepository;
import com.example.cinema.modules.theater.business.ShowtimeService;
import com.example.cinema.modules.theater.business.ShowtimeStatus;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShowtimeServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-01T10:00:00Z");

    @Mock
    private ShowtimeRepository showtimeRepository;

    @Mock
    private AuditoriumRepository auditoriumRepository;

    @Mock
    private SeatRepository seatRepository;

    @Mock
    private MovieModuleApi movieModuleApi;

    private ShowtimeService service;
    private UUID movieId;
    private UUID auditoriumId;

    @BeforeEach
    void setUp() {
        service = new ShowtimeService(
                showtimeRepository,
                auditoriumRepository,
                seatRepository,
                movieModuleApi,
                Clock.fixed(NOW, ZoneOffset.UTC));
        movieId = UUID.randomUUID();
        auditoriumId = UUID.randomUUID();
        when(movieModuleApi.findMovieForScheduling(movieId))
                .thenReturn(Optional.of(new MovieModuleApi.MovieSnapshot(movieId, 120)));
        when(auditoriumRepository.findById(auditoriumId))
                .thenReturn(Optional.of(new Auditorium(
                        auditoriumId,
                        "Room 01",
                        AuditoriumStatus.ACTIVE,
                        null,
                        NOW,
                        NOW)));
        when(showtimeRepository.existsOverlapping(any(), any(), any())).thenReturn(false);
    }

    @Test
    void calculatesEndTimeWithMovieDurationAndCleanupBuffer() {
        when(showtimeRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        Instant startsAt = NOW.plusSeconds(3600);

        Showtime created = service.create(new CreateShowtimeCommand(
                movieId,
                auditoriumId,
                startsAt,
                new BigDecimal("90000.00")));

        assertThat(created.endsAt()).isEqualTo(startsAt.plusSeconds(135 * 60L));
        assertThat(created.status()).isEqualTo(ShowtimeStatus.SCHEDULED);
    }

    @Test
    void rejectsOverlappingSchedule() {
        when(showtimeRepository.existsOverlapping(any(), any(), any())).thenReturn(true);

        assertThatThrownBy(() -> service.create(new CreateShowtimeCommand(
                movieId,
                auditoriumId,
                NOW.plusSeconds(3600),
                new BigDecimal("90000.00"))))
                .isInstanceOf(ScheduleOverlapException.class);
    }
}
