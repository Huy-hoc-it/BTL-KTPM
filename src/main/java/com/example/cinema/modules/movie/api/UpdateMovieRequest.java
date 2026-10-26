package com.example.cinema.modules.movie.api;

import com.example.cinema.modules.movie.business.MovieStatus;
import java.time.LocalDate;
import com.example.cinema.modules.movie.business.AgeRating;
import com.example.cinema.modules.movie.business.MovieField;
import java.util.EnumSet;
import java.util.Set;
import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.annotation.JsonAnySetter;

@Schema(description = "Field không gửi giữ nguyên. Gửi posterUrl: null để xóa ảnh.",
        minProperties = 1, additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
public class UpdateMovieRequest {
    @JsonDeserialize(using = MovieStringDeserializer.class)
    private String title;
    @JsonDeserialize(using = MovieStringDeserializer.class)
    private String description;
    @JsonDeserialize(using = MovieIntegerDeserializer.class)
    private Integer durationMinutes;
    @JsonDeserialize(using = MovieAgeRatingDeserializer.class)
    private AgeRating ageRating;
    @JsonDeserialize(using = MovieDateDeserializer.class)
    private LocalDate releaseDate;
    @Schema(types = {"string", "null"}, maxLength = 500)
    @JsonDeserialize(using = MovieStringDeserializer.class)
    private String posterUrl;
    @JsonDeserialize(using = MovieStatusDeserializer.class)
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

    @JsonAnySetter
    public void rejectUnknownField(String fieldName, Object value) {
        throw new IllegalArgumentException("Unknown movie field: " + fieldName);
    }
}
