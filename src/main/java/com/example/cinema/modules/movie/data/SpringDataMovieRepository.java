package com.example.cinema.modules.movie.data;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.cinema.modules.movie.business.MovieStatus;
import java.util.UUID;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SpringDataMovieRepository extends JpaRepository<MovieEntity, UUID>{
    public Optional<MovieEntity> findByIdAndDeletedAtIsNull(UUID id);

    public Page<MovieEntity> findByDeletedAtIsNull(Pageable pageable);

    public Page<MovieEntity> findByStatusAndDeletedAtIsNull(MovieStatus status, Pageable pageable);
}
