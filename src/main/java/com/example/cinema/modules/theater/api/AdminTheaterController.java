package com.example.cinema.modules.theater.api;

import com.example.cinema.modules.theater.business.Auditorium;
import com.example.cinema.modules.theater.business.AuditoriumService;
import com.example.cinema.modules.theater.business.CreateShowtimeCommand;
import com.example.cinema.modules.theater.business.Seat;
import com.example.cinema.modules.theater.business.Showtime;
import com.example.cinema.modules.theater.business.ShowtimeService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import java.util.UUID;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@SecurityRequirement(name = "bearerAuth")
@ApiResponses({
        @ApiResponse(responseCode = "400", description = "Malformed request"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "403", description = "Admin role required"),
        @ApiResponse(responseCode = "404", description = "Movie, auditorium or showtime not found"),
        @ApiResponse(responseCode = "409", description = "Duplicate name, seat, overlapping schedule, future showtimes or confirmed bookings"),
        @ApiResponse(responseCode = "422", description = "Validation or Theater business rule violation")
})
@RequestMapping("/api/v1/admin")
@ConditionalOnProperty(name = "cinema.theater.enabled", havingValue = "true", matchIfMissing = true)
public class AdminTheaterController {

    private final AuditoriumService auditoriumService;
    private final ShowtimeService showtimeService;

    public AdminTheaterController(AuditoriumService auditoriumService, ShowtimeService showtimeService) {
        this.auditoriumService = auditoriumService;
        this.showtimeService = showtimeService;
    }

    @GetMapping("/auditoriums")
    @Operation(summary = "List active auditoriums and their seat layouts")
    public List<AuditoriumResponse> listAuditoriums() {
        return auditoriumService.findActive().stream().map(this::toResponse).toList();
    }

    @PostMapping("/auditoriums")
    @ApiResponse(responseCode = "201", description = "Auditorium and seats created")
    public ResponseEntity<AuditoriumResponse> createAuditorium(@Valid @RequestBody CreateAuditoriumRequest request) {
        Auditorium auditorium = auditoriumService.create(request.toCommand());
        AuditoriumResponse response = toResponse(auditorium);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/showtimes")
    @ApiResponse(responseCode = "201", description = "Showtime created with a 15 minute cleanup buffer")
    public ResponseEntity<ShowtimeResponse> createShowtime(@Valid @RequestBody CreateShowtimeRequest request) {
        CreateShowtimeCommand command = request.toCommand();
        Showtime showtime = showtimeService.create(command);
        return ResponseEntity.created(java.net.URI.create("/api/v1/showtimes/" + showtime.id()))
                .body(ShowtimeController.toResponse(showtime));
    }

    @DeleteMapping("/auditoriums/{id}")
    @ApiResponse(responseCode = "204", description = "Auditorium soft deleted; seats retained")
    public ResponseEntity<Void> deleteAuditorium(@PathVariable UUID id) {
        auditoriumService.softDelete(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/showtimes/{id}")
    @ApiResponse(responseCode = "204", description = "Future scheduled showtime without confirmed bookings cancelled")
    public ResponseEntity<Void> cancelShowtime(@PathVariable UUID id) {
        showtimeService.cancel(id);
        return ResponseEntity.noContent().build();
    }

    private AuditoriumResponse toResponse(Auditorium auditorium) {
        List<Seat> seats = auditoriumService.findSeats(auditorium.id());
        List<AuditoriumResponse.SeatResponse> seatResponses = seats.stream()
                .map(seat -> new AuditoriumResponse.SeatResponse(
                        seat.id(), seat.rowLabel(), seat.seatNumber(), seat.type()))
                .toList();
        return new AuditoriumResponse(
                auditorium.id(),
                auditorium.name(),
                auditorium.status(),
                seatResponses.size(),
                auditorium.createdAt(),
                auditorium.updatedAt(),
                seatResponses);
    }
}
