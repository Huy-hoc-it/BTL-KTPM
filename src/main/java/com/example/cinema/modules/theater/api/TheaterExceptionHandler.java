package com.example.cinema.modules.theater.api;

import com.example.cinema.modules.theater.business.AuditoriumNameAlreadyExistsException;
import com.example.cinema.modules.theater.business.AuditoriumNotFoundException;
import com.example.cinema.modules.theater.business.DuplicateSeatPositionException;
import com.example.cinema.modules.theater.business.MovieModuleUnavailableException;
import com.example.cinema.modules.theater.business.MovieNotFoundException;
import com.example.cinema.modules.theater.business.ScheduleOverlapException;
import com.example.cinema.modules.theater.business.ShowtimeNotFoundException;
import com.example.cinema.modules.theater.business.TheaterBusinessException;
import com.example.cinema.shared.api.ErrorResponse;
import com.example.cinema.shared.api.RequestIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackageClasses = AdminTheaterController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TheaterExceptionHandler {

    @ExceptionHandler(MovieNotFoundException.class)
    ResponseEntity<ErrorResponse> handleMovieNotFound(HttpServletRequest request) {
        return error(request, HttpStatus.NOT_FOUND, "MOVIE_NOT_FOUND", "Movie not found");
    }

    @ExceptionHandler(AuditoriumNotFoundException.class)
    ResponseEntity<ErrorResponse> handleAuditoriumNotFound(HttpServletRequest request) {
        return error(request, HttpStatus.NOT_FOUND, "AUDITORIUM_NOT_FOUND", "Auditorium not found");
    }

    @ExceptionHandler(ShowtimeNotFoundException.class)
    ResponseEntity<ErrorResponse> handleShowtimeNotFound(HttpServletRequest request) {
        return error(request, HttpStatus.NOT_FOUND, "SHOWTIME_NOT_FOUND", "Showtime not found");
    }

    @ExceptionHandler(AuditoriumNameAlreadyExistsException.class)
    ResponseEntity<ErrorResponse> handleAuditoriumNameConflict(HttpServletRequest request) {
        return error(request, HttpStatus.CONFLICT, "AUDITORIUM_ALREADY_EXISTS", "Auditorium name already exists");
    }

    @ExceptionHandler(DuplicateSeatPositionException.class)
    ResponseEntity<ErrorResponse> handleDuplicateSeatPosition(HttpServletRequest request) {
        return error(request, HttpStatus.CONFLICT, "DUPLICATE_SEAT_POSITION", "Seat position already exists");
    }

    @ExceptionHandler(ScheduleOverlapException.class)
    ResponseEntity<ErrorResponse> handleScheduleOverlap(HttpServletRequest request) {
        return error(request, HttpStatus.CONFLICT, "SCHEDULE_OVERLAP", "Showtime overlaps an existing schedule");
    }

    @ExceptionHandler(MovieModuleUnavailableException.class)
    ResponseEntity<ErrorResponse> handleMovieModuleUnavailable(HttpServletRequest request) {
        return error(request, HttpStatus.SERVICE_UNAVAILABLE, "MOVIE_MODULE_UNAVAILABLE", "Movie lookup is unavailable");
    }

    @ExceptionHandler(TheaterBusinessException.class)
    ResponseEntity<ErrorResponse> handleTheaterBusinessError(HttpServletRequest request) {
        return error(request, HttpStatus.UNPROCESSABLE_ENTITY, "THEATER_RULE_VIOLATION", "Theater rule was not satisfied");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ErrorResponse> handleIllegalArgument(HttpServletRequest request) {
        return error(request, HttpStatus.UNPROCESSABLE_ENTITY, "VALIDATION_ERROR", "Request is invalid");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ErrorResponse> handleDataIntegrityViolation(HttpServletRequest request) {
        return error(request, HttpStatus.CONFLICT, "DATA_CONFLICT", "The request conflicts with stored data");
    }

    private ResponseEntity<ErrorResponse> error(
            HttpServletRequest request,
            HttpStatus status,
            String code,
            String message
    ) {
        return ResponseEntity.status(status).body(ErrorResponse.of(code, message, RequestIdFilter.get(request)));
    }
}
