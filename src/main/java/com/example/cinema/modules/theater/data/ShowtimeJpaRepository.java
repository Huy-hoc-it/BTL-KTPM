package com.example.cinema.modules.theater.data;

import com.example.cinema.modules.theater.business.ShowtimeStatus;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShowtimeJpaRepository extends JpaRepository<ShowtimeEntity, UUID> {

    boolean existsByAuditoriumIdAndStartsAtLessThanAndEndsAtGreaterThanAndStatusNot(
            UUID auditoriumId,
            Instant endsAt,
            Instant startsAt,
            ShowtimeStatus status
    );

    boolean existsByAuditoriumIdAndStartsAtAfterAndStatusIn(
            UUID auditoriumId,
            Instant from,
            Collection<ShowtimeStatus> statuses
    );

    List<ShowtimeEntity> findAllByMovieIdAndStartsAtBetweenOrderByStartsAtAsc(
            UUID movieId,
            Instant from,
            Instant to
    );
}
