-- Run after contention, before cancelling its winning booking.
-- psql: \set showtime_id 'uuid-from-fixtures'
--       \set seat_id 'uuid-from-fixtures'
-- Expected: active_claims=1, confirmed_bookings=1, duplicate_groups=0.
SELECT count(*) AS active_claims
FROM booking_seats
WHERE showtime_id = :'showtime_id'::uuid
  AND seat_id = :'seat_id'::uuid AND released_at IS NULL;

SELECT count(DISTINCT b.id) AS confirmed_bookings
FROM bookings b JOIN booking_seats bs ON bs.booking_id = b.id
WHERE b.status = 'CONFIRMED'
  AND bs.showtime_id = :'showtime_id'::uuid
  AND bs.seat_id = :'seat_id'::uuid AND bs.released_at IS NULL;

SELECT count(*) AS duplicate_groups FROM (
    SELECT showtime_id, seat_id FROM booking_seats
    WHERE released_at IS NULL GROUP BY showtime_id, seat_id HAVING count(*) > 1
) duplicates;
