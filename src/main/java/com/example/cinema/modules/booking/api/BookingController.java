package com.example.cinema.modules.booking.api;

import com.example.cinema.modules.booking.business.BookingService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    public ResponseEntity<BookingDetailResponse> create(@RequestBody CreateBookingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(BookingDetailResponse.from(
                bookingService.createBooking(request.getUserId(), request.getShowtimeId(), request.getSeatIds())));
    }

    @GetMapping
    public ResponseEntity<List<BookingSummaryResponse>> getAllByUserId(@RequestParam UUID userId) {
        List<BookingSummaryResponse> response = bookingService.getBookingsByUserId(userId).stream()
                .map(BookingSummaryResponse::from)
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{bookingId}")
    public ResponseEntity<BookingDetailResponse> getByUserIdAndBookingId(
            @PathVariable UUID bookingId,
            @RequestParam UUID userId
    ) {
        return ResponseEntity.ok(BookingDetailResponse.from(
                bookingService.getBookingByUserId(userId, bookingId)));
    }
}
