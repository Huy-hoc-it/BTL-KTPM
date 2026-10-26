package com.example.cinema.modules.theater.data;

import org.hibernate.exception.ConstraintViolationException;

final class TheaterConstraintViolation {
    private TheaterConstraintViolation() {
    }

    static boolean isConstraint(Throwable failure, String name) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause instanceof ConstraintViolationException violation
                    && name.equals(violation.getConstraintName())) {
                return true;
            }
        }
        return false;
    }
}
