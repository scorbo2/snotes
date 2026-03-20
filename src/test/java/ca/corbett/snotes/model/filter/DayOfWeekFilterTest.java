package ca.corbett.snotes.model.filter;

import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DayOfWeekFilterTest extends FilterTest {

    @Test
    public void constructor_withNullInput_shouldThrow() {
        // WHEN we give garbage to the constructor, THEN it should immediately throw:
        assertThrows(IllegalArgumentException.class, () -> new DayOfWeekFilter(null, DayOfWeekFilter.FilterType.IS));
        assertThrows(IllegalArgumentException.class, () -> new DayOfWeekFilter(DayOfWeek.MONDAY, null));
    }

    @Test
    public void isFiltered_undatedNote_shouldFilter() {
        // WHEN we use an undated Note, THEN it should automatically be filtered:
        assertTrue(
                new DayOfWeekFilter(DayOfWeek.MONDAY, DayOfWeekFilter.FilterType.IS).isFiltered(NOTE_UNDATED_UNTAGGED));
    }

    @Test
    public void isFiltered_ISwithMatchingDayOfWeek_shouldNotFilter() {
        // GIVEN a DayOfWeekFilter with IS type and a target day of week that matches our test note:
        DayOfWeekFilter filter = new DayOfWeekFilter(DayOfWeek.MONDAY, DayOfWeekFilter.FilterType.IS);

        // WHEN we try to filter the note:
        boolean actual = filter.isFiltered(NOTE_DATED_TAGGED);

        // THEN it should not be filtered, because the Note's day of week matches the target day of week:
        assertFalse(actual);
    }

    @Test
    public void isFiltered_ISwithNonMatchingDayOfWeek_shouldFilter() {
        // GIVEN a DayOfWeekFilter with IS type and a target day of week that does not match our test note:
        DayOfWeekFilter filter = new DayOfWeekFilter(DayOfWeek.TUESDAY, DayOfWeekFilter.FilterType.IS);

        // WHEN we try to filter the note:
        boolean actual = filter.isFiltered(NOTE_DATED_TAGGED);

        // THEN it should be filtered, because the Note's day of week does not match the target day of week:
        assertTrue(actual);
    }

    @Test
    public void isFiltered_IS_NOTwithMatchingDayOfWeek_shouldFilter() {
        // GIVEN a DayOfWeekFilter with IS_NOT type and a target day of week that
        // matches our test note:
        DayOfWeekFilter filter = new DayOfWeekFilter(DayOfWeek.MONDAY, DayOfWeekFilter.FilterType.IS_NOT);

        // WHEN we try to filter the note:
        boolean actual = filter.isFiltered(NOTE_DATED_TAGGED);

        // THEN it should be filtered, because it matches, and we're in IS_NOT mode:
        assertTrue(actual);
    }

    @Test
    public void isFiltered_IS_NOTwithNonMatchingDayOfWeek_shouldNotFilter() {
        // GIVEN a DayOfWeekFilter with IS_NOT type and a target day of week that
        // does not match our test note:
        DayOfWeekFilter filter = new DayOfWeekFilter(DayOfWeek.TUESDAY, DayOfWeekFilter.FilterType.IS_NOT);

        // WHEN we try to filter the note:
        boolean actual = filter.isFiltered(NOTE_DATED_TAGGED);

        // THEN it should not be filtered, because it does not match, and we're in IS_NOT mode:
        assertFalse(actual);
    }
}