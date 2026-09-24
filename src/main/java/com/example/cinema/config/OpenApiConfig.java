package com.example.cinema.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI cinemaOpenApi() {
        return new OpenAPI().info(new Info().title("Cinema API").version("v1"));
    }
}
