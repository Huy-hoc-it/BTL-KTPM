package com.example.cinema.unit.modules.movie.business;

import com.example.cinema.modules.movie.business.*;
import java.util.*;

class FakeMovieRepository implements MovieRepository {
    final Map<UUID, Movie> rows = new HashMap<>();
    public Movie save(Movie movie) { rows.put(movie.getId(), movie); return movie; }
    public Optional<Movie> findUndeletedById(UUID id) {
        Movie movie = rows.get(id);
        if (movie == null || movie.getDeletedAt() != null) { return Optional.empty(); }
        return Optional.of(movie);
    }
    public MoviePage findUndeleted(int page, int pageSize, MovieStatus status) {
        List<Movie> all = rows.values().stream()
                .filter(movie -> movie.getDeletedAt() == null && (status == null || movie.getStatus() == status))
                .sorted(Comparator.comparing(Movie::getCreatedAt).thenComparing(Movie::getId).reversed()).toList();
        return new MoviePage(all.stream().skip((long)(page - 1) * pageSize).limit(pageSize).toList(), all.size());
    }
}
