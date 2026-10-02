CREATE TABLE movies (
    id uuid PRIMARY KEY,
    title varchar(200) NOT NULL CHECK (btrim(title) <> ''),
    description text NOT NULL CHECK (btrim(description) <> ''),
    duration_minutes integer NOT NULL CHECK (duration_minutes > 0),
    age_rating varchar(10) NOT NULL
        CHECK (age_rating IN ('P', 'T13', 'T16', 'T18')),
    release_date date NOT NULL,
    poster_url varchar(500),
    status varchar(20) NOT NULL
        CHECK (status IN ('COMING_SOON', 'NOW_SHOWING', 'ENDED')),
    deleted_at timestamptz,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL
);

CREATE INDEX idx_movies_status_release_date
    ON movies (status, release_date)
    WHERE deleted_at IS NULL;