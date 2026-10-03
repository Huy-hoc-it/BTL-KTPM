package com.example.cinema.modules.identity.data;

import com.example.cinema.modules.identity.business.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
class UserEntity {
    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private User.Role role;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected UserEntity() {
    }

    UserEntity(User user) {
        id = user.id();
        username = user.username();
        passwordHash = user.passwordHash();
        role = user.role();
        createdAt = user.createdAt();
        updatedAt = user.updatedAt();
    }

    User toModel() {
        return new User(id, username, passwordHash, role, createdAt, updatedAt);
    }
}
