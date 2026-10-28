package com.example.cinema.modules.theater.data;

import com.example.cinema.modules.theater.business.Showtime;
import com.example.cinema.modules.theater.business.ShowtimeRepository;
import com.example.cinema.modules.theater.business.ShowtimeStatus;
import com.example.cinema.modules.theater.business.ScheduleOverlapException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import org.springframework.dao.DataIntegrityViolationException;

@Repository
@ConditionalOnProperty(name = "cinema.theater.enabled", havingValue = "true", matchIfMissing = true)
class ShowtimeRepositoryAdapter implements ShowtimeRepository {

    private final ShowtimeJpaRepository showtimeRepository;

    ShowtimeRepositoryAdapter(ShowtimeJpaRepository showtimeRepository) {
        this.showtimeRepository = showtimeRepository;
    }

    @Override
    public Showtime save(Showtime showtime) {
        try {
            return TheaterDataMapper.toBusiness(showtimeRepository.saveAndFlush(TheaterDataMapper.toEntity(showtime)));
        } catch (DataIntegrityViolationException exception) {
            if (TheaterConstraintViolation.isConstraint(exception, "ex_showtimes_no_overlap")) {
                throw new ScheduleOverlapException(showtime.auditoriumId());
            }
            throw exception;
        }
    }

    @Override
    public Optional<Showtime> findById(UUID showtimeId) {
        return showtimeRepository.findById(showtimeId).map(TheaterDataMapper::toBusiness);
    }

    @Override
    public Optional<Showtime> findByIdForUpdate(UUID showtimeId) {
        return showtimeRepository.findByIdForUpdate(showtimeId).map(TheaterDataMapper::toBusiness);
    }

    @Override
    public List<Showtime> findByMovieIdAndStartsAtBetween(UUID movieId, Instant from, Instant to) {
        return showtimeRepository.findAllByMovieIdAndStartsAtGreaterThanEqualAndStartsAtLessThanOrderByStartsAtAsc(movieId, from, to).stream()
                .map(TheaterDataMapper::toBusiness)
                .toList();
    }

    @Override
    public boolean existsOverlapping(UUID auditoriumId, Instant startsAt, Instant endsAt) {
        return showtimeRepository.existsByAuditoriumIdAndStartsAtLessThanAndEndsAtGreaterThanAndStatusNot(
                auditoriumId, endsAt, startsAt, ShowtimeStatus.CANCELLED);
    }

    @Override
    public boolean existsFutureByAuditoriumId(UUID auditoriumId, Instant from) {
        return showtimeRepository.existsByAuditoriumIdAndStartsAtAfterAndStatusIn(
                auditoriumId, from, List.of(ShowtimeStatus.SCHEDULED));
    }
}
