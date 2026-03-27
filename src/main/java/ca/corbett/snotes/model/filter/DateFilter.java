package ca.corbett.snotes.model.filter;

import ca.corbett.snotes.model.Note;
import ca.corbett.snotes.model.YMDDate;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * This Filter can be used to filter Notes by their calendar date.
 * The included FilterType enum can be used to specify the type of date comparison.
 * <p>
 * Note that the actual date comparisons are deferred to the YMDDate comparison methods.
 * </p>
 * <p>
 * <B>NOTE:</B> all date filters automatically filter out undated notes.
 * Applying any type of date filter will automatically exclude all notes that do not have a date,
 * or whose date is null.
 * </p>
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
public class DateFilter extends Filter {

    private final YMDDate targetDate;
    private final DateFilterType filterType;

    @JsonCreator
    public DateFilter(@JsonProperty("targetDate") YMDDate targetDate,
                      @JsonProperty("filterType") DateFilterType filterType) {
        if (targetDate == null || filterType == null) {
            throw new IllegalArgumentException("targetDate and filterType cannot be null");
        }
        this.targetDate = targetDate;
        this.filterType = filterType;
    }

    public YMDDate getTargetDate() {
        return targetDate;
    }

    public DateFilterType getFilterType() {
        return filterType;
    }

    @Override
    public String getDescription() {
        return "Filter by specific calendar date";
    }

    @Override
    public boolean isFiltered(Note note) {
        if (note == null || !note.hasDate() || note.getDate() == null) {
            // All date filters automatically filter out non-dated notes.
            return true;
        }
        int comparison = note.getDate().compareTo(targetDate);
        return switch (filterType) {
            case BEFORE_EXCLUSIVE -> comparison >= 0;
            case BEFORE_INCLUSIVE -> comparison > 0;
            case ON -> comparison != 0;
            case AFTER_INCLUSIVE -> comparison < 0;
            case AFTER_EXCLUSIVE -> comparison <= 0;
        };
    }

    /**
     * Returns a human-readable summary of this filter.
     */
    @Override
    public String toString() {
        return "Date is " + filterType.toString().toLowerCase() + " " + targetDate.toString();
    }
}
