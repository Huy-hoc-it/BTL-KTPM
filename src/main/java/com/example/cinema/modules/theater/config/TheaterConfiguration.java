package com.example.cinema.modules.theater.config;

import com.example.cinema.modules.movie.business.MovieModuleApi;
import com.example.cinema.modules.theater.business.AuditoriumService;
import com.example.cinema.modules.theater.business.MovieModuleUnavailableException;
import com.example.cinema.modules.theater.business.SeatRepository;
import com.example.cinema.modules.theater.business.ShowtimeRepository;
import com.example.cinema.modules.theater.business.ShowtimeService;
import com.example.cinema.modules.theater.business.ShowtimeCancellationGuard;
import com.example.cinema.modules.theater.business.AuditoriumRepository;
import java.time.Clock;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "cinema.theater.enabled", havingValue = "true", matchIfMissing = true)
public class TheaterConfiguration {

    @Bean
    @ConditionalOnMissingBean(Clock.class)
    Clock theaterClock() {
        return Clock.systemUTC();
    }

    @Bean
    AuditoriumService auditoriumService(
            AuditoriumRepository auditoriumRepository,
            SeatRepository seatRepository,
            ShowtimeRepository showtimeRepository,
            Clock theaterClock
    ) {
        return new AuditoriumService(auditoriumRepository, seatRepository, showtimeRepository, theaterClock);
    }

    @Bean
    ShowtimeService showtimeService(
            ShowtimeRepository showtimeRepository,
            AuditoriumRepository auditoriumRepository,
            SeatRepository seatRepository,
            ObjectProvider<MovieModuleApi> movieModuleApiProvider,
            Clock theaterClock,
            List<ShowtimeCancellationGuard> cancellationGuards
    ) {
        MovieModuleApi movieModuleApi = movieId -> {
            MovieModuleApi provider = movieModuleApiProvider.getIfAvailable();
            if (provider == null) {
                throw new MovieModuleUnavailableException();
            }
            return provider.findMovieForScheduling(movieId);
        };
        return new ShowtimeService(
                showtimeRepository,
                auditoriumRepository,
                seatRepository,
                movieModuleApi,
                theaterClock,
                cancellationGuards);
    }
}
