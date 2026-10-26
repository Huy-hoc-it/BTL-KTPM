package com.example.cinema.modules.movie.business;

import java.util.List;

public class MovieValidationException extends RuntimeException {
    private final List<MovieFieldError> details;

    public MovieValidationException(List<MovieFieldError> details) {
        super("Movie request is invalid");
        this.details = List.copyOf(details);
    }

    public List<MovieFieldError> getDetails() { return details; }
}
