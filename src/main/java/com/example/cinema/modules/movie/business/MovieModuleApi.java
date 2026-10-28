package com.example.cinema.modules.movie.business;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Public business contract used by other modules that need movie information.
 */
public interface MovieModuleApi {

    /**
     * Finds the minimal movie data required to schedule a showtime.
     *
     * @param movieId movie identifier
     * @return an empty result when the movie does not exist or has been soft-deleted
     */
    Optional<MovieSnapshot> findMovieForScheduling(UUID movieId);

    /**
     * Immutable view of the movie data that Theater is allowed to consume.
     */
    record MovieSnapshot(UUID movieId, int durationMinutes) {

        public MovieSnapshot {
            Objects.requireNonNull(movieId, "movieId must not be null");
            if (durationMinutes <= 0) {
                throw new IllegalArgumentException("durationMinutes must be greater than zero");
            }
        }
    }
}
