package com.example.cinema.modules.movie.api;

import com.example.cinema.modules.movie.business.MovieService;
import com.example.cinema.modules.movie.business.MovieStatus;

import java.util.ArrayList;

import org.springframework.http.ResponseEntity;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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
import com.example.cinema.modules.movie.business.MovieNotFoundException;
import com.example.cinema.shared.api.ErrorResponse;
import jakarta.validation.Valid;
import java.net.URI;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1")
@ConditionalOnProperty(name = "cinema.movie.enabled", havingValue = "true", matchIfMissing = true)
@Tag(name = "Movie", description = "Xem phim và quản lý thông tin phim")
@ApiResponses({
    @ApiResponse(responseCode = "400", description = "JSON, UUID hoặc query sai kiểu", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(responseCode = "422", description = "Dữ liệu không hợp lệ; details chỉ ra field và lý do", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(responseCode = "500", description = "Lỗi hệ thống", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
})
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
    @Operation(summary = "Tạo phim", description = "Yêu cầu quyền Admin.", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Đã tạo phim", content = @Content(schema = @Schema(implementation = SingleMovieResponse.class))),
        @ApiResponse(responseCode = "401", description = "Thiếu hoặc sai token", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Không có quyền Admin", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<SingleMovieResponse> create(@Valid @RequestBody CreateMovieRequest request) {
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
        return ResponseEntity.created(URI.create("/api/v1/movies/" + movie.getId())).body(singleMovieResponse);
    }

    @GetMapping("/movies/{movieId}")
    @Operation(summary = "Xem chi tiết phim", description = "Cần đăng nhập. Phim đã xóa mềm được xem là không tồn tại.", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Thông tin phim", content = @Content(schema = @Schema(implementation = SingleMovieResponse.class))),
        @ApiResponse(responseCode = "404", description = "Không tìm thấy phim", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<SingleMovieResponse> getById(@PathVariable("movieId") UUID movieId) {
        Optional<Movie> movieOpt = movieService.getById(movieId);
        if (movieOpt.isEmpty()) {
            throw new MovieNotFoundException();
        }
        Movie movie = movieOpt.get();
        MovieResponse movieResponse = toResponse(movie);
        SingleMovieResponse response = new SingleMovieResponse(movieResponse);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/movies")
    @Operation(summary = "Danh sách phim", description = "Cần đăng nhập. Sắp xếp createdAt giảm dần, rồi id giảm dần; không bao gồm phim đã xóa.", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "200", description = "Danh sách và phân trang", content = @Content(schema = @Schema(implementation = MovieListResponse.class)))
    public ResponseEntity<MovieListResponse> list(
            @Parameter(description = "Trang bắt đầu từ 1", schema = @Schema(minimum = "1", defaultValue = "1"))
            @RequestParam(name = "page", defaultValue = "1") int page,
            @Parameter(description = "Số phim mỗi trang", schema = @Schema(minimum = "1", maximum = "100", defaultValue = "20"))
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

    @PatchMapping("/admin/movies/{movieId}")
    @Operation(summary = "Cập nhật phim", description = "Yêu cầu Admin. Field không gửi giữ nguyên; posterUrl: null xóa ảnh. Body rỗng hoặc null ở field bắt buộc trả 422.", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Phim sau cập nhật", content = @Content(schema = @Schema(implementation = SingleMovieResponse.class))),
        @ApiResponse(responseCode = "404", description = "Không tìm thấy phim", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "Thiếu hoặc sai token", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Không có quyền Admin", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<SingleMovieResponse> update(
            @PathVariable("movieId") UUID movieId,
            @RequestBody UpdateMovieRequest request) {
        Optional<Movie> movieOpt = movieService.update(
                movieId,
                request.getTitle(),
                request.getDescription(),
                request.getDurationMinutes(),
                request.getAgeRating(),
                request.getReleaseDate(),
                request.getPosterUrl(),
                request.getStatus(),
                request.providedFields()
        );
        if(movieOpt.isEmpty()) {
            throw new MovieNotFoundException();
        }
        Movie movie = movieOpt.get();
        MovieResponse movieResponse = toResponse(movie);
        SingleMovieResponse response = new SingleMovieResponse(movieResponse);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/admin/movies/{movieId}")
    @Operation(summary = "Xóa mềm phim", description = "Yêu cầu Admin. Giữ hàng dữ liệu và ghi deletedAt; xóa lại trả 404.", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Đã xóa phim; không có response body", content = @Content),
        @ApiResponse(responseCode = "404", description = "Không tìm thấy phim", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "Thiếu hoặc sai token", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Không có quyền Admin", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> delete(
            @PathVariable("movieId") UUID movieId
    ) {
        boolean deleted = movieService.softDelete(movieId);
        if (deleted == false) {
            throw new MovieNotFoundException();
        }
        return ResponseEntity.noContent().build();
    }
}
