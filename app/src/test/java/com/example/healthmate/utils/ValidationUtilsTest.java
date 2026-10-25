package com.example.healthmate.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/*
 * Test cases 1 and 2: good input is accepted, wrong input is rejected.
 */
public class ValidationUtilsTest {

    private static final String TODAY = "2026-10-18";

    // ---------- Good values (Test case 1) ----------

    @Test
    public void validRecordValues_areAccepted() {
        assertTrue(ValidationUtils.parseWholeNumber("7850", ValidationUtils.MAX_STEPS).isValid());
        assertTrue(ValidationUtils.parseWholeNumber("420", ValidationUtils.MAX_CALORIES).isValid());
        assertTrue(ValidationUtils.parseWholeNumber("2000", ValidationUtils.MAX_WATER_ML).isValid());
        assertTrue(ValidationUtils.parseSleepHoursPart("7").isValid());
        assertTrue(ValidationUtils.parseSleepMinutesPart("30").isValid());
        assertEquals(ValidationUtils.Error.NONE, ValidationUtils.validateDate(TODAY, TODAY));
    }

    @Test
    public void zeroValues_areAccepted() {
        assertEquals(0, ValidationUtils.parseWholeNumber("0", ValidationUtils.MAX_STEPS).intValue());
        assertEquals(0, ValidationUtils.parseSleepHoursPart("0").intValue());
        assertEquals(0, ValidationUtils.parseSleepMinutesPart("").intValue()); // empty minutes = 0
    }

    @Test
    public void parsedValues_areCorrect() {
        assertEquals(7850, ValidationUtils.parseWholeNumber(" 7850 ", ValidationUtils.MAX_STEPS).intValue());
        assertEquals(45, ValidationUtils.parseSleepMinutesPart(" 45 ").intValue());
        assertEquals(42, ValidationUtils.parseWholeNumber("0042", ValidationUtils.MAX_STEPS).intValue());
    }

    @Test
    public void upperLimits_areInclusive() {
        assertTrue(ValidationUtils.parseWholeNumber("100000", ValidationUtils.MAX_STEPS).isValid());
        assertTrue(ValidationUtils.parseSleepHoursPart("24").isValid());
        assertTrue(ValidationUtils.parseSleepMinutesPart("59").isValid());
        assertEquals(ValidationUtils.Error.NONE, ValidationUtils.validateSleepTotal(24, 0));
    }

    // ---------- Wrong values (Test case 2) ----------

    @Test
    public void emptyInput_isRequired() {
        assertEquals(ValidationUtils.Error.REQUIRED, ValidationUtils.parseWholeNumber("", 100).error);
        assertEquals(ValidationUtils.Error.REQUIRED, ValidationUtils.parseWholeNumber("   ", 100).error);
        assertEquals(ValidationUtils.Error.REQUIRED, ValidationUtils.parseWholeNumber(null, 100).error);
        assertEquals(ValidationUtils.Error.REQUIRED, ValidationUtils.parseSleepHoursPart("").error);
    }

    @Test
    public void negativeNumbers_areRejected() {
        assertEquals(ValidationUtils.Error.NEGATIVE,
                ValidationUtils.parseWholeNumber("-5", ValidationUtils.MAX_STEPS).error);
        assertEquals(ValidationUtils.Error.NEGATIVE, ValidationUtils.parseSleepHoursPart("-1").error);
    }

    @Test
    public void decimals_areRejectedForWholeNumbers() {
        assertEquals(ValidationUtils.Error.NOT_WHOLE_NUMBER,
                ValidationUtils.parseWholeNumber("12.5", ValidationUtils.MAX_STEPS).error);
    }

    @Test
    public void text_isRejected() {
        assertEquals(ValidationUtils.Error.INVALID_NUMBER,
                ValidationUtils.parseWholeNumber("abc", ValidationUtils.MAX_STEPS).error);
        assertEquals(ValidationUtils.Error.INVALID_NUMBER, ValidationUtils.parseSleepHoursPart("seven").error);
        assertEquals(ValidationUtils.Error.NOT_WHOLE_NUMBER, ValidationUtils.parseSleepHoursPart("7.5").error);
        assertEquals(ValidationUtils.Error.INVALID_NUMBER, ValidationUtils.parseSleepMinutesPart("NaN").error);
    }

    @Test
    public void tooLargeValues_areRejected() {
        assertEquals(ValidationUtils.Error.TOO_LARGE,
                ValidationUtils.parseWholeNumber("100001", ValidationUtils.MAX_STEPS).error);
        assertEquals(ValidationUtils.Error.TOO_LARGE,
                ValidationUtils.parseWholeNumber("20001", ValidationUtils.MAX_WATER_ML).error);
    }

    @Test
    public void hugeNumbers_doNotOverflow() {
        // bigger than int and long can hold - the app must not crash
        assertEquals(ValidationUtils.Error.TOO_LARGE,
                ValidationUtils.parseWholeNumber("99999999999999999999999", ValidationUtils.MAX_STEPS).error);
        assertEquals(ValidationUtils.Error.TOO_LARGE,
                ValidationUtils.parseWholeNumber("2147483648", ValidationUtils.MAX_STEPS).error);
    }

    @Test
    public void sleepOutOfRange_isRejected() {
        assertEquals(ValidationUtils.Error.TOO_LARGE, ValidationUtils.parseSleepHoursPart("25").error);
        assertEquals(ValidationUtils.Error.TOO_LARGE, ValidationUtils.parseSleepMinutesPart("60").error);
        // 24 hours 30 minutes is more than one day
        assertEquals(ValidationUtils.Error.SLEEP_OVER_24H, ValidationUtils.validateSleepTotal(24, 30));
        assertEquals(ValidationUtils.Error.NONE, ValidationUtils.validateSleepTotal(23, 59));
    }

    @Test
    public void dates_areValidated() {
        assertEquals(ValidationUtils.Error.REQUIRED, ValidationUtils.validateDate(null, TODAY));
        assertEquals(ValidationUtils.Error.REQUIRED, ValidationUtils.validateDate("", TODAY));
        assertEquals(ValidationUtils.Error.INVALID_DATE, ValidationUtils.validateDate("18/10/2026", TODAY));
        assertEquals(ValidationUtils.Error.INVALID_DATE, ValidationUtils.validateDate("2026-02-30", TODAY));
        assertEquals(ValidationUtils.Error.FUTURE_DATE, ValidationUtils.validateDate("2026-10-19", TODAY));
        assertEquals(ValidationUtils.Error.NONE, ValidationUtils.validateDate("2026-10-17", TODAY));
    }

    @Test
    public void goalLimits_useMinimum() {
        assertEquals(ValidationUtils.Error.TOO_SMALL, ValidationUtils.parseWholeNumber("0",
                ValidationUtils.MIN_GOAL_STEPS, ValidationUtils.MAX_GOAL_STEPS).error);
        assertEquals(ValidationUtils.Error.OUT_OF_RANGE, ValidationUtils.parseSleepGoal("0.5").error);
        assertTrue(ValidationUtils.parseSleepGoal("8").isValid());
        assertFalse(ValidationUtils.parseSleepGoal("30").isValid());
    }

    // ---------- Login screen ----------

    @Test
    public void username_rules() {
        assertEquals(ValidationUtils.Error.NONE, ValidationUtils.validateUsername("fathima_01"));
        assertEquals(ValidationUtils.Error.NONE, ValidationUtils.validateUsername("  abc  ")); // spaces trimmed
        assertEquals(ValidationUtils.Error.REQUIRED, ValidationUtils.validateUsername(""));
        assertEquals(ValidationUtils.Error.USERNAME_INVALID, ValidationUtils.validateUsername("ab"));
        assertEquals(ValidationUtils.Error.USERNAME_INVALID, ValidationUtils.validateUsername("my name"));
        assertEquals(ValidationUtils.Error.USERNAME_INVALID, ValidationUtils.validateUsername("a".repeat(21)));
    }

    @Test
    public void password_rules() {
        assertEquals(ValidationUtils.Error.NONE, ValidationUtils.validatePassword("secret"));
        assertEquals(ValidationUtils.Error.REQUIRED, ValidationUtils.validatePassword(""));
        assertEquals(ValidationUtils.Error.PASSWORD_TOO_SHORT, ValidationUtils.validatePassword("12345"));
        assertEquals(ValidationUtils.Error.NONE, ValidationUtils.validatePasswordConfirm("secret", "secret"));
        assertEquals(ValidationUtils.Error.PASSWORDS_DONT_MATCH,
                ValidationUtils.validatePasswordConfirm("secret", "Secret"));
        assertEquals(ValidationUtils.Error.REQUIRED, ValidationUtils.validatePasswordConfirm("secret", ""));
    }
}