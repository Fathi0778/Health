package com.example.healthmate.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Pattern;

/*
 * Helpers for dates.
 * I save every date as text like "2026-10-18" (yyyy-MM-dd) because this text
 * sorts in the right order and is easy to search ("2026-10" = all of October).
 * No Android code here, so DateUtilsTest can test it.
 */
public final class DateUtils {

    public static final String ISO_PATTERN = "yyyy-MM-dd";
    private static final Pattern ISO_REGEX = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");
    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

    private DateUtils() {
    }

    private static SimpleDateFormat isoFormat(TimeZone zone) {
        SimpleDateFormat format = new SimpleDateFormat(ISO_PATTERN, Locale.US);
        format.setLenient(false);
        format.setTimeZone(zone);
        return format;
    }

    // today's date on this device, e.g. "2026-10-18".
    public static String today() {
        return isoFormat(TimeZone.getDefault()).format(new Date());
    }

    // true for a real calendar date in ISO format ("2026-02-30" is false).
    public static boolean isValidIso(String value) {
        if (value == null || !ISO_REGEX.matcher(value).matches()) {
            return false;
        }
        try {
            isoFormat(UTC).parse(value);
            return true;
        } catch (ParseException e) {
            return false;
        }
    }

    // ISO date strings compare correctly as text.
    public static boolean isAfter(String isoA, String isoB) {
        return isoA.compareTo(isoB) > 0;
    }

    // adds (or subtracts) whole days, e.g. addDays("2026-10-01", -1) = "2026-09-30".
    public static String addDays(String iso, int days) {
        Calendar calendar = toCalendarUtc(iso);
        calendar.add(Calendar.DAY_OF_MONTH, days);
        return isoFormat(UTC).format(calendar.getTime());
    }

    // the last count calendar days ending with endIso, oldest first.
    public static List<String> lastDays(String endIso, int count) {
        List<String> days = new ArrayList<>();
        for (int i = count - 1; i >= 0; i--) {
            days.add(addDays(endIso, -i));
        }
        return days;
    }

    // the Material date picker uses milliseconds (UTC midnight)
    public static long isoToUtcMillis(String iso) {
        return toCalendarUtc(iso).getTimeInMillis();
    }

    public static String utcMillisToIso(long utcMillis) {
        return isoFormat(UTC).format(new Date(utcMillis));
    }

    // "Sun, 18 Oct 2026"
    public static String formatLong(String iso) {
        return format(iso, "EEE, d MMM yyyy");
    }

    // "Sunday, 18 October 2026"
    public static String formatFull(String iso) {
        return format(iso, "EEEE, d MMMM yyyy");
    }

    // "18 Oct"
    public static String formatShort(String iso) {
        return format(iso, "d MMM");
    }

    // "Mon"
    public static String dayOfWeekShort(String iso) {
        return format(iso, "EEE");
    }

    // "18"
    public static String dayOfMonth(String iso) {
        return format(iso, "d");
    }

    // "OCT"
    public static String monthShortUpper(String iso) {
        return format(iso, "MMM").toUpperCase(Locale.getDefault());
    }

    // 0 = morning (before 12), 1 = afternoon (12-17), 2 = evening (17+).
    public static int partOfDay(int hourOfDay) {
        if (hourOfDay < 12) {
            return 0;
        } else if (hourOfDay < 17) {
            return 1;
        }
        return 2;
    }

    private static String format(String iso, String pattern) {
        SimpleDateFormat output = new SimpleDateFormat(pattern, Locale.getDefault());
        output.setTimeZone(UTC);
        return output.format(toCalendarUtc(iso).getTime());
    }

    private static Calendar toCalendarUtc(String iso) {
        try {
            Date date = isoFormat(UTC).parse(iso);
            Calendar calendar = Calendar.getInstance(UTC, Locale.US);
            calendar.setTime(date);
            return calendar;
        } catch (ParseException | NullPointerException e) {
            throw new IllegalArgumentException("Invalid ISO date: " + iso, e);
        }
    }
}