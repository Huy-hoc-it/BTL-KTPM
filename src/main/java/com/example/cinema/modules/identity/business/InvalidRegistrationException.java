package com.example.cinema.modules.identity.business;

public class InvalidRegistrationException extends RuntimeException {
    public InvalidRegistrationException() {
        super("Registration data is invalid");
    }
}
