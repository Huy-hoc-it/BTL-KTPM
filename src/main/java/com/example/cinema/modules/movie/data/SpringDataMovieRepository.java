package com.example.cinema.modules.movie.data;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.cinema.modules.movie.business.MovieStatus;
import java.util.UUID;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;

public interface SpringDataMovieRepository extends JpaRepository<MovieEntity, UUID>{
    public Optional<MovieEntity> findByIdAndDeletedAtIsNull(UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from MovieEntity m where m.id = :id and m.deletedAt is null")
    public Optional<MovieEntity> findUndeletedByIdForUpdate(UUID id);

    public Page<MovieEntity> findByDeletedAtIsNull(Pageable pageable);

    public Page<MovieEntity> findByStatusAndDeletedAtIsNull(MovieStatus status, Pageable pageable);
}
