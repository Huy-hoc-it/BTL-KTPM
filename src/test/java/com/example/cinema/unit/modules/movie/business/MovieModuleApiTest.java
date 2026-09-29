package com.example.cinema.unit.modules.movie.business;

import com.example.cinema.modules.movie.business.MovieModuleApi.MovieSnapshot;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MovieModuleApiTest {

    @Test
    void movieSnapshotRequiresPositiveDuration() {
        assertThatThrownBy(() -> new MovieSnapshot(UUID.randomUUID(), 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("durationMinutes must be greater than zero");
    }
}
