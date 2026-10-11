package com.example.cinema.modules.movie.api;

import com.example.cinema.modules.movie.business.MovieService;
import com.example.cinema.modules.movie.business.MovieStatus;

import java.util.ArrayList;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.example.cinema.modules.movie.business.Movie;
import com.example.cinema.modules.movie.business.MoviePage;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/api/v1")
public class MovieController {
    private MovieService movieService;

    public MovieController(MovieService movieService) {
        this.movieService = movieService;
    }

    private MovieResponse toResponse(Movie movie) {
        return new MovieResponse(
                movie.getId(),
                movie.getTitle(),
                movie.getDescription(),
                movie.getDurationMinutes(),
                movie.getAgeRating(),
                movie.getReleaseDate(),
                movie.getPosterUrl(),
                movie.getStatus(),
                movie.getCreatedAt(),
                movie.getUpdatedAt()
        );
    }

    @PostMapping("/admin/movies")
    public ResponseEntity<SingleMovieResponse> create(@RequestBody CreateMovieRequest request) {
        Movie movie = movieService.create(
            request.getTitle(),
            request.getDescription(),
            request.getDurationMinutes(),
            request.getAgeRating(),
            request.getReleaseDate(),
            request.getPosterUrl(),
            request.getStatus()
        );
        MovieResponse movieResponse = toResponse(movie);
        SingleMovieResponse singleMovieResponse = new SingleMovieResponse(movieResponse);
        return ResponseEntity.status(HttpStatus.CREATED).body(singleMovieResponse);
    }

    @GetMapping("/movies/{movieId}")
    public ResponseEntity<SingleMovieResponse> getById(@PathVariable("movieId") UUID movieId) {
        Optional<Movie> movieOpt = movieService.getById(movieId);
        if (movieOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Movie movie = movieOpt.get();
        MovieResponse movieResponse = toResponse(movie);
        SingleMovieResponse response = new SingleMovieResponse(movieResponse);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/movies")
    public ResponseEntity<MovieListResponse> list(
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "pageSize", defaultValue = "20") int pageSize,
            @RequestParam(name = "status", required = false) MovieStatus status) {
        MoviePage moviePage = movieService.list(page, pageSize, status);
        List<MovieResponse> movieResponses = new ArrayList<>();
        for (Movie movie : moviePage.getItems()) {
            MovieResponse movieResponse = toResponse(movie);
            movieResponses.add(movieResponse);
        }
        long totalItems = moviePage.getTotalItems();
        int totalPages = (int) Math.ceil((double) totalItems / pageSize);
        PaginationMeta meta = new PaginationMeta(page, pageSize, totalItems, totalPages);
        MovieListResponse response = new MovieListResponse(movieResponses, meta);
        return ResponseEntity.ok(response);
    }
}
