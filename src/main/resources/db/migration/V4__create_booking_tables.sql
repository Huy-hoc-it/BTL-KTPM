CREATE TABLE bookings (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    showtime_id UUID NOT NULL,
    total_amount NUMERIC(12, 2) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_booking_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_booking_showtime FOREIGN KEY (showtime_id) REFERENCES showtimes(id),
    CONSTRAINT chk_booking_total_amount CHECK (total_amount >= 0)
);

CREATE TABLE booking_seats (
    id UUID PRIMARY KEY,
    booking_id UUID NOT NULL,
    showtime_id UUID NOT NULL,
    seat_id UUID NOT NULL,
    price NUMERIC(12, 2) NOT NULL,
    CONSTRAINT fk_booking_seat_booking FOREIGN KEY (booking_id) REFERENCES bookings(id) ON DELETE CASCADE,
    CONSTRAINT fk_booking_seat_showtime FOREIGN KEY (showtime_id) REFERENCES showtimes(id),
    CONSTRAINT fk_booking_seat_seat FOREIGN KEY (seat_id) REFERENCES seats(id),
    CONSTRAINT uq_booking_seat_in_booking UNIQUE (booking_id, seat_id),
    CONSTRAINT uq_booking_seat_per_showtime UNIQUE (showtime_id, seat_id),
    CONSTRAINT chk_booking_seat_price CHECK (price >= 0)
);

CREATE INDEX idx_bookings_user_created_at ON bookings(user_id, created_at DESC);
CREATE INDEX idx_bookings_showtime_id ON bookings(showtime_id);
