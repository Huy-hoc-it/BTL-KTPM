package com.example.cinema.modules.movie.api;

import com.example.cinema.modules.movie.business.MovieStatus;
import java.time.LocalDate;
import com.example.cinema.modules.movie.business.AgeRating;
import com.example.cinema.modules.movie.business.MovieField;
import java.util.EnumSet;
import java.util.Set;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

public class UpdateMovieRequest {
    private String title;
    private String description;
    private Integer durationMinutes;
    private AgeRating ageRating;
    @JsonDeserialize(using = MovieDateDeserializer.class)
    private LocalDate releaseDate;
    private String posterUrl;
    private MovieStatus status;
    private final Set<MovieField> providedFields = EnumSet.noneOf(MovieField.class);

    public UpdateMovieRequest() {
    }

    public String getTitle() {
        return title;
    }
  
    public void setTitle(String title) {
        this.title = title;
        providedFields.add(MovieField.TITLE);
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
        providedFields.add(MovieField.DESCRIPTION);
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
        providedFields.add(MovieField.DURATION_MINUTES);
    }

    public AgeRating getAgeRating() {
        return ageRating;
    }

    public void setAgeRating(AgeRating ageRating) {
        this.ageRating = ageRating;
        providedFields.add(MovieField.AGE_RATING);
    }

    public LocalDate getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate(LocalDate releaseDate) {
        this.releaseDate = releaseDate;
        providedFields.add(MovieField.RELEASE_DATE);
    }

    public String getPosterUrl() {
        return posterUrl;
    }

    public void setPosterUrl(String posterUrl) {
        this.posterUrl = posterUrl;
        providedFields.add(MovieField.POSTER_URL);
    }

    public MovieStatus getStatus() {
        return status;
    }

    public void setStatus(MovieStatus status) {
        this.status = status;
        providedFields.add(MovieField.STATUS);
    }

    public boolean hasPosterUrl() {
        return providedFields.contains(MovieField.POSTER_URL);
    }

    public Set<MovieField> providedFields() { return Set.copyOf(providedFields); }
}
