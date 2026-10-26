package com.example.cinema.modules.booking.data;

import java.util.UUID;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface BookingJpaRepository extends JpaRepository<BookingEntity, UUID> {
    Optional<BookingEntity> findByIdAndUserId(UUID bookingId, UUID userId);
    List<BookingEntity> findAllByUserIdOrderByCreatedAtDesc(UUID userId);
}
