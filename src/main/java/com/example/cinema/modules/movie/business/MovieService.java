package com.example.cinema.modules.movie.business;

import java.time.LocalDate;
import java.util.UUID;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service 
@ConditionalOnProperty(name = "cinema.movie.enabled", havingValue = "true", matchIfMissing = true)
public class MovieService implements MovieModuleApi {
    private MovieRepository movieRepository;
    
    public MovieService(MovieRepository movieRepository) {
        this.movieRepository = movieRepository;
    }

    @Transactional
    public Movie create(String title, String description, Integer durationMinutes, AgeRating ageRating, LocalDate releaseDate, String posterUrl, MovieStatus status) {
        MovieValidation.validate(title, description, durationMinutes, ageRating, releaseDate, posterUrl, status);
        UUID id = UUID.randomUUID();
        Instant createdAt = Instant.now();
        Movie movie = new Movie(id, title, description, durationMinutes, ageRating, releaseDate, posterUrl, status, createdAt, createdAt, null);
        return movieRepository.save(movie);
    }

    public Optional<Movie> getById(UUID movieId) {
        if(movieId == null) {
            return Optional.empty();
        }
        else return movieRepository.findUndeletedById(movieId);
    }

    public MoviePage list(int page, int pageSize, MovieStatus status) {
        MovieValidation.validatePage(page, pageSize);
        return movieRepository.findUndeleted(page, pageSize, status);
    }

    public Optional<MovieForShowtime> findForShowtime(UUID movieId) {
        Optional<Movie> movieOpt = movieRepository.findUndeletedById(movieId);
        if(movieOpt.isEmpty()) {
            return Optional.empty(); 
        }
        Movie movie = movieOpt.get();
        MovieForShowtime movieForShowtime = new MovieForShowtime(movie.getId(), movie.getTitle(), movie.getDurationMinutes());
        return Optional.of(movieForShowtime);
    }

    @Override
    public Optional<MovieSnapshot> findMovieForScheduling(UUID movieId) {
        Optional<MovieForShowtime> movieOpt = findForShowtime(movieId);
        if (movieOpt.isEmpty()) {
            return Optional.empty();
        }
        MovieForShowtime movie = movieOpt.get();
        return Optional.of(new MovieSnapshot(movie.getId(), movie.getDurationMinutes()));
    }

    @Transactional
    public Optional<Movie> update(
            UUID movieId,
            String title,
            String description,
            Integer durationMinutes,
            AgeRating ageRating,
            LocalDate releaseDate,
            String posterUrl,
            MovieStatus status,
            Set<MovieField> providedFields) {
        MovieValidation.validatePatch(title, description, durationMinutes, ageRating, releaseDate, posterUrl, status, providedFields);
        Optional<Movie> movieOpt = movieRepository.findUndeletedById(movieId);
        if (movieOpt.isEmpty()) {
            return Optional.empty();
        }
        Movie oldmovie = movieOpt.get();
        String newTitle = oldmovie.getTitle();
        if(providedFields.contains(MovieField.TITLE)) {
            newTitle = title;
        }
        String newDescription = oldmovie.getDescription();
        if(providedFields.contains(MovieField.DESCRIPTION)) {
            newDescription = description;
        }  
        Integer newDurationMinutes = oldmovie.getDurationMinutes();
        if(providedFields.contains(MovieField.DURATION_MINUTES)) {
            newDurationMinutes = durationMinutes;
        }
        AgeRating newAgeRating = oldmovie.getAgeRating();
        if(providedFields.contains(MovieField.AGE_RATING)) {
            newAgeRating = ageRating;
        }
        LocalDate newReleaseDate = oldmovie.getReleaseDate();
        if(providedFields.contains(MovieField.RELEASE_DATE)) {
            newReleaseDate = releaseDate;
        }
        String newPosterUrl = oldmovie.getPosterUrl();
        if(providedFields.contains(MovieField.POSTER_URL)) {
            newPosterUrl = posterUrl;
        }
        MovieStatus newStatus = oldmovie.getStatus();
        if(providedFields.contains(MovieField.STATUS)) {
            newStatus = status;
        }  
        Instant newcreatedAt = oldmovie.getCreatedAt();
        MovieValidation.validate(newTitle, newDescription, newDurationMinutes, newAgeRating, newReleaseDate, newPosterUrl, newStatus);
        Instant newupdatedAt = Instant.now();
        Instant newdeletedAt = oldmovie.getDeletedAt();
        Movie newmovie = new Movie(movieId, newTitle, newDescription, newDurationMinutes, newAgeRating, newReleaseDate, newPosterUrl, newStatus, newcreatedAt, newupdatedAt, newdeletedAt);
        Movie savedMovie = movieRepository.save(newmovie);
        return Optional.of(savedMovie);
    }

    @Transactional
    public boolean softDelete(UUID movieId) {
        Optional<Movie> movieOpt = movieRepository.findUndeletedById(movieId);
        if (movieOpt.isEmpty()) {
            return false;
        }
        Movie oldMovie = movieOpt.get();
        Instant now = Instant.now();
        Movie deletedMovie = new Movie(
                oldMovie.getId(),
                oldMovie.getTitle(),
                oldMovie.getDescription(),
                oldMovie.getDurationMinutes(),
                oldMovie.getAgeRating(),
                oldMovie.getReleaseDate(),
                oldMovie.getPosterUrl(),
                oldMovie.getStatus(),
                oldMovie.getCreatedAt(),
                now,
                now
        );
        movieRepository.save(deletedMovie);
        return true;
    }
}
