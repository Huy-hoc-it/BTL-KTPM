package com.example.cinema.modules.theater.business;

import java.util.UUID;

public class MovieNotFoundException extends TheaterBusinessException {

    public MovieNotFoundException(UUID movieId) {
        super("Movie not found: " + movieId);
    }
}
