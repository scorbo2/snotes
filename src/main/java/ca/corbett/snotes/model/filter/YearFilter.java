package ca.corbett.snotes.model.filter;

import ca.corbett.snotes.model.Note;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * This Filter can be used to filter Notes by the year of their calendar date.
 * For more specific date filtering, use the DateFilter instead, or consider
 * combining this filter with a MonthFilter ("all notes from December of 2020").
 * If you find yourself combining a YearFilter with a MonthFilter and a DayOfMonthFilter,
 * then you should probably just use a single DateFilter instead.
 * <p>
 * <B>NOTE:</B> all date filters automatically filter out undated notes.
 * Applying any type of date filter will automatically exclude all notes that do not have a date,
 * or whose date is null.
 * </p>
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
public class YearFilter extends Filter {

    public enum FilterType {
        BEFORE_EXCLUSIVE, BEFORE_INCLUSIVE, ON, AFTER_INCLUSIVE, AFTER_EXCLUSIVE
    }

    private final int targetYear;
    private final FilterType filterType;

    /**
     * Creates a YearFilter for the given target year and filter type.
     * We don't validate targetYear - any integer is technically a valid year, even negative integers.
     * Just don't be surprised if Integer.MAX_VALUE doesn't yield many matches.
     */
    @JsonCreator
    public YearFilter(@JsonProperty("targetYear") int targetYear,
                       @JsonProperty("filterType") FilterType filterType) {
        if (filterType == null) {
            throw new IllegalArgumentException("filterType cannot be null");
        }
        this.targetYear = targetYear;
        this.filterType = filterType;
    }

    public int getTargetYear() {
        return targetYear;
    }

    public FilterType getFilterType() {
        return filterType;
    }

    @Override
    public String getDescription() {
        return "Filter by year";
    }

    @Override
    public boolean isFiltered(Note note) {
        if (note == null || !note.hasDate() || note.getDate() == null) {
            // All date filters automatically filter out non-dated notes.
            return true;
        }
        int noteYear = note.getDate().getYear();
        return switch (filterType) {
            case BEFORE_EXCLUSIVE -> noteYear >= targetYear;
            case BEFORE_INCLUSIVE -> noteYear > targetYear;
            case ON -> noteYear != targetYear;
            case AFTER_INCLUSIVE -> noteYear < targetYear;
            case AFTER_EXCLUSIVE -> noteYear <= targetYear;
        };
    }
}
