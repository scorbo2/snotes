package ca.corbett.snotes.model;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.logging.Logger;

/**
 * A simple wrapper around dates in a hard-coded yyyy-MM-dd format.
 * All date values in the entire application are represented in this
 * format, localization be damned. YMDDate instances are immutable,
 * and always contain a valid date (defaulting to today's date if
 * given bad input).
 * <P>
 *     This is largely legacy stuff ported over from Snotes 1.0 and
 *     minorly updated to use LocalDate instead of Date, but it seems
 *     a little clunky and unnecessary. Might be worth revisiting later.
 * </P>
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 * @since Snotes 1.0
 */
public class YMDDate implements Comparable<YMDDate> {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final Logger log = Logger.getLogger(YMDDate.class.getName());
    private LocalDate date;

    /**
     * Constructs a YMDDate representing today's date.
     */
    public YMDDate() {
        date = LocalDate.now();
    }

    /**
     * Attempts to construct a YMDDate from the given String in yyyy-MM-dd format.
     * If the String is badly formatted, today's date will be used instead.
     */
    public YMDDate(String ymdString) {
        try {
            date = ymdString == null ? LocalDate.now() : LocalDate.parse(ymdString, FORMATTER);
        } catch (Exception e) {
            log.warning("Invalid date string '" + ymdString + "': " + e.getMessage() + ". Using today's date instead.");
            date = LocalDate.now();
        }
    }

    /**
     * Returns the day prior to this date.
     */
    public YMDDate getYesterday() {
        LocalDate yesterday = date.minusDays(1);
        return new YMDDate(yesterday.format(FORMATTER));
    }

    /**
     * Returns the day after this date.
     */
    public YMDDate getTomorrow() {
        LocalDate tomorrow = date.plusDays(1);
        return new YMDDate(tomorrow.format(FORMATTER));
    }

    /**
     * Returns the human-readable name of the day of the week for this date.
     * <p>
     *     I know I said "localization be damned" up there, but this is one place
     *     where it makes sense to localize the day name. This value is NOT used
     *     for tagging purposes, but only when displaying a read-only Snote, so
     *     localization is appropriate.
     * </p>
     */
    public String getDayName() {
        return date.getDayOfWeek().getDisplayName(TextStyle.FULL, java.util.Locale.getDefault());
    }

    /**
     * Returns the year in string format (four digits always).
     */
    public String getYearStr() {
        return String.format("%04d", date.getYear());
    }

    /**
     * Returns the month in string format (two digits always).
     */
    public String getMonthStr() {
        return String.format("%02d", date.getMonthValue());
    }

    /**
     * Returns the day of month in string format (two digits always).
     */
    public String getDayStr() {
        return String.format("%02d", date.getDayOfMonth());
    }

    /**
     * Returns a yyyy-MM-dd formatted String representation of this date.
     */
    @Override
    public String toString() {
        return date.format(FORMATTER);
    }

    /**
     * Reports whether the given String conforms to yyyy-MM-dd format.
     */
    public static boolean isValidYMD(String candidate) {
        try {
            LocalDate.parse(candidate, FORMATTER);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Compares this YMDDate to another.
     */
    @Override
    public int compareTo(YMDDate o) {
        if (o == null) {
            return 1;
        }
        return this.date.compareTo(o.date);
    }
}
