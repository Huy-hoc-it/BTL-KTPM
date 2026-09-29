package com.example.cinema.modules.theater.business;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuditoriumRepository {

    Auditorium save(Auditorium auditorium);

    Optional<Auditorium> findById(UUID auditoriumId);

    List<Auditorium> findActive();

    boolean existsByName(String name);

    boolean hasFutureShowtimes(UUID auditoriumId, Instant from);
}
