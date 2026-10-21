package com.example.cinema.modules.movie.api;

import com.example.cinema.modules.movie.business.AgeRating;
import com.example.cinema.modules.movie.business.MovieStatus;
import java.time.LocalDate;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

public class CreateMovieRequest {
    @NotBlank @Size(max = 200)
    private String title;
    @NotBlank
    private String description;
    @NotNull @Positive
    private Integer durationMinutes;
    @NotNull
    private AgeRating ageRating;
    @NotNull
    @JsonDeserialize(using = MovieDateDeserializer.class)
    private LocalDate releaseDate;
    @Size(max = 500)
    private String posterUrl;
    @NotNull
    private MovieStatus status;

    public CreateMovieRequest() {
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public AgeRating getAgeRating() {
        return ageRating;
    }

    public void setAgeRating(AgeRating ageRating) {
        this.ageRating = ageRating;
    }

    public LocalDate getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate(LocalDate releaseDate) {
        this.releaseDate = releaseDate;
    }

    public String getPosterUrl() {
        return posterUrl;
    }

    public void setPosterUrl(String posterUrl) {
        this.posterUrl = posterUrl;
    }

    public MovieStatus getStatus() {
        return status;
    }

    public void setStatus(MovieStatus status) {
        this.status = status;
    }
    
}
