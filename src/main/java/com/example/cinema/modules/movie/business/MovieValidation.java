package com.example.cinema.modules.movie.business;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public class MovieValidation {
    private MovieValidation() { }

    public static void validate(String title, String description, Integer durationMinutes,
            AgeRating ageRating, LocalDate releaseDate, String posterUrl, MovieStatus status) {
        validateFields(title, description, durationMinutes, ageRating, releaseDate, posterUrl, status,
                EnumSet.allOf(MovieField.class));
    }

    public static void validatePatch(String title, String description, Integer durationMinutes,
            AgeRating ageRating, LocalDate releaseDate, String posterUrl, MovieStatus status,
            Set<MovieField> providedFields) {
        if (providedFields == null || providedFields.isEmpty()) {
            throw new MovieValidationException(List.of(new MovieFieldError("body", "Provide at least one movie field")));
        }
        validateFields(title, description, durationMinutes, ageRating, releaseDate, posterUrl, status, providedFields);
    }

    private static void validateFields(String title, String description, Integer durationMinutes,
            AgeRating ageRating, LocalDate releaseDate, String posterUrl, MovieStatus status,
            Set<MovieField> fields) {
        List<MovieFieldError> errors = new ArrayList<>();
        if (fields.contains(MovieField.TITLE)) {
            if (title == null || title.isBlank()) {
                errors.add(new MovieFieldError("title", "Must not be blank"));
            } else if (title.length() > 200) {
                errors.add(new MovieFieldError("title", "Must be at most 200 characters"));
            }
        }
        if (fields.contains(MovieField.DESCRIPTION) && (description == null || description.isBlank())) {
            errors.add(new MovieFieldError("description", "Must not be blank"));
        }
        if (fields.contains(MovieField.DURATION_MINUTES) && (durationMinutes == null || durationMinutes <= 0)) {
            errors.add(new MovieFieldError("durationMinutes", "Must be greater than zero"));
        }
        if (fields.contains(MovieField.AGE_RATING) && ageRating == null) {
            errors.add(new MovieFieldError("ageRating", "Must not be null"));
        }
        if (fields.contains(MovieField.RELEASE_DATE) && releaseDate == null) {
            errors.add(new MovieFieldError("releaseDate", "Must not be null"));
        }
        if (fields.contains(MovieField.POSTER_URL) && posterUrl != null && posterUrl.length() > 500) {
            errors.add(new MovieFieldError("posterUrl", "Must be at most 500 characters"));
        }
        if (fields.contains(MovieField.STATUS) && status == null) {
            errors.add(new MovieFieldError("status", "Must not be null"));
        }
        if (!errors.isEmpty()) { throw new MovieValidationException(errors); }
    }

    public static void validatePage(int page, int pageSize) {
        List<MovieFieldError> errors = new ArrayList<>();
        if (page < 1) { errors.add(new MovieFieldError("page", "Must be at least 1")); }
        if (pageSize < 1 || pageSize > 100) {
            errors.add(new MovieFieldError("pageSize", "Must be between 1 and 100"));
        }
        if (!errors.isEmpty()) { throw new MovieValidationException(errors); }
    }
}
