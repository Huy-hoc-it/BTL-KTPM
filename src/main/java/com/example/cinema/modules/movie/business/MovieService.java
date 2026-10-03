package com.example.cinema.modules.movie.business;

import java.time.LocalDate;
import java.util.UUID;
import java.time.Instant;
import java.util.Optional;

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
}
