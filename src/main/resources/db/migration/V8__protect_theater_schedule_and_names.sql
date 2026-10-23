CREATE EXTENSION IF NOT EXISTS btree_gist;

-- Adjacent intervals are allowed, including the 15 minute cleanup buffer.
ALTER TABLE showtimes ADD CONSTRAINT ex_showtimes_no_overlap
    EXCLUDE USING gist (
        auditorium_id WITH =,
        tstzrange(starts_at, ends_at, '[)') WITH &&
    ) WHERE (status <> 'CANCELLED');

-- Match the case-insensitive business check even for concurrent requests.
CREATE UNIQUE INDEX uq_auditorium_name_lower ON auditoriums (lower(name));
