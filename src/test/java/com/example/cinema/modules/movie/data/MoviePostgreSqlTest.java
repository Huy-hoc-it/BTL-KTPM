package com.example.cinema.modules.movie.data;

import com.example.cinema.config.JwtTokenService;
import com.example.cinema.modules.identity.business.*;
import com.example.cinema.modules.movie.business.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MoviePostgreSqlTest {
    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) { MovieTestDatabase.configure(registry); }
    @Autowired MovieService service;
    @Autowired SpringDataMovieRepository repository;
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired JwtTokenService tokens;
    @Autowired PlatformTransactionManager transactions;
    String admin;

    @BeforeEach
    void setUp() {
        jdbc.execute("TRUNCATE TABLE showtimes, seats, auditoriums, movies CASCADE");
        admin="Bearer "+tokens.issue(new AuthenticatedUser(UUID.randomUUID(),User.Role.ADMIN));
    }
    Movie create() { return service.create("Movie","Description",120,AgeRating.P,LocalDate.of(2026,10,1),"poster.jpg",MovieStatus.NOW_SHOWING); }

    @Test
    void realCrudPreservesNullPatchAndSoftDeleteContract() throws Exception {
        String response=mvc.perform(post("/api/v1/admin/movies").header("Authorization",admin).contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"Interstellar","description":"Space travel","durationMinutes":169,
                     "ageRating":"T13","releaseDate":"2026-10-01","status":"NOW_SHOWING","posterUrl":"poster.jpg"}
                    """)).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        UUID id=UUID.fromString(mapper.readTree(response).path("data").path("id").asText());
        Instant createdAt=service.getById(id).orElseThrow().getCreatedAt();
        mvc.perform(patch("/api/v1/admin/movies/"+id).header("Authorization",admin).contentType(MediaType.APPLICATION_JSON)
                .content("{\"durationMinutes\":140,\"posterUrl\":null}")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Interstellar")).andExpect(jsonPath("$.data.posterUrl").isEmpty());
        assertThat(service.findMovieForScheduling(id).orElseThrow().durationMinutes()).isEqualTo(140);
        assertThat(service.getById(id).orElseThrow().getCreatedAt()).isEqualTo(createdAt);
        mvc.perform(delete("/api/v1/admin/movies/"+id).header("Authorization",admin)).andExpect(status().isNoContent());
        assertThat(repository.findById(id).orElseThrow().getDeletedAt()).isNotNull();
        assertThat(service.getById(id)).isEmpty();
        assertThat(service.findMovieForScheduling(id)).isEmpty();
        assertThat(service.list(1,20,null).getTotalItems()).isZero();
        assertThat(service.softDelete(id)).isFalse();
    }

    @Test
    void databaseSortsEqualTimestampsByIdAndFiltersStatus() {
        Instant sameTime=Instant.parse("2026-10-01T00:00:00Z");
        for (int index=1;index<=3;index++) {
            UUID id=UUID.fromString("00000000-0000-0000-0000-00000000000"+index);
            repository.save(new MovieEntity(id,"Movie "+index,"Description",120,AgeRating.P,
                    LocalDate.of(2026,10,1),null,index==1?MovieStatus.ENDED:MovieStatus.NOW_SHOWING,sameTime,sameTime,null));
        }
        MoviePage page=service.list(1,2,null);
        assertThat(page.getTotalItems()).isEqualTo(3);
        assertThat(page.getItems()).extracting(Movie::getTitle).containsExactly("Movie 3","Movie 2");
        assertThat(service.list(2,2,null).getItems()).extracting(Movie::getTitle).containsExactly("Movie 1");
        assertThat(service.list(1,100,MovieStatus.NOW_SHOWING).getTotalItems()).isEqualTo(2);
    }

    @Test
    void invalidPatchLeavesDatabaseUnchanged() throws Exception {
        Movie movie=create();
        Instant updatedAt=service.getById(movie.getId()).orElseThrow().getUpdatedAt();
        mvc.perform(patch("/api/v1/admin/movies/"+movie.getId()).header("Authorization",admin).contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Changed\",\"durationMinutes\":0}")).andExpect(status().isUnprocessableEntity());
        Movie after=service.getById(movie.getId()).orElseThrow();
        assertThat(after.getTitle()).isEqualTo("Movie");
        assertThat(after.getUpdatedAt()).isEqualTo(updatedAt);
    }

    @Test
    void patchWaitsForDeleteAndCannotResurrectTheRow() throws Exception {
        UUID id=create().getId();
        CountDownLatch deleted=new CountDownLatch(1), release=new CountDownLatch(1);
        TransactionTemplate tx=new TransactionTemplate(transactions);
        try (ExecutorService executor=Executors.newFixedThreadPool(2)) {
            Future<Boolean> delete=executor.submit(() -> tx.execute(status -> {
                boolean result=service.softDelete(id); deleted.countDown();
                try { if (!release.await(10,TimeUnit.SECONDS)) { throw new IllegalStateException("Delete release timed out"); } }
                catch (InterruptedException error) { Thread.currentThread().interrupt(); throw new IllegalStateException(error); }
                return result;
            }));
            try {
                assertThat(deleted.await(5,TimeUnit.SECONDS)).isTrue();
                Future<Optional<Movie>> patch=executor.submit(() -> service.update(id,"Changed",null,null,null,null,null,null,EnumSet.of(MovieField.TITLE)));
                await().atMost(java.time.Duration.ofSeconds(5)).until(() -> jdbc.queryForObject(
                        "select count(*) from pg_stat_activity where datname=current_database() and wait_event_type='Lock' and query ilike '%movies%'",Integer.class)>0);
                release.countDown();
                assertThat(delete.get(5,TimeUnit.SECONDS)).isTrue();
                assertThat(patch.get(5,TimeUnit.SECONDS)).isEmpty();
                assertThat(repository.findById(id).orElseThrow().getDeletedAt()).isNotNull();
            } finally { release.countDown(); }
        }
    }

    @Test
    void flywayAndDatabaseConstraintsArePresent() {
        assertThat(jdbc.queryForList("select version from flyway_schema_history where success and version is not null",String.class))
                .contains("1","3","4","5").doesNotContain("2");
        assertThatThrownBy(() -> jdbc.update("insert into movies(id,title,description,duration_minutes,age_rating,release_date,status,created_at,updated_at) values (?,' ','D',120,'P',current_date,'NOW_SHOWING',now(),now())",UUID.randomUUID()))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }
}
