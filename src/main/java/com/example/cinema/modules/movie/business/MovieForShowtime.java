package com.example.cinema.modules.movie.business;

import java.util.UUID;

public class MovieForShowtime {
    private UUID id;
    private String title;
    private int durationMinutes;

    public MovieForShowtime(UUID id, String title, int durationMinutes) {
        this.id = id;
        this.title = title;
        this.durationMinutes = durationMinutes;
    }

    public UUID getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }   
}
