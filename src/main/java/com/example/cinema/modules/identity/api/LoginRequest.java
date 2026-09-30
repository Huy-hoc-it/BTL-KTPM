package com.example.cinema.modules.identity.api;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotNull @Size(min = 3, max = 50) @Pattern(regexp = "[a-zA-Z0-9._-]+")
        @JsonDeserialize(using = RegisterRequest.StrictStringDeserializer.class) String username,
        @NotNull @Size(min = 8, max = 16)
        @JsonDeserialize(using = RegisterRequest.StrictStringDeserializer.class) String password
) {
    public LoginRequest {
        username = username == null ? null : username.trim();
    }

    @Override
    public String toString() {
        return "LoginRequest[redacted]";
    }
}
