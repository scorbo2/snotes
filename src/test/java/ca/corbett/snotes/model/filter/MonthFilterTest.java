package ca.corbett.snotes.model.filter;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MonthFilterTest extends FilterTest {

    @Test
    public void constructor_withNullInput_shouldThrow() {
        // WHEN we give garbage to the constructor, THEN it should immediately throw:
        assertThrows(IllegalArgumentException.class, () -> new MonthFilter(13, MonthFilter.FilterType.IS));
        assertThrows(IllegalArgumentException.class, () -> new MonthFilter(0, MonthFilter.FilterType.IS));
        assertThrows(IllegalArgumentException.class, () -> new MonthFilter(1, null));
    }

    @Test
    public void isFiltered_withUndatedNote_shouldFilter() {
        // WHEN we use an undated Note, THEN it should automatically be filtered:
        assertTrue(new MonthFilter(1, MonthFilter.FilterType.IS).isFiltered(NOTE_UNDATED_UNTAGGED));
    }

    @Test
    public void isFiltered_ISwithMatchingMonth_shouldNotFilter() {
        // GIVEN a MonthFilter with IS type and a target month that matches our test note:
        MonthFilter filter = new MonthFilter(1, MonthFilter.FilterType.IS);

        // WHEN we try to filter the note:
        boolean actual = filter.isFiltered(NOTE_JAN_1_2020);

        // THEN it should not be filtered, because the Note's month matches the target month:
        assertFalse(actual);
    }

    @Test
    public void isFiltered_ISwithNonMatchingMonth_shouldFilter() {
        // GIVEN a MonthFilter with IS type and a target month that does not match our test note:
        MonthFilter filter = new MonthFilter(2, MonthFilter.FilterType.IS);

        // WHEN we try to filter the note:
        boolean actual = filter.isFiltered(NOTE_JAN_1_2020);

        // THEN it should be filtered, because the Note's month does not match the target month:
        assertTrue(actual);
    }

    @Test
    public void isFiltered_IS_NOTwithMatchingMonth_shouldFilter() {
        // GIVEN a MonthFilter with IS_NOT type and a target month that
        // matches our test note:
        MonthFilter filter = new MonthFilter(1, MonthFilter.FilterType.IS_NOT);

        // WHEN we try to filter the note:
        boolean actual = filter.isFiltered(NOTE_JAN_1_2020);

        // THEN it should be filtered, because it matches, and we're in IS_NOT mode:
        assertTrue(actual);
    }

    @Test
    public void isFiltered_IS_NOTwithNonMatchingMonth_shouldNotFilter() {
        // GIVEN a MonthFilter with IS_NOT type and a target month that
        // does not match our test note:
        MonthFilter filter = new MonthFilter(2, MonthFilter.FilterType.IS_NOT);

        // WHEN we try to filter the note:
        boolean actual = filter.isFiltered(NOTE_JAN_1_2020);

        // THEN it should not be filtered, because it does not match, and we're in IS_NOT mode:
        assertFalse(actual);
    }
}