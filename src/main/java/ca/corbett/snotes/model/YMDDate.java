package ca.corbett.snotes.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.time.format.TextStyle;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * A simple wrapper around dates in a hard-coded yyyy-MM-dd format.
 * All date values are represented in this format.
 * <p>
 * YMDDate instances are immutable, and always contain a valid date,
 * defaulting to today's date if given bad input.
 * </p>
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 * @since Snotes 1.0
 */
public class YMDDate implements Comparable<YMDDate> {
    /**
     * Note: we have to use "uuuu" and ResolverStyle.STRICT here, because the default is a little too loose.
     * Without STRICT, the parser will accept dates that are technically invalid. Actual example: "2024-02-30".
     * Weirdly, using "yyyy" with ResolverStyle.STRICT rejects valid dates. Actual example: "2024-01-01".
     * Only the combination of both "uuuu" and STRICT seems to resolve dates correctly.
     */
    protected static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("uuuu-MM-dd")
                                                                          .withResolverStyle(ResolverStyle.STRICT);
    private static final Logger log = Logger.getLogger(YMDDate.class.getName());
    protected final LocalDate date;

    /**
     * Constructs a YMDDate representing today's date.
     */
    public YMDDate() {
        date = LocalDate.now();
    }

    /**
     * Attempts to construct a YMDDate from the given String in yyyy-MM-dd format.
     * If the String is badly formatted, a warning is logged, and today's date will be used instead.
     * <p>
     * <b>Note:</b> this constructor is intentionally lenient and is meant for general programmatic use.
     * JSON deserialization uses the strict {@link #fromJson(String)} factory method instead, which
     * will throw an exception on bad input rather than silently falling back to today's date.
     * </p>
     */
    public YMDDate(String ymdString) {
        LocalDate date;
        try {
            date = ymdString == null ? LocalDate.now() : LocalDate.parse(ymdString, FORMATTER);
        }
        catch (Exception e) {
            log.warning("Invalid date string '" + ymdString + "': " + e.getMessage() + ". Using today's date instead.");
            date = LocalDate.now();
        }
        this.date = date;
    }

    /**
     * Strict JSON factory method used by Jackson during deserialization.
     * Unlike the string constructor, this method throws {@link IllegalArgumentException}
     * if the given string is null or does not conform to yyyy-MM-dd format.
     * This ensures that malformed dates in saved query files surface as load errors rather than
     * silently being replaced with today's date.
     *
     * @param ymdString A yyyy-MM-dd formatted date string.
     * @return A valid YMDDate.
     * @throws IllegalArgumentException if ymdString is null or not a valid yyyy-MM-dd date.
     */
    @JsonCreator
    public static YMDDate fromJson(String ymdString) {
        if (!isValidYMD(ymdString)) {
            throw new IllegalArgumentException("Invalid or null date string for YMDDate: '" + ymdString + "'");
        }
        return new YMDDate(ymdString);
    }

    /**
     * For unit testing purposes, to make it easier to create test objects.
     */
    protected YMDDate(LocalDate date) {
        this.date = date;
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
     * This value is NOT used for tagging purposes, but can be used when displaying
     * a dated Note to the user.
     * </p>
     * <p>
     * Strongly suggest to use getDayOfWeek() instead, as it is not affected by locale.
     * </p>
     */
    public String getDayName() {
        return date.getDayOfWeek().getDisplayName(TextStyle.FULL, java.util.Locale.getDefault());
    }

    /**
     * Returns the DayOfWeek enum value for this date. This is not affected by locale.
     *
     * @return The DayOfWeek enum value for this date.
     */
    public DayOfWeek getDayOfWeek() {
        return date.getDayOfWeek();
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
     * Returns the year as an int.
     */
    public int getYear() {
        return date.getYear();
    }

    /**
     * Returns the 1-based month as an int.
     * Note that this is 1-based! January would be returned as "1", not "0".
     */
    public int getMonth() {
        return date.getMonthValue();
    }

    /**
     * Returns the 1-based day of month as an int.
     * Note that this is 1-based! The first day of the month would be returns as "1", not "0".
     */
    public int getDayOfMonth() {
        return date.getDayOfMonth();
    }

    /**
     * Returns a yyyy-MM-dd formatted String representation of this date.
     */
    @JsonValue
    @Override
    public String toString() {
        return date.format(FORMATTER);
    }

    /**
     * Returns true if this YMDDate is before the given YMDDate, false otherwise.
     */
    public boolean isBefore(YMDDate other) {
        if (other == null) {
            // Aligns with compareTo: this is not considered "before" a null date.
            return false;
        }
        return this.date.isBefore(other.date);
    }

    /**
     * Returns true if this YMDDate is after the given YMDDate, false otherwise.
     */
    public boolean isAfter(YMDDate other) {
        if (other == null) {
            // Aligns with compareTo: this is considered "after" a null date.
            return true;
        }
        return this.date.isAfter(other.date);
    }

    /**
     * Returns true if this YMDDate is the same as the given YMDDate, false otherwise.
     */
    public boolean isSameDate(YMDDate other) {
        if (other == null) {
            return false;
        }
        return this.date.isEqual(other.date);
    }

    /**
     * Reports whether the given String conforms to yyyy-MM-dd format.
     */
    public static boolean isValidYMD(String candidate) {
        try {
            LocalDate.parse(candidate, FORMATTER);
            return true;
        }
        catch (Exception e) {
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

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof YMDDate ymdDate)) { return false; }
        return Objects.equals(date, ymdDate.date);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(date);
    }
}
