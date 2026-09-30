package com.example.cinema.unit.modules.theater.business;

import com.example.cinema.modules.theater.business.Auditorium;
import com.example.cinema.modules.theater.business.AuditoriumRepository;
import com.example.cinema.modules.theater.business.AuditoriumService;
import com.example.cinema.modules.theater.business.CreateAuditoriumCommand;
import com.example.cinema.modules.theater.business.DuplicateSeatPositionException;
import com.example.cinema.modules.theater.business.Seat;
import com.example.cinema.modules.theater.business.SeatRepository;
import com.example.cinema.modules.theater.business.SeatType;
import com.example.cinema.modules.theater.business.ShowtimeRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditoriumServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-01T10:00:00Z");

    @Mock
    private AuditoriumRepository auditoriumRepository;

    @Mock
    private SeatRepository seatRepository;

    @Mock
    private ShowtimeRepository showtimeRepository;

    private AuditoriumService service;

    @BeforeEach
    void setUp() {
        service = new AuditoriumService(
                auditoriumRepository,
                seatRepository,
                showtimeRepository,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void createsAuditoriumAndExpandsSeatLayout() {
        when(auditoriumRepository.existsByName(any())).thenReturn(false);
        when(auditoriumRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(seatRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        Auditorium auditorium = service.create(new CreateAuditoriumCommand(
                "  Room 01  ",
                List.of(
                        new CreateAuditoriumCommand.SeatDefinition("a", 1, 2, SeatType.STANDARD),
                        new CreateAuditoriumCommand.SeatDefinition("B", 1, 1, SeatType.VIP))));

        assertThat(auditorium.name()).isEqualTo("Room 01");
        assertThat(auditorium.status()).isEqualTo(com.example.cinema.modules.theater.business.AuditoriumStatus.ACTIVE);

        ArgumentCaptor<List<Seat>> seats = ArgumentCaptor.forClass(List.class);
        verify(seatRepository).saveAll(seats.capture());
        assertThat(seats.getValue()).extracting(Seat::rowLabel).containsExactly("A", "A", "B");
        assertThat(seats.getValue()).extracting(Seat::seatNumber).containsExactly(1, 2, 1);
        assertThat(seats.getValue()).extracting(Seat::type)
                .containsExactly(SeatType.STANDARD, SeatType.STANDARD, SeatType.VIP);
    }

    @Test
    void softDeleteMarksAuditoriumInactiveWhenNoFutureShowtimeExists() {
        Auditorium auditorium = new Auditorium(
                java.util.UUID.randomUUID(),
                "Room 01",
                com.example.cinema.modules.theater.business.AuditoriumStatus.ACTIVE,
                null,
                NOW,
                NOW);
        when(auditoriumRepository.findById(auditorium.id())).thenReturn(Optional.of(auditorium));
        when(showtimeRepository.existsFutureByAuditoriumId(auditorium.id(), NOW)).thenReturn(false);
        when(auditoriumRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.softDelete(auditorium.id());

        ArgumentCaptor<Auditorium> saved = ArgumentCaptor.forClass(Auditorium.class);
        verify(auditoriumRepository).save(saved.capture());
        assertThat(saved.getValue().status()).isEqualTo(com.example.cinema.modules.theater.business.AuditoriumStatus.INACTIVE);
        assertThat(saved.getValue().deletedAt()).isEqualTo(NOW);
    }

    @Test
    void rejectsOverlappingSeatDefinitions() {
        when(auditoriumRepository.existsByName(any())).thenReturn(false);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.create(new CreateAuditoriumCommand(
                "Room 01",
                List.of(
                        new CreateAuditoriumCommand.SeatDefinition("A", 1, 3, SeatType.STANDARD),
                        new CreateAuditoriumCommand.SeatDefinition("a", 3, 4, SeatType.VIP)))))
                .isInstanceOf(DuplicateSeatPositionException.class);
    }
}
