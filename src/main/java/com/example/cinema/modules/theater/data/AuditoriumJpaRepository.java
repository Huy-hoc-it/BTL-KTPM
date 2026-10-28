package com.example.cinema.modules.theater.data;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import com.example.cinema.modules.theater.business.AuditoriumStatus;

public interface AuditoriumJpaRepository extends JpaRepository<AuditoriumEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from AuditoriumEntity a where a.id = :id")
    Optional<AuditoriumEntity> findByIdForUpdate(UUID id);

    boolean existsByNameIgnoreCase(String name);

    List<AuditoriumEntity> findAllByStatusAndDeletedAtIsNullOrderByNameAsc(AuditoriumStatus status);
}
