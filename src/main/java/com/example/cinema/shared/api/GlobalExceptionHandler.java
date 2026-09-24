package com.example.cinema.shared.api;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ErrorResponse> handleNotFound(HttpServletRequest request) {
        return error(request, HttpStatus.NOT_FOUND, "NOT_FOUND", "Resource not found");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ErrorResponse> handleMalformedRequest(HttpServletRequest request) {
        return error(request, HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST", "Request is malformed");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ErrorResponse> handleInvalidParameter(HttpServletRequest request) {
        return error(request, HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST", "Request is malformed");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> handleValidationError(HttpServletRequest request) {
        return error(request, HttpStatus.UNPROCESSABLE_ENTITY, "VALIDATION_ERROR", "Request is invalid");
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> handleUnexpectedError(Exception exception, HttpServletRequest request) {
        if (exception instanceof org.springframework.web.ErrorResponse webError) {
            HttpStatusCode status = webError.getStatusCode();
            HttpStatus knownStatus = HttpStatus.resolve(status.value());
            String code = status.value() == 400 ? "MALFORMED_REQUEST"
                    : knownStatus == null ? "HTTP_" + status.value() : knownStatus.name();
            String message = knownStatus == null ? "HTTP error" : knownStatus.getReasonPhrase();
            return ResponseEntity.status(status).headers(webError.getHeaders())
                    .body(ErrorResponse.of(code, message, RequestIdFilter.get(request)));
        }
        log.error("Unexpected error requestId={} type={}", RequestIdFilter.get(request), exception.getClass().getName());
        return error(request, HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "An unexpected error occurred");
    }

    private ResponseEntity<ErrorResponse> error(HttpServletRequest request, HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(ErrorResponse.of(code, message, RequestIdFilter.get(request)));
    }
}
