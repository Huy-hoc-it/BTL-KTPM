package com.example.cinema.modules.identity.business;

import java.util.UUID;

public record AuthenticatedUser(UUID id, User.Role role) {
}
