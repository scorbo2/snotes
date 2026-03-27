package ca.corbett.snotes.model.filter;

import ca.corbett.snotes.model.Note;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Locale;

/**
 * This Filter can be used to filter Notes by their month, regardless of year.
 * For example, "I want to see all notes that were written in March, in any year".
 * This can be combined with a DayOfMonthFilter and/or a DayOfWeekFilter to
 * say something like "show me all Notes from December in any year, if it was the 25th and a Wednesday".
 * <p>
 * <B>NOTE:</B> all date filters automatically filter out undated notes.
 * Applying any type of date filter will automatically exclude all notes that do not have a date,
 * or whose date is null.
 * </p>
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
public class MonthFilter extends Filter {

    private final int targetMonth;
    private final BooleanFilterType filterType;

    @JsonCreator
    public MonthFilter(@JsonProperty("targetMonth") int targetMonth,
                       @JsonProperty("filterType") BooleanFilterType filterType) {
        if (targetMonth < 1 || targetMonth > 12) {
            throw new IllegalArgumentException("targetMonth must be between 1 and 12");
        }
        if (filterType == null) {
            throw new IllegalArgumentException("filterType cannot be null");
        }
        this.targetMonth = targetMonth;
        this.filterType = filterType;
    }

    public int getTargetMonth() {
        return targetMonth;
    }

    public BooleanFilterType getFilterType() {
        return filterType;
    }

    @Override
    public String getDescription() {
        return "Filter by month in any year";
    }

    @Override
    public boolean isFiltered(Note note) {
        if (note == null || !note.hasDate() || note.getDate() == null) {
            // All date filters automatically filter out non-dated notes.
            return true;
        }
        return filterType == BooleanFilterType.IS ?
            note.getDate().getMonth() != targetMonth
            : note.getDate().getMonth() == targetMonth;
    }

    @Override
    public String toString() {
        return "Month " + filterType.toString().toLowerCase(Locale.ROOT) + " " + targetMonth;
    }
}
