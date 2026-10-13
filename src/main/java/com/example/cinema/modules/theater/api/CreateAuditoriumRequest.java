package com.example.cinema.modules.theater.api;

import com.example.cinema.modules.theater.business.CreateAuditoriumCommand;
import com.example.cinema.modules.theater.business.SeatType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CreateAuditoriumRequest(
        @NotBlank @Size(max = 100) String name,
        @NotEmpty List<@Valid SeatLayoutRequest> seatLayout
) {

    public CreateAuditoriumCommand toCommand() {
        return new CreateAuditoriumCommand(name, seatLayout.stream()
                .map(seat -> new CreateAuditoriumCommand.SeatDefinition(
                        seat.row(), seat.from(), seat.to(), seat.type()))
                .toList());
    }

    public record SeatLayoutRequest(
            @NotBlank @Size(max = 10) String row,
            @Min(1) int from,
            @Min(1) int to,
            @NotNull SeatType type
    ) {

        @AssertTrue(message = "to must be greater than or equal to from")
        public boolean isRangeValid() {
            return to >= from;
        }
    }
}
