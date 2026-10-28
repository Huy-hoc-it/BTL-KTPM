package com.example.cinema.unit.modules.movie.business;

import com.example.cinema.modules.movie.business.*;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.EnumSource;
import static org.assertj.core.api.Assertions.*;

class MovieServiceTest {
    private FakeMovieRepository repository;
    private MovieService service;
    private final LocalDate releaseDate = LocalDate.of(2026,10,1);

    @BeforeEach
    void setUp() { repository = new FakeMovieRepository(); service = new MovieService(repository); }

    private Movie create() {
        return service.create("Interstellar", "Space travel", 169, AgeRating.T13, releaseDate,
                "https://example.com/poster.jpg", MovieStatus.COMING_SOON);
    }

    @Test
    void createsMovieAndProvidesSnapshotWithoutExposingPersistence() {
        Movie movie = create();
        assertThat(movie.getId()).isNotNull();
        assertThat(movie.getCreatedAt()).isEqualTo(movie.getUpdatedAt());
        assertThat(movie.getDeletedAt()).isNull();
        assertThat(service.getById(movie.getId())).contains(movie);
        assertThat(service.findForShowtime(movie.getId()).orElseThrow().getTitle()).isEqualTo("Interstellar");
        assertThat(service.findMovieForScheduling(movie.getId()))
                .contains(new MovieModuleApi.MovieSnapshot(movie.getId(),169));
    }

    @ParameterizedTest
    @MethodSource("invalidCreate")
    void rejectsInvalidCreateBeforeSaving(String title, String description, Integer duration, AgeRating rating,
            LocalDate date, String poster, MovieStatus status, String field) {
        assertThatThrownBy(() -> service.create(title,description,duration,rating,date,poster,status))
                .isInstanceOfSatisfying(MovieValidationException.class, error ->
                        assertThat(error.getDetails()).extracting(MovieFieldError::getField).contains(field));
        assertThat(repository.rows).isEmpty();
    }

    static Stream<Arguments> invalidCreate() {
        List<Arguments> cases = new ArrayList<>();
        Object[] values = {"Movie","Description",120,AgeRating.P,LocalDate.of(2026,10,1),null,MovieStatus.NOW_SHOWING};
        Object[][] bad = {{0,null,"title"},{0," ","title"},{0,"x".repeat(201),"title"},
                {1,null,"description"},{1,"\t","description"},{2,null,"durationMinutes"},{2,0,"durationMinutes"},
                {2,-1,"durationMinutes"},{3,null,"ageRating"},{4,null,"releaseDate"},{5,"x".repeat(501),"posterUrl"},{6,null,"status"}};
        for (Object[] invalid : bad) {
            Object[] args = Arrays.copyOf(values,8); args[(int)invalid[0]]=invalid[1]; args[7]=invalid[2];
            cases.add(Arguments.of(args));
        }
        return cases.stream();
    }

    @Test
    void acceptsSchemaBoundaryValues() {
        Movie movie = service.create("x".repeat(200),"D",1,AgeRating.P,releaseDate,"x".repeat(500),MovieStatus.ENDED);
        assertThat(movie.getTitle()).hasSize(200);
        assertThat(movie.getPosterUrl()).hasSize(500);
        assertThat(movie.getDurationMinutes()).isEqualTo(1);
    }

    @Test
    void patchKeepsOmittedFieldsAndOriginalCreationTime() {
        Movie old = create();
        Movie updated = service.update(old.getId(),"Changed",null,null,null,null,null,null,
                EnumSet.of(MovieField.TITLE)).orElseThrow();
        assertThat(updated.getTitle()).isEqualTo("Changed");
        assertThat(updated.getDescription()).isEqualTo(old.getDescription());
        assertThat(updated.getDurationMinutes()).isEqualTo(169);
        assertThat(updated.getAgeRating()).isEqualTo(old.getAgeRating());
        assertThat(updated.getReleaseDate()).isEqualTo(releaseDate);
        assertThat(updated.getPosterUrl()).isEqualTo(old.getPosterUrl());
        assertThat(updated.getStatus()).isEqualTo(old.getStatus());
        assertThat(updated.getCreatedAt()).isEqualTo(old.getCreatedAt());
        assertThat(updated.getUpdatedAt()).isAfterOrEqualTo(old.getUpdatedAt());
    }

    @Test
    void updatesAllFieldsAndClearsPosterOnlyWhenPresent() {
        Movie old = create();
        Movie updated = service.update(old.getId(),"New","New description",90,AgeRating.T18,releaseDate.plusDays(1),
                "https://example.com/new.jpg",MovieStatus.NOW_SHOWING,EnumSet.allOf(MovieField.class)).orElseThrow();
        assertThat(updated.getDescription()).isEqualTo("New description");
        assertThat(updated.getAgeRating()).isEqualTo(AgeRating.T18);
        assertThat(updated.getReleaseDate()).isEqualTo(releaseDate.plusDays(1));
        assertThat(updated.getStatus()).isEqualTo(MovieStatus.NOW_SHOWING);
        assertThat(updated.getPosterUrl()).endsWith("new.jpg");
        assertThat(service.findMovieForScheduling(old.getId()).orElseThrow().durationMinutes()).isEqualTo(90);
        updated = service.update(old.getId(),null,null,null,null,null,null,null,
                EnumSet.of(MovieField.POSTER_URL)).orElseThrow();
        assertThat(updated.getPosterUrl()).isNull();
    }

    @ParameterizedTest
    @EnumSource(value=MovieField.class, names="POSTER_URL", mode=EnumSource.Mode.EXCLUDE)
    void explicitNullOnRequiredPatchFieldDoesNotWrite(MovieField field) {
        Movie old = create();
        assertThatThrownBy(() -> service.update(old.getId(),null,null,null,null,null,null,null,EnumSet.of(field)))
                .isInstanceOf(MovieValidationException.class);
        assertThat(repository.rows.get(old.getId())).isSameAs(old);
    }

    @Test
    void invalidOrEmptyPatchDoesNotWrite() {
        Movie old = create();
        assertThatThrownBy(() -> service.update(old.getId(),"Changed",null,0,null,null,null,null,
                EnumSet.of(MovieField.TITLE,MovieField.DURATION_MINUTES))).isInstanceOf(MovieValidationException.class);
        assertThatThrownBy(() -> service.update(old.getId(),null,null,null,null,null,null,null,Set.of()))
                .isInstanceOf(MovieValidationException.class);
        assertThatThrownBy(() -> service.update(old.getId(),null,null,null,null,null,null,null,null))
                .isInstanceOf(MovieValidationException.class);
        assertThat(repository.rows.get(old.getId())).isSameAs(old);
    }

    @ParameterizedTest
    @CsvSource({"0,20","-1,20","1,0","1,-1","1,101"})
    void validatesPagination(int page, int size) {
        assertThatThrownBy(() -> service.list(page,size,null)).isInstanceOf(MovieValidationException.class);
    }

    @Test
    void listsAndFiltersAndSoftDeletesWithoutRemovingTheRow() {
        Movie movie = create();
        assertThat(service.list(1,100,MovieStatus.COMING_SOON).getTotalItems()).isEqualTo(1);
        assertThat(service.list(1,1,MovieStatus.NOW_SHOWING).getItems()).isEmpty();
        assertThat(service.softDelete(movie.getId())).isTrue();
        Movie deleted = repository.rows.get(movie.getId());
        assertThat(deleted.getDeletedAt()).isNotNull().isEqualTo(deleted.getUpdatedAt());
        assertThat(deleted.getCreatedAt()).isEqualTo(movie.getCreatedAt());
        assertThat(service.getById(movie.getId())).isEmpty();
        assertThat(service.findForShowtime(movie.getId())).isEmpty();
        assertThat(service.findMovieForScheduling(movie.getId())).isEmpty();
        assertThat(service.list(1,20,null).getTotalItems()).isZero();
        assertThat(service.softDelete(movie.getId())).isFalse();
    }

    @Test
    void missingMovieDoesNotSaveOrReturnSnapshot() {
        UUID id = UUID.randomUUID();
        assertThat(service.getById(null)).isEmpty();
        assertThat(service.getById(id)).isEmpty();
        assertThat(service.findMovieForScheduling(id)).isEmpty();
        assertThat(service.softDelete(id)).isFalse();
        assertThat(service.update(id,"New",null,null,null,null,null,null,EnumSet.of(MovieField.TITLE))).isEmpty();
        assertThat(repository.rows).isEmpty();
    }
}
