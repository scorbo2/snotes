package ca.corbett.snotes.model.filter;

import ca.corbett.snotes.model.Note;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UndatedFilterTest extends FilterTest {

    @Test
    public void isFiltered_withNullNote_shouldFilter() {
        // GIVEN a null Note:
        Note note = null;

        // WHEN we try to filter it:
        boolean actual = new UndatedFilter().isFiltered(note);

        // THEN it should be filtered:
        assertTrue(actual);
    }

    @Test
    public void isFiltered_withDatedNote_shouldFilter() {
        // GIVEN a Note with a date:
        Note note = NOTE_DATED_TAGGED;

        // WHEN we try to filter it:
        boolean actual = new UndatedFilter().isFiltered(note);

        // THEN it should be filtered:
        assertTrue(actual);
    }

    @Test
    public void isFiltered_withNoDate_shouldNotFilter() {
        // GIVEN a Note with no date:
        Note note = NOTE_UNDATED_UNTAGGED;

        // WHEN we try to filter it:
        boolean actual = new UndatedFilter().isFiltered(note);

        // THEN it should not be filtered:
        assertFalse(actual);
    }
}