package com.example.cinema.modules.theater.data;

import com.example.cinema.modules.theater.business.Auditorium;
import com.example.cinema.modules.theater.business.AuditoriumRepository;
import com.example.cinema.modules.theater.business.AuditoriumNameAlreadyExistsException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import org.springframework.dao.DataIntegrityViolationException;

@Repository
@ConditionalOnProperty(name = "cinema.theater.enabled", havingValue = "true", matchIfMissing = true)
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
        try {
            return TheaterDataMapper.toBusiness(auditoriumRepository.saveAndFlush(TheaterDataMapper.toEntity(auditorium)));
        } catch (DataIntegrityViolationException exception) {
            if (TheaterConstraintViolation.isConstraint(exception, "uq_auditorium_name")
                    || TheaterConstraintViolation.isConstraint(exception, "uq_auditorium_name_lower")) {
                throw new AuditoriumNameAlreadyExistsException(auditorium.name());
            }
            throw exception;
        }
    }

    @Override
    public Optional<Auditorium> findById(UUID auditoriumId) {
        return auditoriumRepository.findById(auditoriumId).map(TheaterDataMapper::toBusiness);
    }

    @Override
    public Optional<Auditorium> findByIdForUpdate(UUID auditoriumId) {
        return auditoriumRepository.findByIdForUpdate(auditoriumId).map(TheaterDataMapper::toBusiness);
    }

    @Override
    public List<Auditorium> findActive() {
        return auditoriumRepository.findAllByStatusAndDeletedAtIsNullOrderByNameAsc(
                        com.example.cinema.modules.theater.business.AuditoriumStatus.ACTIVE).stream()
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
