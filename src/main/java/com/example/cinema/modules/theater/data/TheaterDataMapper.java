package com.example.cinema.modules.theater.data;

import com.example.cinema.modules.theater.business.Auditorium;
import com.example.cinema.modules.theater.business.Seat;
import com.example.cinema.modules.theater.business.Showtime;

final class TheaterDataMapper {

    private TheaterDataMapper() {
    }

    static Auditorium toBusiness(AuditoriumEntity entity) {
        return new Auditorium(
                entity.getId(),
                entity.getName(),
                entity.getStatus(),
                entity.getDeletedAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    static AuditoriumEntity toEntity(Auditorium auditorium) {
        return new AuditoriumEntity(
                auditorium.id(),
                auditorium.name(),
                auditorium.status(),
                auditorium.deletedAt(),
                auditorium.createdAt(),
                auditorium.updatedAt());
    }

    static Seat toBusiness(SeatEntity entity) {
        return new Seat(
                entity.getId(),
                entity.getAuditoriumId(),
                entity.getRowLabel(),
                entity.getSeatNumber(),
                entity.getType());
    }

    static SeatEntity toEntity(Seat seat) {
        return new SeatEntity(
                seat.id(),
                seat.auditoriumId(),
                seat.rowLabel(),
                seat.seatNumber(),
                seat.type());
    }

    static Showtime toBusiness(ShowtimeEntity entity) {
        return new Showtime(
                entity.getId(),
                entity.getMovieId(),
                entity.getAuditoriumId(),
                entity.getStartsAt(),
                entity.getEndsAt(),
                entity.getBasePrice(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    static ShowtimeEntity toEntity(Showtime showtime) {
        return new ShowtimeEntity(
                showtime.id(),
                showtime.movieId(),
                showtime.auditoriumId(),
                showtime.startsAt(),
                showtime.endsAt(),
                showtime.basePrice(),
                showtime.status(),
                showtime.createdAt(),
                showtime.updatedAt());
    }
}
