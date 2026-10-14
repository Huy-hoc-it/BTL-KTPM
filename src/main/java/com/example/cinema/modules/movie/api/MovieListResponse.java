package com.example.cinema.modules.movie.api;

import java.util.List;

public class MovieListResponse {
    private java.util.List<MovieResponse> data;
    private PaginationMeta meta;

    public MovieListResponse(List<MovieResponse> data, PaginationMeta meta) {
        this.data = data;
        this.meta = meta;
    }

    public List<MovieResponse> getData() {
        return data;
    }

    public PaginationMeta getMeta() {
        return meta;
    }
}
