package com.example.cinema.modules.booking.data;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface BookingJpaRepository extends JpaRepository<BookingEntity, UUID> {
}
