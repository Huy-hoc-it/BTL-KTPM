package com.example.cinema.modules.theater.data;

import com.example.cinema.modules.theater.business.ShowtimeStatus;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface ShowtimeJpaRepository extends JpaRepository<ShowtimeEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from ShowtimeEntity s where s.id = :id")
    Optional<ShowtimeEntity> findByIdForUpdate(UUID id);

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

    List<ShowtimeEntity> findAllByMovieIdAndStartsAtGreaterThanEqualAndStartsAtLessThanOrderByStartsAtAsc(
            UUID movieId,
            Instant from,
            Instant to
    );
}
