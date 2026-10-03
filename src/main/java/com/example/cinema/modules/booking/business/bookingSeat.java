package com.example.cinema.modules.booking.business;

import java.math.BigDecimal;
import java.util.UUID;

public class bookingSeat {
    private UUID seatID;
    private BigDecimal price;

    public bookingSeat(UUID seatID, BigDecimal price) {
        this.seatID = seatID;
        this.price = price;
    }

    public UUID getSeatId() {
        return seatID;
    }

    public BigDecimal getPrice() {
        return price;
    }
}
