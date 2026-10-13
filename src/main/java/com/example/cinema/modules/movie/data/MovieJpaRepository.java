package com.example.cinema.modules.movie.data;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovieJpaRepository extends JpaRepository<MovieEntity, UUID> {

    Optional<MovieEntity> findByIdAndDeletedAtIsNull(UUID movieId);
}
