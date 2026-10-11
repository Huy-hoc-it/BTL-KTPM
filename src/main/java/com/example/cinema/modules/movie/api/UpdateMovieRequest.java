package com.example.cinema.modules.movie.api;

import com.example.cinema.modules.movie.business.MovieStatus;
import java.time.LocalDate;
import com.example.cinema.modules.movie.business.AgeRating;

public class UpdateMovieRequest {
    private String title;
    private String description;
    private Integer durationMinutes;
    private AgeRating ageRating;
    private LocalDate releaseDate;
    private String posterUrl;
    private MovieStatus status;
    private boolean posterUrlProvided;

    public UpdateMovieRequest() {
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
        this.posterUrlProvided = true;
    }

    public MovieStatus getStatus() {
        return status;
    }

    public void setStatus(MovieStatus status) {
        this.status = status;
    }

    public boolean hasPosterUrl() {
        return posterUrlProvided;
    }
}
