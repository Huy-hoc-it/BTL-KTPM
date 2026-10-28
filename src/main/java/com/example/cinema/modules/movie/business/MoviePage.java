package com.example.cinema.modules.movie.business;

import java.util.List;

public class MoviePage {
    private List<Movie> items;
    private long totalItems;

    public MoviePage(List<Movie> items, long totalItems) {
        this.items = items;
        this.totalItems = totalItems;
    }

    public List<Movie> getItems() {
        return items;
    }

    public long getTotalItems() {
        return totalItems;
    }
}
