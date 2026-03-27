package ca.corbett.snotes.model.filter;

import ca.corbett.snotes.model.Note;

/**
 * A Filter that specifically looks for undated Notes.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
public class UndatedFilter extends Filter {
    @Override
    public String getDescription() {
        return "Undated notes only";
    }

    @Override
    public boolean isFiltered(Note note) {
        // Filter out null notes, and anything that has a date.
        return note == null || note.hasDate() || note.getDate() != null;
    }

    @Override
    public String toString() {
        return getDescription();
    }
}
