package com.example.cinema.modules.theater.business;

import java.util.List;
import java.util.Objects;

public record CreateAuditoriumCommand(String name, List<SeatDefinition> seatLayout) {

    public CreateAuditoriumCommand {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(seatLayout, "seatLayout must not be null");
        if (name.isBlank() || name.trim().length() > 100) {
            throw new IllegalArgumentException("name must contain 1 to 100 characters");
        }
        if (seatLayout.isEmpty()) {
            throw new IllegalArgumentException("seatLayout must not be empty");
        }
        seatLayout = List.copyOf(seatLayout);
    }

    public record SeatDefinition(String rowLabel, int from, int to, SeatType type) {

        public SeatDefinition {
            Objects.requireNonNull(rowLabel, "rowLabel must not be null");
            Objects.requireNonNull(type, "type must not be null");
            if (rowLabel.isBlank() || rowLabel.trim().length() > 10) {
                throw new IllegalArgumentException("rowLabel must contain 1 to 10 characters");
            }
            if (from <= 0 || to < from) {
                throw new IllegalArgumentException("seat range is invalid");
            }
        }
    }
}
