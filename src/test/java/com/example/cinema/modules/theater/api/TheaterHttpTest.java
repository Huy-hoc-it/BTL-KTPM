package com.example.cinema.modules.theater.api;

import com.example.cinema.modules.booking.business.BookingRepository;
import com.example.cinema.modules.identity.business.UserRepository;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Exercises real controllers, business services, security and OpenAPI without a database. */
@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration",
        "management.endpoint.health.group.readiness.include=readinessState",
        "cinema.movie.enabled=false"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TheaterHttpTest.TimeConfiguration.class)
class TheaterHttpTest {
    private static final Instant NOW = Instant.parse("2030-10-23T10:00:00Z");
    private static final UUID MOVIE = UUID.randomUUID();
    private static final UUID ROOM = UUID.randomUUID();
    private static final UUID SEAT = UUID.randomUUID();
    @Autowired MockMvc mvc;
    @MockitoBean(name = "userRepositoryAdapter") UserRepository users;
    @MockitoBean(name = "auditoriumRepositoryAdapter") AuditoriumRepository auditoriums;
    @MockitoBean(name = "seatRepositoryAdapter") SeatRepository seats;
    @MockitoBean(name = "showtimeRepositoryAdapter") ShowtimeRepository showtimes;
    @MockitoBean(name = "bookingRepositoryAdapter") BookingRepository bookings;
    @MockitoBean MovieModuleApi movies;

    @TestConfiguration
    static class TimeConfiguration {
        @Bean @org.springframework.context.annotation.Primary
        Clock testClock() { return Clock.fixed(NOW, ZoneOffset.UTC); }
    }

    @BeforeEach
    void setUp() {
        var room = new Auditorium(ROOM, "Room", AuditoriumStatus.ACTIVE, null, NOW, NOW);
        when(auditoriums.findByIdForUpdate(ROOM)).thenReturn(Optional.of(room));
        when(auditoriums.findActive()).thenReturn(List.of(room));
        when(auditoriums.save(any())).thenAnswer(call -> call.getArgument(0));
        when(seats.saveAll(anyList())).thenAnswer(call -> call.getArgument(0));
        when(seats.findByAuditoriumId(any())).thenReturn(List.of(new Seat(SEAT, ROOM, "A", 1, SeatType.STANDARD)));
        when(movies.findMovieForScheduling(MOVIE)).thenReturn(Optional.of(new MovieModuleApi.MovieSnapshot(MOVIE, 120)));
        when(showtimes.save(any())).thenAnswer(call -> call.getArgument(0));
    }

    @Test
    void adminCanCreateListAndSoftDeleteRoom() throws Exception {
        mvc.perform(post("/api/v1/admin/auditoriums").with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content(roomRequest("Room")))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.capacity").value(1));
        mvc.perform(get("/api/v1/admin/auditoriums").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].seats[0].row").value("A"));
        mvc.perform(delete("/api/v1/admin/auditoriums/{id}", ROOM).with(user("admin").roles("ADMIN")))
                .andExpect(status().isNoContent()).andExpect(content().string(""));
        verify(auditoriums).save(argThat(room -> room.id().equals(ROOM)
                && room.status() == AuditoriumStatus.INACTIVE && room.deletedAt().equals(NOW)));
    }

    @Test
    void validationRejectsEmptyNullAndReversedSeatLayoutAndBadPrice() throws Exception {
        for (String layout : List.of("[]", "[null]", "[{\"row\":\"A\",\"from\":2,\"to\":1,\"type\":\"VIP\"}]")) {
            mvc.perform(post("/api/v1/admin/auditoriums").with(user("admin").roles("ADMIN"))
                            .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Room\",\"seatLayout\":" + layout + "}"))
                    .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        }
        for (String price : List.of("0", "-1", "1.234", "10000000000")) {
            mvc.perform(post("/api/v1/admin/showtimes").with(user("admin").roles("ADMIN"))
                            .contentType(MediaType.APPLICATION_JSON).content(showtimeRequest(price)))
                    .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        }
        verify(auditoriums, never()).save(any());
        verify(showtimes, never()).save(any());
    }

    @Test
    void duplicateNamesAndFutureSchedulesReturnConflict() throws Exception {
        when(auditoriums.existsByName("Room")).thenReturn(true);
        mvc.perform(post("/api/v1/admin/auditoriums").with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content(roomRequest("Room")))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error.code").value("AUDITORIUM_ALREADY_EXISTS"));
        when(showtimes.existsFutureByAuditoriumId(ROOM, NOW)).thenReturn(true);
        mvc.perform(delete("/api/v1/admin/auditoriums/{id}", ROOM).with(user("admin").roles("ADMIN")))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error.code").value("AUDITORIUM_HAS_FUTURE_SHOWTIMES"));
    }

    @Test
    void adminCreatesShowtimeWithCleanupBufferAndOverlapReturns409() throws Exception {
        mvc.perform(post("/api/v1/admin/showtimes").with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content(showtimeRequest("90000.00")))
                .andExpect(status().isCreated()).andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.endsAt").value("2030-10-23T13:15:00Z"));
        when(showtimes.existsOverlapping(any(), any(), any())).thenReturn(true);
        mvc.perform(post("/api/v1/admin/showtimes").with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content(showtimeRequest("90000.00")))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error.code").value("SCHEDULE_OVERLAP"));
    }

    @Test
    void cancellationRejectsConfirmedBookingsThenSoftCancelsWhenNoneRemain() throws Exception {
        Showtime showtime = showtime(ShowtimeStatus.SCHEDULED);
        when(showtimes.findByIdForUpdate(showtime.id())).thenReturn(Optional.of(showtime));
        when(bookings.existsConfirmedByShowtimeId(showtime.id())).thenReturn(true);
        mvc.perform(delete("/api/v1/admin/showtimes/{id}", showtime.id()).with(user("admin").roles("ADMIN")))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error.code").value("SHOWTIME_HAS_CONFIRMED_BOOKINGS"));
        verify(showtimes, never()).save(any());
        when(bookings.existsConfirmedByShowtimeId(showtime.id())).thenReturn(false);
        mvc.perform(delete("/api/v1/admin/showtimes/{id}", showtime.id()).with(user("admin").roles("ADMIN")))
                .andExpect(status().isNoContent());
        verify(showtimes).save(argThat(saved -> saved.id().equals(showtime.id()) && saved.status() == ShowtimeStatus.CANCELLED));
    }

    @Test
    void guestScheduleUsesHalfOpenUtcDayAndOnlyListsScheduledShowtimes() throws Exception {
        Showtime scheduled = showtime(ShowtimeStatus.SCHEDULED);
        Instant from = Instant.parse("2030-10-23T00:00:00Z");
        Instant to = Instant.parse("2030-10-24T00:00:00Z");
        when(showtimes.findByMovieIdAndStartsAtBetween(MOVIE, from, to)).thenReturn(List.of(
                scheduled, showtime(ShowtimeStatus.CANCELLED), showtime(ShowtimeStatus.FINISHED)));
        mvc.perform(get("/api/v1/showtimes").param("movieId", MOVIE.toString()).param("date", "2030-10-23"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(scheduled.id().toString()));
        verify(showtimes).findByMovieIdAndStartsAtBetween(MOVIE, from, to);
        Showtime cancelled = showtime(ShowtimeStatus.CANCELLED);
        when(showtimes.findById(cancelled.id())).thenReturn(Optional.of(cancelled));
        mvc.perform(get("/api/v1/showtimes/{id}", cancelled.id()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void adminRoutesRequireAuthenticationAndAdminRole() throws Exception {
        for (String resource : List.of("auditoriums", "showtimes")) {
            String route = "/api/v1/admin/" + resource;
            mvc.perform(post(route)).andExpect(status().isUnauthorized());
            mvc.perform(post(route).with(user("customer").roles("CUSTOMER"))).andExpect(status().isForbidden());
            mvc.perform(delete(route + "/" + UUID.randomUUID())).andExpect(status().isUnauthorized());
            mvc.perform(delete(route + "/" + UUID.randomUUID()).with(user("customer").roles("CUSTOMER")))
                    .andExpect(status().isForbidden());
        }
        mvc.perform(get("/api/v1/admin/auditoriums")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/admin/auditoriums").with(user("customer").roles("CUSTOMER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void malformedQueriesAndBodiesReturn400() throws Exception {
        mvc.perform(get("/api/v1/showtimes").param("movieId", "bad").param("date", "2030-10-23"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code").value("MALFORMED_REQUEST"));
        mvc.perform(get("/api/v1/showtimes").param("movieId", MOVIE.toString()).param("date", "bad"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/showtimes")).andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/admin/auditoriums").with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void openApiIncludesRoomShowtimeAndCancellationRoutes() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/admin/auditoriums'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/admin/auditoriums'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/admin/auditoriums/{id}'].delete").exists())
                .andExpect(jsonPath("$.paths['/api/v1/admin/showtimes'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/admin/showtimes/{id}'].delete").exists())
                .andExpect(jsonPath("$.paths['/api/v1/showtimes'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/showtimes/{id}'].get").exists());
    }

    private Showtime showtime(ShowtimeStatus state) {
        return new Showtime(UUID.randomUUID(), MOVIE, ROOM, NOW.plusSeconds(3600), NOW.plusSeconds(11700),
                new BigDecimal("90000.00"), state, NOW, NOW);
    }

    private String roomRequest(String name) {
        return "{\"name\":\"" + name + "\",\"seatLayout\":[{\"row\":\"A\",\"from\":1,\"to\":1,\"type\":\"STANDARD\"}]}";
    }

    private String showtimeRequest(String price) {
        return "{\"movieId\":\"" + MOVIE + "\",\"auditoriumId\":\"" + ROOM
                + "\",\"startsAt\":\"2030-10-23T11:00:00Z\",\"basePrice\":" + price + "}";
    }
}
