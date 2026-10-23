package com.example.healthmate.utils;

import java.util.regex.Pattern;

/*
 * All the input checks for the Add, Edit and Settings screens.
 *
 * No Android code in here, only plain Java, so I can test every rule
 * with normal JUnit tests (ValidationUtilsTest).
 * Each check gives back an Error. The screen turns the Error into a message
 * from strings.xml (see ValidationMessages).
 */
public final class ValidationUtils {

    public static final int MAX_STEPS = 100_000;
    public static final int MAX_CALORIES = 20_000;
    public static final int MAX_WATER_ML = 20_000;
    public static final int MAX_SLEEP_HOURS = 24;
    public static final int MAX_SLEEP_MINUTES_PART = 59;
    public static final int MAX_SLEEP_TOTAL_MINUTES = 24 * 60;

    // limits for the goals on the Settings screen
    public static final int MIN_GOAL_STEPS = 100;
    public static final int MAX_GOAL_STEPS = 100_000;
    public static final int MIN_GOAL_WATER = 100;
    public static final int MAX_GOAL_WATER = 10_000;
    public static final int MIN_GOAL_CALORIES = 50;
    public static final int MAX_GOAL_CALORIES = 10_000;
    public static final double MIN_GOAL_SLEEP = 1.0;
    public static final double MAX_GOAL_SLEEP = 24.0;

    // limits for the login screen
    public static final int MIN_USERNAME = 3;
    public static final int MAX_USERNAME = 20;
    public static final int MIN_PASSWORD = 6;

    // more than 9 digits could overflow an int, so I stop before parsing
    private static final int MAX_DIGITS = 9;

    private static final Pattern WHOLE_NUMBER = Pattern.compile("^\\d+$");
    private static final Pattern DECIMAL_NUMBER = Pattern.compile("^\\d{1,2}(\\.\\d{1,2})?$");
    private static final Pattern NEGATIVE_NUMBER = Pattern.compile("^-\\s*\\d+([.,]\\d+)?$");
    private static final Pattern HAS_DECIMAL_PART = Pattern.compile("^\\d+[.,]\\d*$");
    private static final Pattern USERNAME = Pattern.compile("^[A-Za-z0-9_]+$");

    public enum Error {
        NONE,
        REQUIRED,
        NOT_WHOLE_NUMBER,
        NEGATIVE,
        TOO_LARGE,
        TOO_SMALL,
        INVALID_NUMBER,
        OUT_OF_RANGE,
        SLEEP_OVER_24H,
        INVALID_DATE,
        FUTURE_DATE,
        USERNAME_INVALID,
        PASSWORD_TOO_SHORT,
        PASSWORDS_DONT_MATCH
    }

    // result of reading a number: an error, or the number
    public static final class NumberResult {
        public final Error error;
        public final double value;

        private NumberResult(Error error, double value) {
            this.error = error;
            this.value = value;
        }

        static NumberResult ok(double value) {
            return new NumberResult(Error.NONE, value);
        }

        static NumberResult fail(Error error) {
            return new NumberResult(error, 0);
        }

        public boolean isValid() {
            return error == Error.NONE;
        }

        public int intValue() {
            return (int) value;
        }
    }

    private ValidationUtils() {
    }

    // date must be filled in, must be a real date and can't be after today
    public static Error validateDate(String iso, String todayIso) {
        if (iso == null || iso.trim().isEmpty()) {
            return Error.REQUIRED;
        }
        if (!DateUtils.isValidIso(iso)) {
            return Error.INVALID_DATE;
        }
        if (DateUtils.isAfter(iso, todayIso)) {
            return Error.FUTURE_DATE;
        }
        return Error.NONE;
    }

    // whole number from 0 to max
    public static NumberResult parseWholeNumber(String raw, int max) {
        return parseWholeNumber(raw, 0, max);
    }

    // whole number from min to max
    public static NumberResult parseWholeNumber(String raw, int min, int max) {
        String text = raw == null ? "" : raw.trim();
        if (text.isEmpty()) {
            return NumberResult.fail(Error.REQUIRED);
        }
        if (NEGATIVE_NUMBER.matcher(text).matches()) {
            return NumberResult.fail(Error.NEGATIVE);
        }
        if (HAS_DECIMAL_PART.matcher(text).matches()) {
            return NumberResult.fail(Error.NOT_WHOLE_NUMBER);
        }
        if (!WHOLE_NUMBER.matcher(text).matches()) {
            return NumberResult.fail(Error.INVALID_NUMBER);
        }
        // remove zeros at the start ("007" -> "7")
        String digits = text.replaceFirst("^0+(?=\\d)", "");
        if (digits.length() > MAX_DIGITS) {
            return NumberResult.fail(Error.TOO_LARGE);
        }
        long value = Long.parseLong(digits);
        if (value > max) {
            return NumberResult.fail(Error.TOO_LARGE);
        }
        if (value < min) {
            return NumberResult.fail(Error.TOO_SMALL);
        }
        return NumberResult.ok(value);
    }

    // ---------------- Sleep (hours + minutes) ----------------

    // hours box: required, 0 - 24
    public static NumberResult parseSleepHoursPart(String raw) {
        return parseWholeNumber(raw, 0, MAX_SLEEP_HOURS);
    }

    // minutes box: can be left empty (= 0), otherwise 0 - 59
    public static NumberResult parseSleepMinutesPart(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return NumberResult.ok(0);
        }
        return parseWholeNumber(raw, 0, MAX_SLEEP_MINUTES_PART);
    }

    // total sleep can't be more than 24 hours (24h 30m is wrong)
    public static Error validateSleepTotal(int hours, int minutes) {
        return hours * 60 + minutes > MAX_SLEEP_TOTAL_MINUTES ? Error.SLEEP_OVER_24H : Error.NONE;
    }

    // sleep goal on Settings: 1 - 24 hours, decimals ok ("7.5")
    public static NumberResult parseSleepGoal(String raw) {
        String text = raw == null ? "" : raw.trim().replace(',', '.');
        if (text.isEmpty()) {
            return NumberResult.fail(Error.REQUIRED);
        }
        if (NEGATIVE_NUMBER.matcher(text).matches()) {
            return NumberResult.fail(Error.NEGATIVE);
        }
        if (!DECIMAL_NUMBER.matcher(text).matches()) {
            if (WHOLE_NUMBER.matcher(text).matches()) {
                return NumberResult.fail(Error.OUT_OF_RANGE); // like "125"
            }
            return NumberResult.fail(Error.INVALID_NUMBER); // like "abc" or "7.5.1"
        }
        double value = Double.parseDouble(text);
        if (value < MIN_GOAL_SLEEP || value > MAX_GOAL_SLEEP) {
            return NumberResult.fail(Error.OUT_OF_RANGE);
        }
        return NumberResult.ok(value);
    }

    // ---------------- Login / create account ----------------

    // username: 3 - 20 letters, numbers or _
    public static Error validateUsername(String raw) {
        String name = raw == null ? "" : raw.trim();
        if (name.isEmpty()) {
            return Error.REQUIRED;
        }
        if (name.length() < MIN_USERNAME || name.length() > MAX_USERNAME
                || !USERNAME.matcher(name).matches()) {
            return Error.USERNAME_INVALID;
        }
        return Error.NONE;
    }

    // password: at least 6 characters
    public static Error validatePassword(String password) {
        if (password == null || password.isEmpty()) {
            return Error.REQUIRED;
        }
        return password.length() < MIN_PASSWORD ? Error.PASSWORD_TOO_SHORT : Error.NONE;
    }

    // "confirm password" must be the same as the password
    public static Error validatePasswordConfirm(String password, String confirm) {
        if (confirm == null || confirm.isEmpty()) {
            return Error.REQUIRED;
        }
        return confirm.equals(password) ? Error.NONE : Error.PASSWORDS_DONT_MATCH;
    }
}