package ca.corbett.snotes.model.filter;

import ca.corbett.snotes.model.Note;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * An abstract base class for all filters. Each filter has a human-readable description,
 * and a simple method to determine if a candidate Note should be filtered or not.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = DateFilter.class, name = "DateFilter"),
    @JsonSubTypes.Type(value = DayOfMonthFilter.class, name = "DayOfMonthFilter"),
    @JsonSubTypes.Type(value = DayOfWeekFilter.class, name = "DayOfWeekFilter"),
    @JsonSubTypes.Type(value = MonthFilter.class, name = "MonthFilter"),
    @JsonSubTypes.Type(value = TagFilter.class, name = "TagFilter"),
    @JsonSubTypes.Type(value = TextFilter.class, name = "TextFilter"),
    @JsonSubTypes.Type(value = UndatedFilter.class, name = "UndatedFilter"),
    @JsonSubTypes.Type(value = YearFilter.class, name = "YearFilter")
})
public abstract class Filter {

    /**
     * Returns a brief, human-presentable description of this filter.
     */
    @JsonIgnore
    public abstract String getDescription();

    /**
     * Return true if the given Note should be filtered (removed from the results).
     * Return false if the given Note should be included in the results.
     */
    public abstract boolean isFiltered(Note note);
}
