package com.example.cinema.modules.movie.data;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.PostgreSQLContainer;

public class MovieTestDatabase {
    private static PostgreSQLContainer<?> postgres;

    public static void configure(DynamicPropertyRegistry registry) {
        String url=System.getenv("MOVIE_TEST_DB_URL");
        if (url == null || url.isBlank()) {
            if (postgres == null) {
                postgres=new PostgreSQLContainer<>("postgres:17-alpine");
                postgres.start();
            }
            registry.add("spring.datasource.url",postgres::getJdbcUrl);
            registry.add("spring.datasource.username",postgres::getUsername);
            registry.add("spring.datasource.password",postgres::getPassword);
        } else {
            if (!"YES".equals(System.getenv("ALLOW_MOVIE_TEST_DB_RESET")) ||
                    !url.matches("jdbc:postgresql://(localhost|127\\.0\\.0\\.1):[0-9]+/[a-zA-Z0-9_]+_test")) {
                throw new IllegalStateException("Use a separate local database ending in _test and ALLOW_MOVIE_TEST_DB_RESET=YES");
            }
            registry.add("spring.datasource.url",() -> url);
            registry.add("spring.datasource.username",() -> System.getenv("MOVIE_TEST_DB_USERNAME"));
            registry.add("spring.datasource.password",() -> System.getenv("MOVIE_TEST_DB_PASSWORD"));
        }
    }
}
