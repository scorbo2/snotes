package ca.corbett.snotes.model;

import ca.corbett.snotes.model.filter.DateFilter;
import ca.corbett.snotes.model.filter.DayOfMonthFilter;
import ca.corbett.snotes.model.filter.MonthFilter;
import ca.corbett.snotes.model.filter.TagFilter;
import ca.corbett.snotes.model.filter.TextFilter;

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
 */
public class QueryFactory {

    private QueryFactory() {
    }

    /**
     * Returns a Query that looks for Notes between two dates, inclusive.
     */
    public static Query between(YMDDate startDate, YMDDate endDate) {
        return new Query()
            .addFilter(new DateFilter(startDate, DateFilter.FilterType.AFTER_INCLUSIVE))
            .addFilter(new DateFilter(endDate, DateFilter.FilterType.BEFORE_INCLUSIVE));
    }

    /**
     * Returns a Query that looks for Notes between two dates, inclusive, and with a specific tag.
     */
    public static Query between(YMDDate startDate, YMDDate endDate, String tag) {
        return new Query()
            .addFilter(new DateFilter(startDate, DateFilter.FilterType.AFTER_INCLUSIVE))
            .addFilter(new DateFilter(endDate, DateFilter.FilterType.BEFORE_INCLUSIVE))
            .addFilter(new TagFilter(List.of(new Tag(tag)), TagFilter.FilterType.ALL));
    }

    /**
     * Returns a Query that looks for Notes on a specific date in any year.
     * For example: specificDateAnyYear(12, 25) would return all Xmas Notes from any year.
     */
    public static Query specificDateAnyYear(int month, int day) {
        return new Query()
            .addFilter(new MonthFilter(month, MonthFilter.FilterType.IS))
            .addFilter(new DayOfMonthFilter(day, DayOfMonthFilter.FilterType.IS));
    }

    /**
     * Returns a very simple Query that returns all Notes that contain the given text, case-insensitive.
     */
    public static Query contains(String text) {
        return new Query().addFilter(new TextFilter(text, false));
    }
}
