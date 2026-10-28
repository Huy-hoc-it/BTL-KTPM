package com.example.cinema.modules.theater.api;

import com.example.cinema.modules.theater.business.Showtime;
import com.example.cinema.modules.theater.business.ShowtimeService;
import com.example.cinema.modules.theater.business.ShowtimeStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/showtimes")
@ConditionalOnProperty(name = "cinema.theater.enabled", havingValue = "true", matchIfMissing = true)
public class ShowtimeController {

    private final ShowtimeService showtimeService;

    public ShowtimeController(ShowtimeService showtimeService) {
        this.showtimeService = showtimeService;
    }

    @GetMapping
    @Operation(summary = "List scheduled showtimes for a movie on a UTC date",
            description = "Uses [00:00 UTC, next day 00:00 UTC); excludes CANCELLED and FINISHED.")
    @ApiResponse(responseCode = "400", description = "Missing or malformed movieId or date")
    public List<ShowtimeResponse> listByMovieAndDate(
            @RequestParam UUID movieId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        Instant from = date.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant through = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        return showtimeService.findByMovieIdAndStartsAtBetween(movieId, from, through).stream()
                .filter(showtime -> showtime.status() == ShowtimeStatus.SCHEDULED)
                .map(ShowtimeController::toResponse)
                .toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get showtime details, including CANCELLED and FINISHED showtimes")
    @ApiResponse(responseCode = "404", description = "Showtime not found")
    public ShowtimeResponse getById(@PathVariable("id") UUID showtimeId) {
        return toResponse(showtimeService.findById(showtimeId));
    }

    static ShowtimeResponse toResponse(Showtime showtime) {
        return new ShowtimeResponse(
                showtime.id(),
                showtime.movieId(),
                showtime.auditoriumId(),
                showtime.startsAt(),
                showtime.endsAt(),
                showtime.basePrice(),
                showtime.status());
    }
}
