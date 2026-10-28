package com.example.cinema.modules.movie.data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.EnumType;
import java.time.LocalDate;
import com.example.cinema.modules.movie.business.AgeRating;
import com.example.cinema.modules.movie.business.MovieStatus;

@Entity
@Table(name = "movies")
public class MovieEntity {
    @Id
    @Column(name = "id", nullable = false, unique = true)
    private UUID id;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column (name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes;

    @Enumerated(EnumType.STRING)
    @Column (name = "age_rating", nullable = false, length = 10)
    private AgeRating ageRating;

    @Column (name = "release_date", nullable = false)
    private LocalDate releaseDate;

    @Column (name = "poster_url", nullable = true, length = 500)
    private String posterUrl;

    @Enumerated (EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private MovieStatus status;

    @Column (name = "created_at", nullable = false)
    private Instant createdAt;

    @Column (name = "updated_at", nullable = false)
    private Instant updatedAt;
    
    @Column(name = "deleted_at", nullable = true)
    private Instant deletedAt;

    protected MovieEntity() {
        // Default constructor for JPA
    }

    public MovieEntity(UUID id, String title, String description, int durationMinutes, AgeRating ageRating, LocalDate releaseDate, String posterUrl, MovieStatus status, Instant createdAt, Instant updatedAt, Instant deletedAt) {
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
