package ca.corbett.snotes.model.filter;

import ca.corbett.snotes.model.Note;

import java.time.DayOfWeek;

/**
 * This filter can be used to filter Notes based on the day of the week of their date, regardless
 * of specific year, month, or day of month. For example, "I want to see all notes that were written
 * on a Monday, in any month and year". This can be combined with a MonthFilter and/or a DayOfMonthFilter
 * to say something like "show me all Notes from December 25th in any year, if it was a Wednesday".
 * <p>
 * <B>NOTE:</B> all date filters automatically filter out undated notes.
 * Applying any type of date filter will automatically exclude all notes that do not have a date,
 * or whose date is null.
 * </p>
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
public class DayOfWeekFilter extends Filter {

    public enum FilterType {
        IS, IS_NOT
    }

    private final DayOfWeek dayOfWeek;
    private final FilterType filterType;

    public DayOfWeekFilter(DayOfWeek dayOfWeek, FilterType filterType) {
        if (dayOfWeek == null || filterType == null) {
            throw new IllegalArgumentException("dayOfWeek and filterType cannot be null");
        }
        this.dayOfWeek = dayOfWeek;
        this.filterType = filterType;
    }

    @Override
    public String getDescription() {
        return "Filter by day of week";
    }

    @Override
    public boolean isFiltered(Note note) {
        if (note == null || !note.hasDate() || note.getDate() == null) {
            // All date filters automatically filter out non-dated notes.
            return true;
        }
        return filterType == FilterType.IS ?
            note.getDate().getDayOfWeek() != dayOfWeek
            : note.getDate().getDayOfWeek() == dayOfWeek;
    }
}
