package com.example.cinema.modules.booking.business;

import com.example.cinema.modules.theater.business.ShowtimeCancellationGuard;
import com.example.cinema.modules.theater.business.ShowtimeHasBookingsException;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "cinema.booking.enabled", havingValue = "true", matchIfMissing = true)
public class BookingShowtimeCancellationGuard implements ShowtimeCancellationGuard {
    private final BookingRepository bookingRepository;

    public BookingShowtimeCancellationGuard(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    @Override
    public void checkCancellationAllowed(UUID showtimeId) {
        if (bookingRepository.existsConfirmedByShowtimeId(showtimeId)) {
            throw new ShowtimeHasBookingsException(showtimeId);
        }
    }
}
