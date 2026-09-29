package com.example.cinema.modules.theater.business;

import java.time.Clock;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;
import org.springframework.transaction.annotation.Transactional;

public class AuditoriumService {

    private final AuditoriumRepository auditoriumRepository;
    private final SeatRepository seatRepository;
    private final ShowtimeRepository showtimeRepository;
    private final Clock clock;

    public AuditoriumService(
            AuditoriumRepository auditoriumRepository,
            SeatRepository seatRepository,
            ShowtimeRepository showtimeRepository,
            Clock clock
    ) {
        this.auditoriumRepository = auditoriumRepository;
        this.seatRepository = seatRepository;
        this.showtimeRepository = showtimeRepository;
        this.clock = clock;
    }

    @Transactional
    public Auditorium create(CreateAuditoriumCommand command) {
        String name = command.name().trim();
        if (auditoriumRepository.existsByName(name)) {
            throw new AuditoriumNameAlreadyExistsException(name);
        }

        Instant now = clock.instant();
        UUID auditoriumId = UUID.randomUUID();
        Set<String> positions = new HashSet<>();
        command.seatLayout().forEach(definition -> IntStream.rangeClosed(definition.from(), definition.to())
                .mapToObj(number -> definition.rowLabel().trim().toUpperCase(Locale.ROOT) + "#" + number)
                .forEach(position -> {
                    if (!positions.add(position)) {
                        String[] parts = position.split("#", 2);
                        throw new DuplicateSeatPositionException(parts[0], Integer.parseInt(parts[1]));
                    }
                }));
        Auditorium auditorium = new Auditorium(
                auditoriumId,
                name,
                AuditoriumStatus.ACTIVE,
                null,
                now,
                now
        );
        Auditorium saved = auditoriumRepository.save(auditorium);

        List<Seat> seats = command.seatLayout().stream()
                .flatMap(definition -> IntStream.rangeClosed(definition.from(), definition.to())
                        .mapToObj(number -> new Seat(
                                UUID.randomUUID(),
                                auditoriumId,
                                definition.rowLabel().trim().toUpperCase(Locale.ROOT),
                                number,
                                definition.type())))
                .toList();
        seatRepository.saveAll(seats);
        return saved;
    }

    public List<Auditorium> findActive() {
        return auditoriumRepository.findActive();
    }

    @Transactional
    public void softDelete(UUID auditoriumId) {
        Auditorium auditorium = auditoriumRepository.findById(auditoriumId)
                .orElseThrow(() -> new AuditoriumNotFoundException(auditoriumId));
        if (showtimeRepository.existsFutureByAuditoriumId(auditoriumId, clock.instant())) {
            throw new AuditoriumHasFutureShowtimesException(auditoriumId);
        }
        Instant now = clock.instant();
        auditoriumRepository.save(new Auditorium(
                auditorium.id(),
                auditorium.name(),
                AuditoriumStatus.INACTIVE,
                now,
                auditorium.createdAt(),
                now
        ));
    }
}
