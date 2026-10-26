package com.example.cinema.modules.booking.api;

import com.example.cinema.modules.booking.business.BookingService;
import com.example.cinema.modules.booking.business.SeatAvailability;
import com.example.cinema.modules.theater.business.ShowtimeModuleApi.SeatKind;
import com.example.cinema.modules.theater.business.ShowtimeNotFoundException;
import com.example.cinema.shared.api.ErrorResponse;
import com.example.cinema.shared.api.RequestIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ConditionalOnProperty(name = "cinema.booking.enabled", havingValue = "true", matchIfMissing = true)
public class ShowtimeSeatController {
    private final BookingService bookingService;

    public ShowtimeSeatController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @GetMapping("/api/v1/showtimes/{id}/seats")
    @Operation(summary = "Get physical seats and current availability for a showtime")
    @ApiResponse(responseCode = "404", description = "Showtime not found")
    public List<SeatResponse> list(@PathVariable UUID id) {
        return bookingService.getSeatAvailability(id).stream().map(SeatResponse::from).toList();
    }

    @ExceptionHandler(ShowtimeNotFoundException.class)
    ResponseEntity<ErrorResponse> showtimeNotFound(HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorResponse.of(
                "SHOWTIME_NOT_FOUND", "Showtime not found", RequestIdFilter.get(request)));
    }

    public record SeatResponse(UUID id, String row, int number, SeatKind type, String status) {
        static SeatResponse from(SeatAvailability availability) {
            var seat = availability.seat();
            return new SeatResponse(seat.seatId(), seat.rowLabel(), seat.seatNumber(), seat.type(),
                    availability.booked() ? "BOOKED" : "AVAILABLE");
        }
    }
}
