package util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Display formatting shared by every screen. */
public final class Format {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US);
    private static final DateTimeFormatter SHORT_DATE = DateTimeFormatter.ofPattern("MMM d", Locale.US);
    private static final DateTimeFormatter MONTH_YEAR = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("h:mm a", Locale.US);
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("MMM d, yyyy 'at' h:mm a", Locale.US);

    private Format() {
    }

    public static String money(double amount) {
        if (Math.abs(amount - Math.rint(amount)) < 0.005) {
            return String.format(Locale.US, "$%,.0f", amount);
        }
        return String.format(Locale.US, "$%,.2f", amount);
    }

    public static String date(LocalDate date) {
        return date == null ? "—" : DATE.format(date);
    }

    public static String dateTime(LocalDateTime dateTime) {
        return dateTime == null ? "—" : DATE_TIME.format(dateTime);
    }

    /** "Dec 6", or "Dec 6, 2027" when the date isn't in the current year. */
    public static String shortDate(LocalDate date) {
        if (date == null) {
            return "—";
        }
        return date.getYear() == LocalDate.now().getYear() ? SHORT_DATE.format(date) : DATE.format(date);
    }

    /** "Feb 4 – Mar 21, 2027": a year only where it isn't the current one. */
    public static String dateRange(LocalDate from, LocalDate until) {
        if (from == null || until == null) {
            return shortDate(from) + " – " + shortDate(until);
        }
        int thisYear = LocalDate.now().getYear();
        String start = SHORT_DATE.format(from);
        String end = SHORT_DATE.format(until);
        if (from.getYear() != until.getYear() && from.getYear() != thisYear) {
            start += ", " + from.getYear();
        }
        if (until.getYear() != thisYear || from.getYear() != until.getYear()) {
            end += ", " + until.getYear();
        }
        return start + " – " + end;
    }

    public static String monthYear(LocalDate date) {
        return date == null ? "—" : MONTH_YEAR.format(date);
    }

    public static String time(LocalDateTime dateTime) {
        return dateTime == null ? "" : TIME.format(dateTime);
    }

    /** A compact time for message lists: "4:05 PM" today, "Yesterday", or a date. */
    public static String relative(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        LocalDate day = dateTime.toLocalDate();
        LocalDate today = LocalDate.now();
        if (day.equals(today)) {
            return TIME.format(dateTime);
        }
        if (day.equals(today.minusDays(1))) {
            return "Yesterday";
        }
        return shortDate(day);
    }

    public static String plural(int count, String word) {
        return count + " " + word + (count == 1 ? "" : "s");
    }
}
