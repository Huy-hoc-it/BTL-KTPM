package com.example.cinema.modules.movie.data;

import com.example.cinema.modules.movie.business.MovieModuleApi;
import java.util.Optional;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "cinema.movie.enabled", havingValue = "true", matchIfMissing = true)
class MovieRepositoryAdapter implements MovieModuleApi {

    private final MovieJpaRepository movieRepository;

    MovieRepositoryAdapter(MovieJpaRepository movieRepository) {
        this.movieRepository = movieRepository;
    }

    @Override
    public Optional<MovieSnapshot> findMovieForScheduling(UUID movieId) {
        return movieRepository.findByIdAndDeletedAtIsNull(movieId)
                .map(movie -> new MovieSnapshot(movie.getId(), movie.getDurationMinutes()));
    }
}
