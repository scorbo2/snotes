package ca.corbett.snotes.model.filter;

import ca.corbett.snotes.model.Note;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Locale;

/**
 * This filter can be used to filter by day of month, regardless which specific month or year the note is from.
 * For example, "I want to see all notes that were written on the 15th of any month, in any year".
 * This can be combined with MonthFilter to say "I want to see all notes from March 15th of any year".
 * <p>
 * <B>NOTE:</B> all date filters automatically filter out undated notes.
 * Applying any type of date filter will automatically exclude all notes that do not have a date,
 * or whose date is null.
 * </p>
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
public class DayOfMonthFilter extends Filter {

    private final int dayOfMonth;
    private final BooleanFilterType filterType;

    /**
     * Creates a DayOfMonthFilter for the given 1-based day of month (1-31) and filter type.
     */
    @JsonCreator
    public DayOfMonthFilter(@JsonProperty("dayOfMonth") int dayOfMonth,
                            @JsonProperty("filterType") BooleanFilterType filterType) {
        if (dayOfMonth < 1 || dayOfMonth > 31) {
            throw new IllegalArgumentException("dayOfMonth must be between 1 and 31");
        }
        if (filterType == null) {
            throw new IllegalArgumentException("filterType cannot be null");
        }
        this.dayOfMonth = dayOfMonth;
        this.filterType = filterType;
    }

    public int getDayOfMonth() {
        return dayOfMonth;
    }

    public BooleanFilterType getFilterType() {
        return filterType;
    }

    @Override
    public String getDescription() {
        return "Filter by day of month in any month/year";
    }

    @Override
    public boolean isFiltered(Note note) {
        if (note == null || !note.hasDate() || note.getDate() == null) {
            // All date filters automatically filter out non-dated notes.
            return true;
        }
        return filterType == BooleanFilterType.IS ?
            note.getDate().getDayOfMonth() != dayOfMonth
            : note.getDate().getDayOfMonth() == dayOfMonth;
    }

    @Override
    public String toString() {
        return "Day of month " + filterType.toString().toLowerCase(Locale.ROOT) + " " + dayOfMonth;
    }
}
