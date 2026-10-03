package com.example.cinema.modules.identity.business;

public class UsernameAlreadyExistsException extends RuntimeException {
    public UsernameAlreadyExistsException() {
        super("Username is already registered");
    }
}
