package com.example.cinema.modules.theater.data;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SeatJpaRepository extends JpaRepository<SeatEntity, UUID> {

    List<SeatEntity> findAllByAuditoriumIdOrderByRowLabelAscSeatNumberAsc(UUID auditoriumId);
}
