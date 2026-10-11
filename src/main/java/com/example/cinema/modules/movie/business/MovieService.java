package com.example.cinema.modules.movie.business;

import java.time.LocalDate;
import java.util.UUID;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service 
public class MovieService implements MovieModuleApi {
    private MovieRepository movieRepository;
    
    public MovieService(MovieRepository movieRepository) {
        this.movieRepository = movieRepository;
    }

    public Movie create(String title, String description, int durationMinutes, AgeRating ageRating, LocalDate releaseDate, String posterUrl, MovieStatus status) {
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
        return movieRepository.findUndeleted(page, pageSize, status);
    }

    @Override
    public Optional<MovieForShowtime> findForShowtime(UUID movieId) {
        Optional<Movie> movieOpt = movieRepository.findUndeletedById(movieId);
        if(movieOpt.isEmpty()) {
            return Optional.empty(); 
        }
        Movie movie = movieOpt.get();
        MovieForShowtime movieForShowtime = new MovieForShowtime(movie.getId(), movie.getTitle(), movie.getDurationMinutes());
        return Optional.of(movieForShowtime);
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
            boolean posterUrlProvided) {
        Optional<Movie> movieOpt = movieRepository.findUndeletedById(movieId);
        if (movieOpt.isEmpty()) {
            return Optional.empty();
        }
        Movie oldmovie = movieOpt.get();
        String newTitle = oldmovie.getTitle();
        if(title != null) {
            newTitle = title;
        }
        String newDescription = oldmovie.getDescription();
        if(description != null) {
            newDescription = description;
        }  
        Integer newDurationMinutes = oldmovie.getDurationMinutes();
        if(durationMinutes != null) {
            newDurationMinutes = durationMinutes;
        }
        AgeRating newAgeRating = oldmovie.getAgeRating();
        if(ageRating != null) {
            newAgeRating = ageRating;
        }
        LocalDate newReleaseDate = oldmovie.getReleaseDate();
        if(releaseDate != null) {
            newReleaseDate = releaseDate;
        }
        String newPosterUrl = oldmovie.getPosterUrl();
        if(posterUrlProvided) {
            newPosterUrl = posterUrl;
        }
        MovieStatus newStatus = oldmovie.getStatus();
        if(status != null) {
            newStatus = status;
        }  
        Instant newcreatedAt = oldmovie.getCreatedAt();
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
