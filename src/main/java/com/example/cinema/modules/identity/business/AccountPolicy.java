package com.example.cinema.modules.identity.business;

import java.util.Locale;
import java.util.regex.Pattern;

public final class AccountPolicy {
    private static final Pattern USERNAME_PATTERN = Pattern.compile("[a-zA-Z0-9._-]+");

    private AccountPolicy() {
    }

    public static String normalizeUsername(String username) {
        return username == null ? null : username.trim().toLowerCase(Locale.ROOT);
    }

    public static boolean isValidUsername(String username) {
        return username != null && username.length() >= 3 && username.length() <= 50
                && USERNAME_PATTERN.matcher(username).matches();
    }

    public static boolean isValidPassword(String password) {
        return password != null && password.length() >= 8 && password.length() <= 16;
    }
}
