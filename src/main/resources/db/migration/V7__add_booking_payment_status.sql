ALTER TABLE bookings
    ADD COLUMN payment_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    ADD CONSTRAINT chk_booking_payment_status CHECK (payment_status IN ('PENDING', 'PAID', 'FAILED'));
