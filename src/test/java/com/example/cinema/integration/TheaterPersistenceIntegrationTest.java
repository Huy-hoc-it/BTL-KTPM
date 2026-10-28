package com.example.cinema.integration;

import com.example.cinema.modules.theater.business.AuditoriumStatus;
import com.example.cinema.modules.theater.business.SeatType;
import com.example.cinema.modules.theater.business.ShowtimeStatus;
import com.example.cinema.modules.theater.data.AuditoriumEntity;
import com.example.cinema.modules.theater.data.AuditoriumJpaRepository;
import com.example.cinema.modules.theater.data.SeatEntity;
import com.example.cinema.modules.theater.data.SeatJpaRepository;
import com.example.cinema.modules.theater.data.ShowtimeEntity;
import com.example.cinema.modules.theater.data.ShowtimeJpaRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
class TheaterPersistenceIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired
    private AuditoriumJpaRepository auditoriumRepository;

    @Autowired
    private SeatJpaRepository seatRepository;

    @Autowired
    private ShowtimeJpaRepository showtimeRepository;

    @Test
    void persistsTheaterEntitiesAgainstFlywaySchema() {
        UUID auditoriumId = UUID.randomUUID();
        Instant now = Instant.parse("2026-10-01T10:00:00Z");
        auditoriumRepository.save(new AuditoriumEntity(
                auditoriumId,
                "Integration Room",
                AuditoriumStatus.ACTIVE,
                null,
                now,
                now));
        UUID seatId = UUID.randomUUID();
        seatRepository.save(new SeatEntity(seatId, auditoriumId, "A", 1, SeatType.STANDARD));
        UUID showtimeId = UUID.randomUUID();
        showtimeRepository.save(new ShowtimeEntity(
                showtimeId,
                UUID.randomUUID(),
                auditoriumId,
                now.plusSeconds(3600),
                now.plusSeconds(3600 + 8100),
                new BigDecimal("90000.00"),
                ShowtimeStatus.SCHEDULED,
                now,
                now));

        assertThat(auditoriumRepository.findById(auditoriumId)).isPresent();
        assertThat(seatRepository.findAllByAuditoriumIdOrderByRowLabelAscSeatNumberAsc(auditoriumId))
                .extracting(SeatEntity::getId)
                .containsExactly(seatId);
        assertThat(showtimeRepository.findById(showtimeId)).isPresent();
    }
}
