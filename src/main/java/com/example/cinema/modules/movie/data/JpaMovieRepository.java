package com.example.cinema.modules.movie.data;

import com.example.cinema.modules.movie.business.Movie;
import com.example.cinema.modules.movie.business.MovieRepository;
import com.example.cinema.modules.movie.business.MoviePage;
import com.example.cinema.modules.movie.business.MovieStatus;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Repository;
import java.util.ArrayList;
import java.util.UUID;
import java.util.Optional;

@Repository
public class JpaMovieRepository implements MovieRepository {
    private SpringDataMovieRepository springDataMovieRepository;

    public JpaMovieRepository(SpringDataMovieRepository  springDataMovieRepository) {
        this.springDataMovieRepository = springDataMovieRepository;
    }

    private MovieEntity toEntity(Movie movie) {
        return new MovieEntity(
                movie.getId(),
                movie.getTitle(),
                movie.getDescription(),
                movie.getDurationMinutes(),
                movie.getAgeRating(),
                movie.getReleaseDate(),
                movie.getPosterUrl(),
                movie.getStatus(),
                movie.getCreatedAt(),
                movie.getUpdatedAt(),
                movie.getDeletedAt()
        );
    }

    private Movie toMovie(MovieEntity movieEntity) {
        return new Movie(
                movieEntity.getId(),
                movieEntity.getTitle(),
                movieEntity.getDescription(),
                movieEntity.getDurationMinutes(),
                movieEntity.getAgeRating(),
                movieEntity.getReleaseDate(),
                movieEntity.getPosterUrl(),
                movieEntity.getStatus(),
                movieEntity.getCreatedAt(),
                movieEntity.getUpdatedAt(),
                movieEntity.getDeletedAt()
        );
    }

    @Override 
    public Movie save(Movie movie) {
        MovieEntity movieEntity = toEntity(movie);
        MovieEntity savedEntity = springDataMovieRepository.save(movieEntity);
        return toMovie(savedEntity);
    }

    @Override 
    public Optional<Movie> findUndeletedById(UUID id) {
        Optional<MovieEntity> movieEntity = springDataMovieRepository.findByIdAndDeletedAtIsNull(id);
        if (movieEntity.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(toMovie(movieEntity.get()));
    }

    @Override
    public MoviePage findUndeleted(int page, int pageSize, MovieStatus status) {
        PageRequest pageRequest = PageRequest.of(page - 1, pageSize, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        Page<MovieEntity> movieEntities;
        if (status == null) {
            movieEntities = springDataMovieRepository.findByDeletedAtIsNull(pageRequest);
        } else {
            movieEntities = springDataMovieRepository.findByStatusAndDeletedAtIsNull(status, pageRequest);
        }
        ArrayList<Movie> movies = new ArrayList<>();
        for (MovieEntity movieEntity : movieEntities) {
            movies.add(toMovie(movieEntity));
        }
        return new MoviePage(movies, movieEntities.getTotalElements());
    }
}
