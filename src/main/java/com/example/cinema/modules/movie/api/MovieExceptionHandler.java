package com.example.cinema.modules.movie.api;

import com.example.cinema.modules.movie.business.MovieFieldError;
import com.example.cinema.modules.movie.business.MovieNotFoundException;
import com.example.cinema.modules.movie.business.MovieValidationException;
import com.example.cinema.shared.api.ErrorResponse;
import com.example.cinema.shared.api.RequestIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackageClasses = MovieController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MovieExceptionHandler {
    @ExceptionHandler(MovieNotFoundException.class)
    ResponseEntity<ErrorResponse> handleNotFound(HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ErrorResponse.of("MOVIE_NOT_FOUND", "Movie not found", RequestIdFilter.get(request)));
    }

    @ExceptionHandler(MovieValidationException.class)
    ResponseEntity<ErrorResponse> handleValidation(MovieValidationException exception, HttpServletRequest request) {
        return validationError(request, exception.getDetails());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> handleInvalidBody(MethodArgumentNotValidException exception, HttpServletRequest request) {
        List<MovieFieldError> details = new ArrayList<>();
        for (var fieldError : exception.getBindingResult().getFieldErrors()) {
            details.add(new MovieFieldError(fieldError.getField(), fieldError.getDefaultMessage()));
        }
        return validationError(request, details);
    }

    private ResponseEntity<ErrorResponse> validationError(HttpServletRequest request, List<MovieFieldError> details) {
        ErrorResponse response = new ErrorResponse(new ErrorResponse.ApiError("VALIDATION_ERROR", "Movie request is invalid",
                new ArrayList<>(details), RequestIdFilter.get(request)));
        return ResponseEntity.unprocessableEntity().body(response);
    }
}
