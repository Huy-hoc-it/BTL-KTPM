package com.example.cinema.modules.movie.business;

public class MovieNotFoundException extends RuntimeException {
    public MovieNotFoundException() { super("Movie not found"); }
}
