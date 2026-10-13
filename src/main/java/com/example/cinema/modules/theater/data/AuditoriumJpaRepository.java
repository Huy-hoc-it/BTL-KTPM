package com.example.cinema.modules.theater.data;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.cinema.modules.theater.business.AuditoriumStatus;

public interface AuditoriumJpaRepository extends JpaRepository<AuditoriumEntity, UUID> {

    boolean existsByNameIgnoreCase(String name);

    List<AuditoriumEntity> findAllByStatusAndDeletedAtIsNullOrderByNameAsc(AuditoriumStatus status);
}
