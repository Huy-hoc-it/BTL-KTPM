package com.example.cinema.modules.identity.api;

import com.example.cinema.modules.identity.business.User;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

public record UserResponse(Data data) {
    public static UserResponse from(User user, String role) {
        return new UserResponse(new Data(user.id(), user.username(), role));
    }

    public record Data(UUID id, String username,
                       @Schema(allowableValues = {"CUSTOMER", "ADMIN"}) String role) {
    }
}
