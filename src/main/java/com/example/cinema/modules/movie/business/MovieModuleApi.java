package com.example.cinema.modules.movie.business;

import java.util.Optional;
import java.util.UUID;

public interface MovieModuleApi {
    public Optional<MovieForShowtime> findForShowtime(UUID movieId);
}
