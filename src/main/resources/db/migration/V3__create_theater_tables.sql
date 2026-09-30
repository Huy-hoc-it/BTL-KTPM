CREATE TABLE auditoriums (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL,
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_auditorium_name UNIQUE (name),
    CONSTRAINT chk_auditorium_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE TABLE seats (
    id UUID PRIMARY KEY,
    auditorium_id UUID NOT NULL,
    row_label VARCHAR(10) NOT NULL,
    seat_number INTEGER NOT NULL,
    type VARCHAR(20) NOT NULL,
    CONSTRAINT fk_seat_auditorium FOREIGN KEY (auditorium_id) REFERENCES auditoriums (id),
    CONSTRAINT uq_seat_position UNIQUE (auditorium_id, row_label, seat_number),
    CONSTRAINT chk_seat_number CHECK (seat_number > 0),
    CONSTRAINT chk_seat_type CHECK (type IN ('STANDARD', 'VIP'))
);

CREATE TABLE showtimes (
    id UUID PRIMARY KEY,
    movie_id UUID NOT NULL,
    auditorium_id UUID NOT NULL,
    starts_at TIMESTAMPTZ NOT NULL,
    ends_at TIMESTAMPTZ NOT NULL,
    base_price NUMERIC(12, 2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_showtime_auditorium FOREIGN KEY (auditorium_id) REFERENCES auditoriums (id),
    CONSTRAINT chk_showtime_period CHECK (ends_at > starts_at),
    CONSTRAINT chk_showtime_price CHECK (base_price >= 0),
    CONSTRAINT chk_showtime_status CHECK (status IN ('SCHEDULED', 'CANCELLED', 'FINISHED'))
);

CREATE INDEX idx_showtimes_movie_starts_at ON showtimes (movie_id, starts_at);
CREATE INDEX idx_showtimes_auditorium_starts_at ON showtimes (auditorium_id, starts_at);
