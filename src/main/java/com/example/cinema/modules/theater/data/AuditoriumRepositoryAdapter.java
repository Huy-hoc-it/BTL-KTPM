package com.example.cinema.modules.theater.data;

import com.example.cinema.modules.theater.business.Auditorium;
import com.example.cinema.modules.theater.business.AuditoriumRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnBean(EntityManagerFactory.class)
class AuditoriumRepositoryAdapter implements AuditoriumRepository {

    private final AuditoriumJpaRepository auditoriumRepository;
    private final ShowtimeJpaRepository showtimeRepository;

    AuditoriumRepositoryAdapter(
            AuditoriumJpaRepository auditoriumRepository,
            ShowtimeJpaRepository showtimeRepository
    ) {
        this.auditoriumRepository = auditoriumRepository;
        this.showtimeRepository = showtimeRepository;
    }

    @Override
    public Auditorium save(Auditorium auditorium) {
        return TheaterDataMapper.toBusiness(auditoriumRepository.save(TheaterDataMapper.toEntity(auditorium)));
    }

    @Override
    public Optional<Auditorium> findById(UUID auditoriumId) {
        return auditoriumRepository.findById(auditoriumId).map(TheaterDataMapper::toBusiness);
    }

    @Override
    public List<Auditorium> findActive() {
        return auditoriumRepository.findAllByDeletedAtIsNullOrderByNameAsc().stream()
                .map(TheaterDataMapper::toBusiness)
                .toList();
    }

    @Override
    public boolean existsByName(String name) {
        return auditoriumRepository.existsByNameIgnoreCase(name);
    }

    @Override
    public boolean hasFutureShowtimes(UUID auditoriumId, Instant from) {
        return showtimeRepository.existsByAuditoriumIdAndStartsAtAfterAndStatusIn(
                auditoriumId,
                from,
                List.of(com.example.cinema.modules.theater.business.ShowtimeStatus.SCHEDULED));
    }
}
