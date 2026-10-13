package com.example.cinema.modules.theater.api;

import com.example.cinema.modules.movie.business.MovieAgeRating;
import com.example.cinema.modules.movie.business.MovieModuleApi;
import com.example.cinema.modules.movie.business.MovieStatus;
import com.example.cinema.modules.movie.data.MovieEntity;
import com.example.cinema.modules.movie.data.MovieJpaRepository;
import com.example.cinema.modules.theater.business.ShowtimeModuleApi;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TheaterApiIntegrationTest {

    private static final UUID MOVIE_ID = UUID.fromString("c172b99d-790d-4244-bc53-c84130c19a29");
    private static final int MOVIE_DURATION_MINUTES = 120;

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ShowtimeModuleApi showtimeModuleApi;

    @Autowired
    private MovieModuleApi movieModuleApi;

    @Autowired
    private MovieJpaRepository movieRepository;

    @BeforeEach
    void persistMovieUsedByTheaterApi() {
        Instant now = Instant.now();
        movieRepository.save(new MovieEntity(
                MOVIE_ID,
                "Integration Movie",
                "A real movie row used by the Theater API integration test.",
                MOVIE_DURATION_MINUTES,
                MovieAgeRating.P,
                LocalDate.now(ZoneOffset.UTC),
                null,
                MovieStatus.NOW_SHOWING,
                null,
                now,
                now));
        assertThat(movieModuleApi.findMovieForScheduling(MOVIE_ID))
                .contains(new MovieModuleApi.MovieSnapshot(MOVIE_ID, MOVIE_DURATION_MINUTES));
        org.assertj.core.api.Assertions.assertThat(showtimeModuleApi).isNotNull();
    }

    @Test
    void adminCreatesAuditoriumAndShowtimeAndGuestReadsScheduleAndDetails() throws Exception {
        MvcResult auditoriumResult = mockMvc.perform(post("/api/v1/admin/auditoriums")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "API Room %s",
                                  "seatLayout": [
                                    {"row": "a", "from": 1, "to": 2, "type": "STANDARD"},
                                    {"row": "B", "from": 1, "to": 1, "type": "VIP"}
                                  ]
                                }
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.capacity").value(3))
                .andExpect(jsonPath("$.seats.length()").value(3))
                .andExpect(jsonPath("$.seats[0].row").value("A"))
                .andExpect(jsonPath("$.seats[2].type").value("VIP"))
                .andReturn();
        UUID auditoriumId = UUID.fromString(objectMapper.readTree(auditoriumResult.getResponse().getContentAsString())
                .path("id").asText());
        mockMvc.perform(get("/api/v1/admin/auditoriums")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(auditoriumId.toString()));

        Instant startsAt = Instant.now().plus(Duration.ofHours(2)).truncatedTo(ChronoUnit.SECONDS);
        MvcResult showtimeResult = mockMvc.perform(post("/api/v1/admin/showtimes")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "movieId": "%s",
                                  "auditoriumId": "%s",
                                  "startsAt": "%s",
                                  "basePrice": 90000.00
                                }
                                """.formatted(MOVIE_ID, auditoriumId, startsAt)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.movieId").value(MOVIE_ID.toString()))
                .andExpect(jsonPath("$.auditoriumId").value(auditoriumId.toString()))
                .andExpect(jsonPath("$.endsAt").value(startsAt.plus(Duration.ofMinutes(135)).toString()))
                .andReturn();
        JsonNode showtime = objectMapper.readTree(showtimeResult.getResponse().getContentAsString());
        UUID showtimeId = UUID.fromString(showtime.path("id").asText());
        LocalDate showDate = startsAt.atZone(ZoneOffset.UTC).toLocalDate();

        mockMvc.perform(get("/api/v1/showtimes")
                        .queryParam("movieId", MOVIE_ID.toString())
                        .queryParam("date", showDate.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(showtimeId.toString()));

        mockMvc.perform(get("/api/v1/showtimes/{id}", showtimeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(showtimeId.toString()))
                .andExpect(jsonPath("$.endsAt").value(startsAt.plus(Duration.ofMinutes(135)).toString()));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void rejectsInvalidSeatLayoutAndUnknownMovieOrAuditorium() throws Exception {
        mockMvc.perform(post("/api/v1/admin/auditoriums")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Bad Room","seatLayout":[
                                  {"row":"A","from":2,"to":1,"type":"STANDARD"}
                                ]}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));

        UUID unknownMovie = UUID.randomUUID();
        UUID unknownAuditorium = UUID.randomUUID();
        mockMvc.perform(post("/api/v1/admin/showtimes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "movieId":"%s",
                                  "auditoriumId":"%s",
                                  "startsAt":"%s",
                                  "basePrice":90000.00
                                }
                                """.formatted(unknownMovie, unknownAuditorium,
                                Instant.now().plus(Duration.ofHours(3)).truncatedTo(ChronoUnit.SECONDS))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("MOVIE_NOT_FOUND"));

        mockMvc.perform(post("/api/v1/admin/showtimes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "movieId":"%s",
                                  "auditoriumId":"%s",
                                  "startsAt":"%s",
                                  "basePrice":90000.00
                                }
                                """.formatted(MOVIE_ID, unknownAuditorium,
                                Instant.now().plus(Duration.ofHours(3)).truncatedTo(ChronoUnit.SECONDS))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("AUDITORIUM_NOT_FOUND"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void refusesToScheduleSoftDeletedMovie() throws Exception {
        UUID deletedMovieId = UUID.randomUUID();
        Instant now = Instant.now();
        movieRepository.save(new MovieEntity(
                deletedMovieId,
                "Deleted Movie",
                "This movie has been soft deleted.",
                MOVIE_DURATION_MINUTES,
                MovieAgeRating.P,
                LocalDate.now(ZoneOffset.UTC),
                null,
                MovieStatus.NOW_SHOWING,
                now,
                now,
                now));

        mockMvc.perform(post("/api/v1/admin/showtimes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "movieId":"%s",
                                  "auditoriumId":"%s",
                                  "startsAt":"%s",
                                  "basePrice":90000.00
                                }
                                """.formatted(deletedMovieId, UUID.randomUUID(),
                                now.plus(Duration.ofHours(3)).truncatedTo(ChronoUnit.SECONDS))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("MOVIE_NOT_FOUND"));
    }

    @Test
    void guestMayReadTheScheduleWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/showtimes")
                        .queryParam("movieId", MOVIE_ID.toString())
                        .queryParam("date", LocalDate.now(ZoneOffset.UTC).toString()))
                .andExpect(status().isOk());
    }

    @Test
    void customerCannotCreateAnAuditorium() throws Exception {
        mockMvc.perform(post("/api/v1/admin/auditoriums")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("customer").roles("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Unauthorized","seatLayout":[{"row":"A","from":1,"to":1,"type":"STANDARD"}]}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void duplicateSeatPositionReturnsConflict() throws Exception {
        mockMvc.perform(post("/api/v1/admin/auditoriums")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Duplicate Seat Room %s","seatLayout":[
                                  {"row":"A","from":1,"to":2,"type":"STANDARD"},
                                  {"row":"a","from":2,"to":3,"type":"VIP"}
                                ]}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("DUPLICATE_SEAT_POSITION"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void overlappingShowtimeReturnsConflict() throws Exception {
        MvcResult auditoriumResult = mockMvc.perform(post("/api/v1/admin/auditoriums")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Overlap Room %s","seatLayout":[
                                  {"row":"A","from":1,"to":1,"type":"STANDARD"}
                                ]}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isCreated())
                .andReturn();
        UUID auditoriumId = UUID.fromString(objectMapper.readTree(auditoriumResult.getResponse().getContentAsString())
                .path("id").asText());
        Instant firstStartsAt = Instant.now().plus(Duration.ofHours(2)).truncatedTo(ChronoUnit.SECONDS);

        mockMvc.perform(post("/api/v1/admin/showtimes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(showtimeRequest(auditoriumId, firstStartsAt)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/admin/showtimes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(showtimeRequest(auditoriumId, firstStartsAt.plus(Duration.ofMinutes(60)))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("SCHEDULE_OVERLAP"));
    }

    private static String showtimeRequest(UUID auditoriumId, Instant startsAt) {
        return """
                {"movieId":"%s","auditoriumId":"%s","startsAt":"%s","basePrice":90000.00}
                """.formatted(MOVIE_ID, auditoriumId, startsAt);
    }

    @Test
    void theaterRoutesArePublishedInOpenApi() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/admin/auditoriums'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/admin/auditoriums'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/admin/showtimes'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/showtimes'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/showtimes/{id}'].get").exists());
    }
}
