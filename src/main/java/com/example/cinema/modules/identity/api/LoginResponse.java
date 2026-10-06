package com.example.cinema.modules.identity.api;

public record LoginResponse(Data data) {
    public static LoginResponse bearerToken(String accessToken, long expiresIn) {
        return new LoginResponse(new Data(accessToken, "Bearer", expiresIn));
    }

    public record Data(String accessToken, String tokenType, long expiresIn) {
        @Override
        public String toString() {
            return "Data[redacted]";
        }
    }
}
