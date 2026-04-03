package ca.corbett.snotes.model;

import ca.corbett.snotes.model.filter.BooleanFilterType;
import ca.corbett.snotes.model.filter.DateFilter;
import ca.corbett.snotes.model.filter.DateFilterType;
import ca.corbett.snotes.model.filter.DayOfMonthFilter;
import ca.corbett.snotes.model.filter.MonthFilter;
import ca.corbett.snotes.model.filter.TagFilter;
import ca.corbett.snotes.model.filter.TextFilter;
import ca.corbett.snotes.model.filter.YearFilter;

import java.util.List;

/**
 * A utility class with shorthand methods for creating common queries.
 * The returned Query instance is not immutable! You can use these
 * factory methods as a starting point, and add additional criteria if you want.
 * <p>
 * Dev note: the Query UI, once built, might make this entire class unnecessary.
 * Users will be able to visually build out whatever Query they need.
 * Once Query persistence is implemented, they can then save those queries for next time.
 * For now, these can be handy code shortcuts.
 * </p>
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 * @since Snotes 2.0
 */
public class QueryFactory {

    private QueryFactory() {
    }

    /**
     * Returns a Query that looks for Notes between two dates, inclusive.
     */
    public static Query between(YMDDate startDate, YMDDate endDate) {
        return new Query()
            .addFilter(new DateFilter(startDate, DateFilterType.AFTER_INCLUSIVE))
            .addFilter(new DateFilter(endDate, DateFilterType.BEFORE_INCLUSIVE));
    }

    /**
     * Returns a Query that looks for Notes between two dates, inclusive, and with a specific tag.
     */
    public static Query between(YMDDate startDate, YMDDate endDate, String tag) {
        return new Query()
            .addFilter(new DateFilter(startDate, DateFilterType.AFTER_INCLUSIVE))
            .addFilter(new DateFilter(endDate, DateFilterType.BEFORE_INCLUSIVE))
            .addFilter(new TagFilter(List.of(new Tag(tag)), TagFilter.FilterType.ALL));
    }

    /**
     * Returns a Query that looks for Notes on a specific date in any year.
     * For example: specificDateAnyYear(12, 25) would return all Xmas Notes from any year.
     */
    public static Query specificDateAnyYear(int month, int day) {
        return new Query()
            .addFilter(new MonthFilter(month, BooleanFilterType.IS))
            .addFilter(new DayOfMonthFilter(day, BooleanFilterType.IS));
    }

    /**
     * Returns a Query that looks for Notes written on xmas day in any year.
     */
    public static Query xmas() {
        return specificDateAnyYear(12, 25);
    }

    /**
     * Returns a Query that looks for Notes written on New Year's Eve in any year.
     */
    public static Query newYearsEve() {
        return specificDateAnyYear(12, 31);
    }

    /**
     * Returns a Query that looks for Notes written on any day in the given year.
     */
    public static Query year(int year) {
        return new Query().addFilter(new YearFilter(year, DateFilterType.ON));
    }

    /**
     * Returns a Query that looks for notes written in the given month (1-12) in any year.
     */
    public static Query month(int month) {
        return new Query().addFilter(new MonthFilter(month, BooleanFilterType.IS));
    }

    /**
     * Returns a Query that looks for notes written in the given month (1-12) of the given year.
     */
    public static Query month(int year, int month) {
        return new Query()
            .addFilter(new YearFilter(year, DateFilterType.ON))
            .addFilter(new MonthFilter(month, BooleanFilterType.IS));
    }

    /**
     * Returns a very simple Query that returns all Notes that contain the given text, case-insensitive.
     */
    public static Query contains(String text) {
        return new Query().addFilter(new TextFilter(text, false));
    }

    /**
     * Returns a very simple Query that returns all Notes that contain exactly the given text, case-sensitive.
     */
    public static Query containsExactly(String text) {
        return new Query().addFilter(new TextFilter(text, true));
    }
}
