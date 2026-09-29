package com.example.cinema.modules.theater.data;

import com.example.cinema.modules.theater.business.Seat;
import com.example.cinema.modules.theater.business.SeatRepository;
import java.util.List;
import java.util.UUID;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnBean(EntityManagerFactory.class)
class SeatRepositoryAdapter implements SeatRepository {

    private final SeatJpaRepository seatRepository;

    SeatRepositoryAdapter(SeatJpaRepository seatRepository) {
        this.seatRepository = seatRepository;
    }

    @Override
    public List<Seat> saveAll(List<Seat> seats) {
        return seatRepository.saveAll(seats.stream().map(TheaterDataMapper::toEntity).toList()).stream()
                .map(TheaterDataMapper::toBusiness)
                .toList();
    }

    @Override
    public List<Seat> findByAuditoriumId(UUID auditoriumId) {
        return seatRepository.findAllByAuditoriumIdOrderByRowLabelAscSeatNumberAsc(auditoriumId).stream()
                .map(TheaterDataMapper::toBusiness)
                .toList();
    }
}
