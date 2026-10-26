package com.example.cinema.modules.booking.api;

import com.example.cinema.modules.booking.business.BookingService;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/bookings")
@ConditionalOnProperty(name = "cinema.booking.enabled", havingValue = "true", matchIfMissing = true)
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    public ResponseEntity<BookingDetailResponse> create(
            @RequestBody CreateBookingRequest request,
            @AuthenticationPrincipal UUID userId
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(BookingDetailResponse.from(
                bookingService.createBooking(userId, request.getShowtimeId(), request.getSeatIds())));
    }

    @GetMapping
    public ResponseEntity<List<BookingSummaryResponse>> getAllByUserId(@AuthenticationPrincipal UUID userId) {
        List<BookingSummaryResponse> response = bookingService.getBookingsByUserId(userId).stream()
                .map(BookingSummaryResponse::from)
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{bookingId}")
    public ResponseEntity<BookingDetailResponse> getByUserIdAndBookingId(
            @PathVariable UUID bookingId,
            @AuthenticationPrincipal UUID userId
    ) {
        return ResponseEntity.ok(BookingDetailResponse.from(
                bookingService.getBookingByUserId(userId, bookingId)));
    }

    @DeleteMapping("/{bookingId}")
    public ResponseEntity<Void> cancel(
            @PathVariable UUID bookingId,
            @AuthenticationPrincipal UUID userId
    ) {
        bookingService.cancelBooking(userId, bookingId);
        return ResponseEntity.noContent().build();
    }
}
