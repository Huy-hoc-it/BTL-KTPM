package com.example.cinema.modules.identity.api;

import com.example.cinema.modules.identity.business.User;
import java.util.UUID;

public record RegisterResponse(Data data) {
    public static RegisterResponse from(User user) {
        return new RegisterResponse(new Data(user.id(), user.username(), user.role().name()));
    }

    public record Data(UUID id, String username, String role) {
    }
}
