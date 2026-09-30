package com.example.cinema.modules.theater.business;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ShowtimeRepository {

    Showtime save(Showtime showtime);

    Optional<Showtime> findById(UUID showtimeId);

    List<Showtime> findByMovieIdAndStartsAtBetween(UUID movieId, Instant from, Instant to);

    boolean existsOverlapping(UUID auditoriumId, Instant startsAt, Instant endsAt);

    boolean existsFutureByAuditoriumId(UUID auditoriumId, Instant from);
}
