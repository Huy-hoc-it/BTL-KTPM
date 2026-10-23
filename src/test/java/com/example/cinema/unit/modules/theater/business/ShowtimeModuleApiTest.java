package com.example.cinema.unit.modules.theater.business;

import com.example.cinema.modules.theater.business.ShowtimeModuleApi.SeatKind;
import com.example.cinema.modules.theater.business.ShowtimeModuleApi.SeatSnapshot;
import com.example.cinema.modules.theater.business.ShowtimeModuleApi.ShowtimeSnapshot;
import com.example.cinema.modules.theater.business.ShowtimeModuleApi.ShowtimeState;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ShowtimeModuleApiTest {

    @Test
    void showtimeSnapshotDefensivelyCopiesSeats() {
        List<SeatSnapshot> source = new ArrayList<>();
        source.add(seat("A", 1));

        ShowtimeSnapshot snapshot = showtime(source);
        source.add(seat("A", 2));

        assertThat(snapshot.seats()).hasSize(1);
        assertThatThrownBy(() -> snapshot.seats().add(seat("A", 3)))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void showtimeSnapshotRejectsNegativeBasePrice() {
        assertThatThrownBy(() -> new ShowtimeSnapshot(
                UUID.randomUUID(),
                UUID.randomUUID(),
                Instant.parse("2026-10-02T12:30:00Z"),
                new BigDecimal("-1.00"),
                ShowtimeState.SCHEDULED,
                List.of(seat("A", 1))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("basePrice must not be negative");
    }

    @Test
    void serviceReturnsRoomOwnedSeatsAndMissingIdsAsAnEmptyResult() {
        var showtimes = org.mockito.Mockito.mock(com.example.cinema.modules.theater.business.ShowtimeRepository.class);
        var seats = org.mockito.Mockito.mock(com.example.cinema.modules.theater.business.SeatRepository.class);
        var service = new com.example.cinema.modules.theater.business.ShowtimeService(showtimes,
                org.mockito.Mockito.mock(com.example.cinema.modules.theater.business.AuditoriumRepository.class), seats,
                org.mockito.Mockito.mock(com.example.cinema.modules.movie.business.MovieModuleApi.class), java.time.Clock.systemUTC());
        UUID roomId = UUID.randomUUID();
        UUID seatId = UUID.randomUUID();
        var seat = new com.example.cinema.modules.theater.business.Seat(seatId, roomId, "B", 2,
                com.example.cinema.modules.theater.business.SeatType.VIP);
        org.mockito.Mockito.when(seats.findByIds(List.of(seatId))).thenReturn(List.of(seat));
        assertThat(service.getSeatSnapshots(List.of(seatId)))
                .containsExactly(new SeatSnapshot(seatId, roomId, "B", 2, SeatKind.VIP));
        assertThat(service.getShowtimeSnapshot(UUID.randomUUID())).isNull();
        assertThat(service.findShowtime(UUID.randomUUID())).isEmpty();
        assertThat(service.getSeatSnapshots(List.of())).isEmpty();
    }

    @Test
    void snapshotPreservesAllShowtimeStatesAndIncludesThePhysicalSeatLayout() {
        var showtimes = org.mockito.Mockito.mock(com.example.cinema.modules.theater.business.ShowtimeRepository.class);
        var seats = org.mockito.Mockito.mock(com.example.cinema.modules.theater.business.SeatRepository.class);
        var service = new com.example.cinema.modules.theater.business.ShowtimeService(showtimes,
                org.mockito.Mockito.mock(com.example.cinema.modules.theater.business.AuditoriumRepository.class), seats,
                org.mockito.Mockito.mock(com.example.cinema.modules.movie.business.MovieModuleApi.class), java.time.Clock.systemUTC());
        UUID roomId = UUID.randomUUID();
        UUID seatId = UUID.randomUUID();
        Instant start = Instant.parse("2030-10-23T11:00:00Z");
        org.mockito.Mockito.when(seats.findByAuditoriumId(roomId)).thenReturn(List.of(
                new com.example.cinema.modules.theater.business.Seat(seatId, roomId, "A", 1,
                        com.example.cinema.modules.theater.business.SeatType.STANDARD)));
        for (var status : com.example.cinema.modules.theater.business.ShowtimeStatus.values()) {
            UUID id = UUID.randomUUID();
            org.mockito.Mockito.when(showtimes.findById(id)).thenReturn(java.util.Optional.of(
                    new com.example.cinema.modules.theater.business.Showtime(id, UUID.randomUUID(), roomId,
                            start, start.plusSeconds(8100), new BigDecimal("90000.00"), status, start, start)));
            var snapshot = service.getShowtimeSnapshot(id);
            assertThat(snapshot.status().name()).isEqualTo(status.name());
            assertThat(snapshot.seats()).containsExactly(new SeatSnapshot(seatId, roomId, "A", 1, SeatKind.STANDARD));
        }
    }

    private static ShowtimeSnapshot showtime(List<SeatSnapshot> seats) {
        return new ShowtimeSnapshot(
                UUID.randomUUID(),
                UUID.randomUUID(),
                Instant.parse("2026-10-02T12:30:00Z"),
                new BigDecimal("90000.00"),
                ShowtimeState.SCHEDULED,
                seats);
    }

    private static SeatSnapshot seat(String rowLabel, int seatNumber) {
        return new SeatSnapshot(UUID.randomUUID(), UUID.randomUUID(), rowLabel, seatNumber, SeatKind.STANDARD);
    }
}
