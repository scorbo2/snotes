package ca.corbett.snotes.model.filter;

import ca.corbett.snotes.model.Note;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TextFilterTest extends FilterTest {

    @Test
    public void isFiltered_caseInsensitive_shouldMatchAnyCase() {
        // GIVEN our target text in various cases:
        String targetTextUpper = TEXT_TO_FIND.toUpperCase(Locale.ROOT);
        String targetTextLower = TEXT_TO_FIND.toLowerCase(Locale.ROOT);
        String targetTextExact = TEXT_TO_FIND;

        // WHEN we try to filter it:
        boolean actualUpper = new TextFilter(targetTextUpper).isFiltered(NOTE_WITH_TEXT_TO_FIND);
        boolean actualLower = new TextFilter(targetTextLower).isFiltered(NOTE_WITH_TEXT_TO_FIND);
        boolean actualExact = new TextFilter(targetTextExact).isFiltered(NOTE_WITH_TEXT_TO_FIND);

        // THEN none of them should be filtered, because we are doing case-insensitive matching by default:
        assertFalse(actualUpper);
        assertFalse(actualLower);
        assertFalse(actualExact);
    }

    @Test
    public void isFiltered_caseSensitive_shouldOnlyMatchExact() {
        // GIVEN our target text in various cases:
        String targetTextUpper = TEXT_TO_FIND.toUpperCase(Locale.ROOT);
        String targetTextLower = TEXT_TO_FIND.toLowerCase(Locale.ROOT);
        String targetTextExact = TEXT_TO_FIND;

        // WHEN we try to filter it:
        boolean actualUpper = new TextFilter(targetTextUpper, true).isFiltered(NOTE_WITH_TEXT_TO_FIND);
        boolean actualLower = new TextFilter(targetTextLower, true).isFiltered(NOTE_WITH_TEXT_TO_FIND);
        boolean actualExact = new TextFilter(targetTextExact, true).isFiltered(NOTE_WITH_TEXT_TO_FIND);

        // THEN only the exact match should survive, both others should get filtered:
        assertTrue(actualUpper);
        assertTrue(actualLower);
        assertFalse(actualExact);
    }

    @Test
    public void isFiltered_withTextNotPresent_shouldFilter() {
        // GIVEN a TextFilter with text that is not present in the Note:
        TextFilter filter = new TextFilter("This text does not appear in the note.");

        // WHEN we try to filter the note:
        boolean actual = filter.isFiltered(NOTE_WITH_TEXT_TO_FIND);

        // THEN it should be filtered:
        assertTrue(actual);
    }

    @Test
    public void isFiltered_withNoteWithNoText_shouldFilter() {
        // GIVEN a Note that has no text at all:
        Note noteWithNoText = NOTE_NO_TEXT;

        // WHEN we try to filter it with any TextFilter:
        TextFilter filter = new TextFilter(TEXT_TO_FIND);

        // THEN it should not be filtered, because no text means no match:
        assertTrue(filter.isFiltered(noteWithNoText));
    }

    @Test
    public void isFiltered_withNullNote_shouldFilter() {
        // GIVEN a null Note:
        Note nullNote = null;

        // WHEN we try to filter it with any TextFilter:
        TextFilter filter = new TextFilter(TEXT_TO_FIND);

        // THEN it should not be filtered, because null input gets filtered automatically:
        assertTrue(filter.isFiltered(nullNote));
    }
}