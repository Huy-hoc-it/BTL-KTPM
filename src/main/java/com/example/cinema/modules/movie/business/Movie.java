package com.example.cinema.modules.movie.business;

import java.time.LocalDate;
import java.time.Instant;
import java.util.UUID;

public class Movie {
    private UUID id;
    private String title;
    private String description;
    private int durationMinutes;
    private AgeRating ageRating;
    private LocalDate releaseDate;
    private String posterUrl;
    private MovieStatus status;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant deletedAt;

    public Movie(UUID id, String title, String description, int durationMinutes, AgeRating ageRating,
                 LocalDate releaseDate, String posterUrl, MovieStatus status, Instant createdAt,
                 Instant updatedAt, Instant deletedAt) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.durationMinutes = durationMinutes;
        this.ageRating = ageRating;
        this.releaseDate = releaseDate;
        this.posterUrl = posterUrl;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
    }

    public UUID getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public AgeRating getAgeRating() {
        return ageRating;
    }

    public LocalDate getReleaseDate() {
        return releaseDate;
    }

    public String getPosterUrl() {
        return posterUrl;
    }

    public MovieStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }
}
