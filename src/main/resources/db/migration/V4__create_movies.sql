CREATE TABLE movies (
    id UUID PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    duration_minutes INTEGER NOT NULL,
    age_rating VARCHAR(10) NOT NULL,
    release_date DATE NOT NULL,
    poster_url VARCHAR(500),
    status VARCHAR(20) NOT NULL,
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_movie_duration CHECK (duration_minutes > 0),
    CONSTRAINT chk_movie_age_rating CHECK (age_rating IN ('P', 'T13', 'T16', 'T18')),
    CONSTRAINT chk_movie_status CHECK (status IN ('COMING_SOON', 'NOW_SHOWING', 'ENDED'))
);

CREATE INDEX idx_movies_status_release_date ON movies (status, release_date);
