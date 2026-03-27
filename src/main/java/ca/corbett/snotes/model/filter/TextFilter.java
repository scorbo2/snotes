package ca.corbett.snotes.model.filter;

import ca.corbett.snotes.model.Note;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Locale;

/**
 * This Filter can be used to filter Notes by their text content.
 * If a Note does not contain the given text (with optional case-sensitivity), then it will be filtered out.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
public class TextFilter extends Filter {

    private final String contains;
    private final boolean caseSensitive;

    public TextFilter(String contains) {
        this(contains, false);
    }

    /**
     * Create a text filter with the given text to search for, with optional case-sensitivity.
     * The given text can be null or empty, but the resulting filter will effectively be a no-op.
     * <p>
     * The given text will NOT be trimmed - it's valid to look for something like "hello   " with
     * three trailing spaces. A value of null will be treated the same as an empty string (no-op).
     * </p>
     */
    @JsonCreator
    public TextFilter(@JsonProperty("contains") String contains,
                      @JsonProperty("caseSensitive") boolean caseSensitive) {
        this.contains = contains == null ? "" : contains;
        this.caseSensitive = caseSensitive;
    }

    public String getContains() {
        return contains;
    }

    public boolean isCaseSensitive() {
        return caseSensitive;
    }

    @Override
    public String getDescription() {
        return "Filter by text content";
    }

    @Override
    public boolean isFiltered(Note note) {
        // If the filter text is empty, this filter is a documented no-op.
        if (contains.isEmpty()) {
            return false;
        }
        // A null note or a note with null/blank text cannot contain the target text.
        if (note == null) {
            return true;
        }
        String noteText = note.getText();
        if (noteText == null || noteText.isBlank()) {
            return true;
        }
        String candidateText = noteText;
        String toFind = contains;
        if (!caseSensitive) {
            candidateText = candidateText.toLowerCase(Locale.ROOT);
            toFind = toFind.toLowerCase(Locale.ROOT);
        }
        return !candidateText.contains(toFind);
    }

    @Override
    public String toString() {
        return "Text contains " + (caseSensitive ? "(exactly) " : "") + "\"" + contains + "\"";
    }
}
