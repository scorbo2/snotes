package ca.corbett.snotes.model.filter;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YearFilterTest extends FilterTest {

    @Test
    public void constructor_withNullInput_shouldThrow() {
        // WHEN we give garbage to the constructor, THEN it should immediately throw:
        assertThrows(IllegalArgumentException.class, () -> new YearFilter(2020, null));
    }

    @Test
    public void isFiltered_withUndatedNote_shouldFilter() {
        // WHEN we use an undated Note, THEN it should automatically be filtered:
        assertTrue(new YearFilter(2020, DateFilterType.ON).isFiltered(NOTE_UNDATED_UNTAGGED));
    }

    @Test
    public void isFiltered_ONwithDifferentYear_shouldFilter() {
        // GIVEN a YearFilter with ON type and a target year different from our test note:
        YearFilter filter = new YearFilter(2020, DateFilterType.ON);

        // WHEN we try to filter a note from a different year:
        boolean actual = filter.isFiltered(NOTE_DATED_TAGGED);

        // THEN it should be filtered, because the Note's year does not match the target year:
        assertTrue(actual);
    }

    @Test
    public void isFiltered_ONwithExactYearMatch_shouldNotFilter() {
        // GIVEN a YearFilter with ON type and the exact year of our test note:
        YearFilter filter = new YearFilter(2020, DateFilterType.ON);

        // WHEN we try to filter the note:
        boolean actual = filter.isFiltered(NOTE_JAN_1_2020);

        // THEN it should not be filtered, because the Note's year matches the target year:
        assertFalse(actual);
    }

    @Test
    public void isFiltered_BEFORE_EXCLUSIVEwithYearBeforeTarget_shouldNotFilter() {
        // GIVEN a YearFilter with BEFORE_EXCLUSIVE type:
        YearFilter filter = new YearFilter(2020, DateFilterType.BEFORE_EXCLUSIVE);

        // WHEN we try to filter a note whose year is before the target year:
        boolean actual = filter.isFiltered(NOTE_VERY_OLD);

        // THEN it should not be filtered, because strictly earlier years are included:
        assertFalse(actual);
    }

    @Test
    public void isFiltered_BEFORE_EXCLUSIVEwithYearOnTarget_shouldFilter() {
        // GIVEN a YearFilter with BEFORE_EXCLUSIVE type:
        YearFilter filter = new YearFilter(2020, DateFilterType.BEFORE_EXCLUSIVE);

        // WHEN we try to filter a note whose year is exactly the target year:
        boolean actual = filter.isFiltered(NOTE_FEB_15_2020);

        // THEN it should be filtered, because BEFORE_EXCLUSIVE rejects boundary matches:
        assertTrue(actual);
    }

    @Test
    public void isFiltered_BEFORE_INCLUSIVEwithYearOnTarget_shouldNotFilter() {
        // GIVEN a YearFilter with BEFORE_INCLUSIVE type:
        YearFilter filter = new YearFilter(2020, DateFilterType.BEFORE_INCLUSIVE);

        // WHEN we try to filter a note whose year is exactly the target year:
        boolean actual = filter.isFiltered(NOTE_JAN_1_2020);

        // THEN it should not be filtered, because BEFORE_INCLUSIVE includes boundary matches:
        assertFalse(actual);
    }

    @Test
    public void isFiltered_BEFORE_INCLUSIVEwithYearAfterTarget_shouldFilter() {
        // GIVEN a YearFilter with BEFORE_INCLUSIVE type:
        YearFilter filter = new YearFilter(2020, DateFilterType.BEFORE_INCLUSIVE);

        // WHEN we try to filter a note whose year is after the target year:
        boolean actual = filter.isFiltered(NOTE_VERY_FUTURE);

        // THEN it should be filtered, because later years are excluded:
        assertTrue(actual);
    }

    @Test
    public void isFiltered_AFTER_INCLUSIVEwithYearOnTarget_shouldNotFilter() {
        // GIVEN a YearFilter with AFTER_INCLUSIVE type:
        YearFilter filter = new YearFilter(2020, DateFilterType.AFTER_INCLUSIVE);

        // WHEN we try to filter a note whose year is exactly the target year:
        boolean actual = filter.isFiltered(NOTE_FEB_15_2020);

        // THEN it should not be filtered, because AFTER_INCLUSIVE includes boundary matches:
        assertFalse(actual);
    }

    @Test
    public void isFiltered_AFTER_INCLUSIVEwithYearBeforeTarget_shouldFilter() {
        // GIVEN a YearFilter with AFTER_INCLUSIVE type:
        YearFilter filter = new YearFilter(2020, DateFilterType.AFTER_INCLUSIVE);

        // WHEN we try to filter a note whose year is before the target year:
        boolean actual = filter.isFiltered(NOTE_VERY_OLD);

        // THEN it should be filtered, because earlier years are excluded:
        assertTrue(actual);
    }

    @Test
    public void isFiltered_AFTER_EXCLUSIVEwithYearAfterTarget_shouldNotFilter() {
        // GIVEN a YearFilter with AFTER_EXCLUSIVE type:
        YearFilter filter = new YearFilter(2020, DateFilterType.AFTER_EXCLUSIVE);

        // WHEN we try to filter a note whose year is after the target year:
        boolean actual = filter.isFiltered(NOTE_VERY_FUTURE);

        // THEN it should not be filtered, because strictly later years are included:
        assertFalse(actual);
    }

    @Test
    public void isFiltered_AFTER_EXCLUSIVEwithYearOnTarget_shouldFilter() {
        // GIVEN a YearFilter with AFTER_EXCLUSIVE type:
        YearFilter filter = new YearFilter(2020, DateFilterType.AFTER_EXCLUSIVE);

        // WHEN we try to filter a note whose year is exactly the target year:
        boolean actual = filter.isFiltered(NOTE_JAN_1_2020);

        // THEN it should be filtered, because AFTER_EXCLUSIVE rejects boundary matches:
        assertTrue(actual);
    }
}