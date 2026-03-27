package ca.corbett.snotes.model.filter;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DayOfMonthFilterTest extends FilterTest {

    @Test
    public void constructor_withNullInput_shouldThrow() {
        // WHEN we give garbage to the constructor, THEN it should immediately throw:
        assertThrows(IllegalArgumentException.class, () -> new DayOfMonthFilter(33, BooleanFilterType.IS));
        assertThrows(IllegalArgumentException.class, () -> new DayOfMonthFilter(1, null));
        assertThrows(IllegalArgumentException.class, () -> new DayOfMonthFilter(-1, BooleanFilterType.IS));
        assertThrows(IllegalArgumentException.class, () -> new DayOfMonthFilter(0, BooleanFilterType.IS));
    }

    @Test
    public void isFiltered_withUndatedNote_shouldFilter() {
        // WHEN we use an undated Note, THEN it should automatically be filtered:
        assertTrue(new DayOfMonthFilter(1, BooleanFilterType.IS).isFiltered(NOTE_UNDATED_UNTAGGED));
    }

    @Test
    public void isFiltered_ISwithMatchingDayOfMonth_shouldNotFilter() {
        // GIVEN a DayOfMonthFilter with IS type and a target day of month that matches our test note:
        DayOfMonthFilter filter = new DayOfMonthFilter(1, BooleanFilterType.IS);

        // WHEN we try to filter the note:
        boolean actual = filter.isFiltered(NOTE_JAN_1_2020);

        // THEN it should not be filtered, because the Note's day of month matches the target day of month:
        assertFalse(actual);
    }

    @Test
    public void isFiltered_ISwithNonMatchingDayOfMonth_shouldFilter() {
        // GIVEN a DayOfMonthFilter with IS type and a target day of month that does not match our test note:
        DayOfMonthFilter filter = new DayOfMonthFilter(2, BooleanFilterType.IS);

        // WHEN we try to filter the note:
        boolean actual = filter.isFiltered(NOTE_JAN_1_2020);

        // THEN it should be filtered, because the Note's day of month does not match the target day of month:
        assertTrue(actual);
    }

    @Test
    public void isFiltered_IS_NOTwithMatchingDayOfMonth_shouldFilter() {
        // GIVEN a DayOfMonthFilter with IS_NOT type and a target day of month that
        // matches our test note:
        DayOfMonthFilter filter = new DayOfMonthFilter(1, BooleanFilterType.IS_NOT);

        // WHEN we try to filter the note:
        boolean actual = filter.isFiltered(NOTE_JAN_1_2020);

        // THEN it should be filtered, because it matches, and we're in IS_NOT mode:
        assertTrue(actual);
    }

    @Test
    public void isFiltered_IS_NOTwithNonMatchingDayOfMonth_shouldNotFilter() {
        // GIVEN a DayOfMonthFilter with IS_NOT type and a target day of month that
        // does not match our test note:
        DayOfMonthFilter filter = new DayOfMonthFilter(2, BooleanFilterType.IS_NOT);

        // WHEN we try to filter the note:
        boolean actual = filter.isFiltered(NOTE_JAN_1_2020);

        // THEN it should not be filtered, because it does not match, and we're in IS_NOT mode:
        assertFalse(actual);
    }
}