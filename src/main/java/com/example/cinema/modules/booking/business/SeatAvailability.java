package com.example.cinema.modules.booking.business;

import com.example.cinema.modules.theater.business.ShowtimeModuleApi.SeatSnapshot;

public record SeatAvailability(SeatSnapshot seat, boolean booked) {
}
