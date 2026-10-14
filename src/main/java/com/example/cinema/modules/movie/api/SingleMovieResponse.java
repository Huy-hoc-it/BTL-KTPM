package com.example.cinema.modules.movie.api;

public class SingleMovieResponse {
    private MovieResponse data;

    public SingleMovieResponse(MovieResponse data) {
        this.data = data;
    }

    public MovieResponse getData() {
        return data;
    }
}
