package com.example.cinema.shared.api;

import java.util.List;

public record ErrorResponse(ApiError error) {

    public static ErrorResponse of(String code, String message, String requestId) {
        return new ErrorResponse(new ApiError(code, message, List.of(), requestId));
    }

    public record ApiError(String code, String message, List<Object> details, String requestId) {
    }
}
