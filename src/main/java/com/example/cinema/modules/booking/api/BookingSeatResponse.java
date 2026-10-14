package com.example.cinema.modules.booking.api;

import com.example.cinema.modules.booking.business.bookingSeat;
import java.math.BigDecimal;
import java.util.UUID;

public class BookingSeatResponse {

    private final UUID seatId;
    private final BigDecimal price;

    public BookingSeatResponse(UUID seatId, BigDecimal price) {
        this.seatId = seatId;
        this.price = price;
    }

    public static BookingSeatResponse from(bookingSeat seat) {
        return new BookingSeatResponse(seat.getSeatId(), seat.getPrice());
    }

    public UUID getSeatId() { return seatId; }
    public BigDecimal getPrice() { return price; }
}
