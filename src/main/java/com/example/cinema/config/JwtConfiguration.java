package com.example.cinema.config;

import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JwtConfiguration {
    @Bean
    JwtTokenService jwtTokenService(
            @Value("${cinema.jwt.secret}") String secret,
            @Value("${cinema.jwt.expiration-seconds}") long expirationSeconds
    ) {
        return new JwtTokenService(secret, expirationSeconds, Clock.systemUTC());
    }
}
