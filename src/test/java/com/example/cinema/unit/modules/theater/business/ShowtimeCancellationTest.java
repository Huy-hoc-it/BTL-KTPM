package com.example.cinema.unit.modules.theater.business;

import com.example.cinema.modules.movie.business.MovieModuleApi;
import com.example.cinema.modules.theater.business.*;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShowtimeCancellationTest {
    private static final Instant NOW = Instant.parse("2026-10-23T10:00:00Z");
    @Mock ShowtimeRepository showtimes;
    @Mock ShowtimeCancellationGuard guard;
    private ShowtimeService service;

    @BeforeEach
    void setUp() {
        service = new ShowtimeService(showtimes, mock(AuditoriumRepository.class), mock(SeatRepository.class),
                mock(MovieModuleApi.class), Clock.fixed(NOW, ZoneOffset.UTC), List.of(guard));
    }

    @Test
    void cancelsWithoutDeletingAndChecksBookingsUnderTheShowtimeLock() {
        Showtime original = showtime(ShowtimeStatus.SCHEDULED, NOW.plusSeconds(3600));
        when(showtimes.findByIdForUpdate(original.id())).thenReturn(Optional.of(original));
        service.cancel(original.id());

        var order = inOrder(showtimes, guard);
        order.verify(showtimes).findByIdForUpdate(original.id());
        order.verify(guard).checkCancellationAllowed(original.id());
        var saved = ArgumentCaptor.forClass(Showtime.class);
        order.verify(showtimes).save(saved.capture());
        assertThat(saved.getValue().status()).isEqualTo(ShowtimeStatus.CANCELLED);
        assertThat(saved.getValue().createdAt()).isEqualTo(original.createdAt());
        assertThat(saved.getValue().updatedAt()).isEqualTo(NOW);
    }

    @Test
    void confirmedBookingPreventsCancellation() {
        Showtime original = showtime(ShowtimeStatus.SCHEDULED, NOW.plusSeconds(3600));
        when(showtimes.findByIdForUpdate(original.id())).thenReturn(Optional.of(original));
        doThrow(new ShowtimeHasBookingsException(original.id())).when(guard).checkCancellationAllowed(original.id());
        assertThatThrownBy(() -> service.cancel(original.id())).isInstanceOf(ShowtimeHasBookingsException.class);
        verify(showtimes, never()).save(any());
    }

    @Test
    void rejectsMissingStartedCancelledAndFinishedShowtimes() {
        UUID missing = UUID.randomUUID();
        assertThatThrownBy(() -> service.cancel(missing)).isInstanceOf(ShowtimeNotFoundException.class);
        for (Showtime original : List.of(showtime(ShowtimeStatus.SCHEDULED, NOW),
                showtime(ShowtimeStatus.CANCELLED, NOW.plusSeconds(3600)),
                showtime(ShowtimeStatus.FINISHED, NOW.minusSeconds(3600)))) {
            when(showtimes.findByIdForUpdate(original.id())).thenReturn(Optional.of(original));
            assertThatThrownBy(() -> service.cancel(original.id())).isInstanceOf(TheaterBusinessException.class);
        }
        verify(showtimes, never()).save(any());
        verifyNoInteractions(guard);
    }

    private Showtime showtime(ShowtimeStatus status, Instant start) {
        return new Showtime(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), start,
                start.plusSeconds(8100), new BigDecimal("90000.00"), status, NOW.minusSeconds(10), NOW.minusSeconds(10));
    }
}
