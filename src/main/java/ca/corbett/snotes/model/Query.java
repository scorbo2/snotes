package ca.corbett.snotes.model;

import ca.corbett.snotes.model.filter.Filter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * A Query is a collection of zero or more Filter instances that can be applied
 * to a list of Notes to produce a filtered list.
 * <p>
 * Note that all Filters in the chain are "and"ed together. There
 * is currently no way to "or" filters together, or to have more complex logic like
 * "filter A and (filter B or filter C)".
 * </p>
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
public class Query {

    public static final String DEFAULT_NAME = "Unnamed Query";
    public static final int NAME_LENGTH_LIMIT = 25;

    private String name;
    private final List<Filter> filters;
    private File sourceFile;
    private boolean isDirty;

    /**
     * Creates an empty, unnamed Query with no filters.
     * In this configuration, the Query will always return a completely unfiltered list of Notes when applied.
     * Use addFilter() to add filters to this Query and make it more selective.
     */
    public Query() {
        this.filters = new ArrayList<>();
        name = DEFAULT_NAME;
        sourceFile = null;
        isDirty = true;
    }

    /**
     * Returns the name of this Query, or DEFAULT_NAME if no name has been set.
     */
    public String getName() {
        return name;
    }

    /**
     * Sets an optional name for this Query. It should be short and user-presentable.
     * If longer than NAME_LENGTH_LIMIT, the name will be truncated to that length.
     * If null or blank, the name will be set to DEFAULT_NAME.
     */
    public Query setName(String name) {
        if (name == null || name.isBlank()) {
            name = DEFAULT_NAME;
        }
        if (name.length() > NAME_LENGTH_LIMIT) {
            name = name.substring(0, NAME_LENGTH_LIMIT);
        }
        this.name = name;
        isDirty = true;
        return this;
    }

    /**
     * Returns the File from which this Query was loaded, or null if this Query has not yet been saved to disk.
     */
    public File getSourceFile() {
        return sourceFile;
    }

    /**
     * Sets the source File for this Query. Replaces any previous value.
     * This should generally only be called from DataManager - calling it directly may
     * result in the old file being left on disk.
     *
     * @param sourceFile The new sourceFile for this Query.
     */
    public void setSourceFile(File sourceFile) {
        this.sourceFile = sourceFile;
        isDirty = true;
    }

    /**
     * Returns true if there are no Filters in this Query.
     */
    public boolean isEmpty() {
        return filters.isEmpty();
    }

    /**
     * Returns a count of Filters in this Query.
     */
    public int size() {
        return filters.size();
    }

    /**
     * Removes all Filters from this Query, and returns it to an empty state.
     * An empty Query will return a completely unfiltered list of Notes when applied.
     */
    public void clear() {
        if (!filters.isEmpty()) {
            isDirty = true;
        }
        filters.clear();
    }

    /**
     * Adds a Filter to this Query. Filters are applied in the order they are added.
     * Note that there is no code here to check that the given Filters make sense
     * and don't conflict with one another. For example, you could add a DateFilter
     * of "before 2010" and another DateFilter of "after 2015" to the same Query.
     * This is technically valid, but will filter out all Notes.
     */
    public Query addFilter(Filter filter) {
        if (filter == null) {
            throw new IllegalArgumentException("Cannot add a null Filter to a Query.");
        }
        filters.add(filter);
        isDirty = true;
        return this;
    }

    /**
     * Removes the given Filter from this Query, if it is present.
     */
    public Query removeFilter(Filter filter) {
        if (filters.remove(filter)) {
            isDirty = true;
        }
        return this;
    }

    /**
     * Returns a copy of the list of Filters in this Query.
     */
    public List<Filter> getFilters() {
        return new ArrayList<>(filters); // Return a copy to prevent external modification
    }

    /**
     * Applies our chain of filters to the provided list of Notes, returning a new list that only contains
     * Notes that were not filtered out by any of the filters in this Query.
     * The resulting list may be empty if the filters are too strict, but it will never be null.
     * The returned list is ordered by date, with most recent items last.
     *
     * @param notes The list of Notes to filter. This list is not modified by this method.
     * @return A new list of Notes that passed through all the filters in this Query. May be empty, but never null.
     */
    public List<Note> execute(List<Note> notes) {
        if (notes == null) {
            return new ArrayList<>();
        }
        List<Note> filteredNotes = new ArrayList<>();
        for (Note note : notes) {
            boolean isFiltered = false;
            for (Filter filter : filters) {
                if (filter.isFiltered(note)) {
                    isFiltered = true;
                    break; // No need to check other filters if one already filters this note
                }
            }
            if (!isFiltered) {
                filteredNotes.add(note);
            }
        }

        // Sort the list by date. For Notes that are dated, we'll use that date. For Notes that are
        // undated, we'll use the sourceFile's lastModified time. Undated Notes that have no
        // source file will be treated as having a date of 0 (the epoch), so they will be sorted before all dated Notes.
        filteredNotes.sort(Note::compareTo);

        return filteredNotes;
    }

    /**
     * Reports whether this Query has unsaved changes.
     */
    public boolean isDirty() {
        return isDirty;
    }

    /**
     * Marks this Query as clean, meaning that it has no unsaved changes.
     */
    public void markClean() {
        isDirty = false;
    }

    /**
     * We override this so that Query instances can be displayed in a JList or JComboBox
     * with a user-friendly display.
     *
     * @return The user-presentable name of this Query.
     */
    @Override
    public String toString() {
        return name;
    }
}
