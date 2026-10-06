package com.example.cinema.modules.identity.api;

import com.example.cinema.modules.identity.business.User;
import java.util.UUID;

public record UserResponse(Data data) {
    public static UserResponse from(User user, User.Role role) {
        return new UserResponse(new Data(user.id(), user.username(), role));
    }

    public record Data(UUID id, String username, User.Role role) {
    }
}
