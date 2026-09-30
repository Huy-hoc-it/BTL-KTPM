package com.example.cinema.modules.theater.data;

import com.example.cinema.modules.theater.business.Showtime;
import com.example.cinema.modules.theater.business.ShowtimeRepository;
import com.example.cinema.modules.theater.business.ShowtimeStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnBean(EntityManagerFactory.class)
class ShowtimeRepositoryAdapter implements ShowtimeRepository {

    private final ShowtimeJpaRepository showtimeRepository;

    ShowtimeRepositoryAdapter(ShowtimeJpaRepository showtimeRepository) {
        this.showtimeRepository = showtimeRepository;
    }

    @Override
    public Showtime save(Showtime showtime) {
        return TheaterDataMapper.toBusiness(showtimeRepository.save(TheaterDataMapper.toEntity(showtime)));
    }

    @Override
    public Optional<Showtime> findById(UUID showtimeId) {
        return showtimeRepository.findById(showtimeId).map(TheaterDataMapper::toBusiness);
    }

    @Override
    public List<Showtime> findByMovieIdAndStartsAtBetween(UUID movieId, Instant from, Instant to) {
        return showtimeRepository.findAllByMovieIdAndStartsAtBetweenOrderByStartsAtAsc(movieId, from, to).stream()
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
