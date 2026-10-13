package com.example.cinema.modules.theater.business;

public class MovieModuleUnavailableException extends TheaterBusinessException {

    public MovieModuleUnavailableException() {
        super("Movie module is not available");
    }
}
