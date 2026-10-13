package com.example.cinema.modules.theater.api;

import com.example.cinema.modules.theater.business.Auditorium;
import com.example.cinema.modules.theater.business.AuditoriumService;
import com.example.cinema.modules.theater.business.CreateShowtimeCommand;
import com.example.cinema.modules.theater.business.Seat;
import com.example.cinema.modules.theater.business.Showtime;
import com.example.cinema.modules.theater.business.ShowtimeService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
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
    public List<AuditoriumResponse> listAuditoriums() {
        return auditoriumService.findActive().stream().map(this::toResponse).toList();
    }

    @PostMapping("/auditoriums")
    public ResponseEntity<AuditoriumResponse> createAuditorium(@Valid @RequestBody CreateAuditoriumRequest request) {
        Auditorium auditorium = auditoriumService.create(request.toCommand());
        AuditoriumResponse response = toResponse(auditorium);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/showtimes")
    public ResponseEntity<ShowtimeResponse> createShowtime(@Valid @RequestBody CreateShowtimeRequest request) {
        CreateShowtimeCommand command = request.toCommand();
        Showtime showtime = showtimeService.create(command);
        return ResponseEntity.created(java.net.URI.create("/api/v1/showtimes/" + showtime.id()))
                .body(ShowtimeController.toResponse(showtime));
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
