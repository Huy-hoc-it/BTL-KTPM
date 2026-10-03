package com.example.cinema.modules.theater.business;

import java.util.List;
import java.util.UUID;

public interface SeatRepository {

    List<Seat> saveAll(List<Seat> seats);

    List<Seat> findByAuditoriumId(UUID auditoriumId);
}
