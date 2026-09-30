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
        return new SeatSnapshot(UUID.randomUUID(), rowLabel, seatNumber, SeatKind.STANDARD);
    }
}
