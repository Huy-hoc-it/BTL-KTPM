package com.example.cinema.modules.theater.business;

import com.example.cinema.modules.movie.business.MovieModuleApi;
import com.example.cinema.modules.theater.business.ShowtimeModuleApi.SeatKind;
import com.example.cinema.modules.theater.business.ShowtimeModuleApi.SeatSnapshot;
import com.example.cinema.modules.theater.business.ShowtimeModuleApi.ShowtimeSnapshot;
import com.example.cinema.modules.theater.business.ShowtimeModuleApi.ShowtimeState;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

public class ShowtimeService implements ShowtimeModuleApi {

    private static final Duration CLEANUP_BUFFER = Duration.ofMinutes(15);

    private final ShowtimeRepository showtimeRepository;
    private final AuditoriumRepository auditoriumRepository;
    private final SeatRepository seatRepository;
    private final MovieModuleApi movieModuleApi;
    private final Clock clock;

    public ShowtimeService(
            ShowtimeRepository showtimeRepository,
            AuditoriumRepository auditoriumRepository,
            SeatRepository seatRepository,
            MovieModuleApi movieModuleApi,
            Clock clock
    ) {
        this.showtimeRepository = showtimeRepository;
        this.auditoriumRepository = auditoriumRepository;
        this.seatRepository = seatRepository;
        this.movieModuleApi = movieModuleApi;
        this.clock = clock;
    }

    @Transactional
    public Showtime create(CreateShowtimeCommand command) {
        MovieModuleApi.MovieSnapshot movie = movieModuleApi
                .findMovieForScheduling(command.movieId())
                .orElseThrow(() -> new MovieNotFoundException(command.movieId()));
        Auditorium auditorium = auditoriumRepository.findById(command.auditoriumId())
                .orElseThrow(() -> new AuditoriumNotFoundException(command.auditoriumId()));
        if (!auditorium.isActive()) {
            throw new TheaterBusinessException("Auditorium is not active: " + auditorium.id());
        }

        Instant now = clock.instant();
        if (!command.startsAt().isAfter(now)) {
            throw new TheaterBusinessException("Showtime must start in the future");
        }
        Instant endsAt = command.startsAt()
                .plus(Duration.ofMinutes(movie.durationMinutes()))
                .plus(CLEANUP_BUFFER);
        if (showtimeRepository.existsOverlapping(command.auditoriumId(), command.startsAt(), endsAt)) {
            throw new ScheduleOverlapException(command.auditoriumId());
        }

        Showtime showtime = new Showtime(
                UUID.randomUUID(),
                command.movieId(),
                command.auditoriumId(),
                command.startsAt(),
                endsAt,
                command.basePrice(),
                ShowtimeStatus.SCHEDULED,
                now,
                now
        );
        return showtimeRepository.save(showtime);
    }

    public Showtime findById(UUID showtimeId) {
        return showtimeRepository.findById(showtimeId)
                .orElseThrow(() -> new ShowtimeNotFoundException(showtimeId));
    }

    public List<Showtime> findByMovieIdAndStartsAtBetween(UUID movieId, Instant from, Instant to) {
        return showtimeRepository.findByMovieIdAndStartsAtBetween(movieId, from, to);
    }

    @Transactional
    public void cancel(UUID showtimeId) {
        Showtime showtime = findById(showtimeId);
        Instant now = clock.instant();
        if (showtime.status() != ShowtimeStatus.SCHEDULED || !showtime.startsAt().isAfter(now)) {
            throw new TheaterBusinessException("Showtime cannot be cancelled: " + showtimeId);
        }
        showtimeRepository.save(new Showtime(
                showtime.id(),
                showtime.movieId(),
                showtime.auditoriumId(),
                showtime.startsAt(),
                showtime.endsAt(),
                showtime.basePrice(),
                ShowtimeStatus.CANCELLED,
                showtime.createdAt(),
                now
        ));
    }

    @Override
    public java.util.Optional<ShowtimeSnapshot> findShowtime(UUID showtimeId) {
        return showtimeRepository.findById(showtimeId).map(showtime -> {
            List<SeatSnapshot> seats = seatRepository.findByAuditoriumId(showtime.auditoriumId()).stream()
                    .map(seat -> new SeatSnapshot(
                            seat.id(),
                            seat.rowLabel(),
                            seat.seatNumber(),
                            seat.type() == SeatType.VIP ? SeatKind.VIP : SeatKind.STANDARD))
                    .toList();
            return new ShowtimeSnapshot(
                    showtime.id(),
                    showtime.auditoriumId(),
                    showtime.startsAt(),
                    showtime.basePrice(),
                    switch (showtime.status()) {
                        case SCHEDULED -> ShowtimeState.SCHEDULED;
                        case CANCELLED -> ShowtimeState.CANCELLED;
                        case FINISHED -> ShowtimeState.FINISHED;
                    },
                    seats);
        });
    }
}
