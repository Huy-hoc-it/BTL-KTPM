package com.example.cinema.modules.movie.business;

public class MovieFieldError {
    private final String field;
    private final String reason;

    public MovieFieldError(String field, String reason) {
        this.field = field;
        this.reason = reason;
    }

    public String getField() { return field; }
    public String getReason() { return reason; }
}
