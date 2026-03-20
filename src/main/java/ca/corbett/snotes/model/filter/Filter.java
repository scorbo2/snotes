package ca.corbett.snotes.model.filter;

import ca.corbett.snotes.model.Note;

/**
 * An abstract base class for all filters. Each filter has a human-readable description,
 * and a simple method to determine if a candidate Note should be filtered or not.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
public abstract class Filter {

    /**
     * Returns a brief, human-presentable description of this filter.
     */
    public abstract String getDescription();

    /**
     * Return true if the given Note should be filtered (removed from the results).
     * Return false if the given Note should be included in the results.
     */
    public abstract boolean isFiltered(Note note);
}
