package ca.corbett.snotes.model.filter;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DateFilterTest extends FilterTest {

    @Test
    public void constructor_withNullInput_shouldThrow() {
        // WHEN we give garbage to the constructor, THEN it should immediately throw:
        assertThrows(IllegalArgumentException.class, () -> new DateFilter(null, DateFilterType.ON));
        assertThrows(IllegalArgumentException.class, () -> new DateFilter(SPECIAL_DATE, null));
    }

    @Test
    public void isFiltered_withUndatedNote_shouldFilter() {
        // WHEN we use an undated Note, THEN it should automatically be filtered:
        assertTrue(new DateFilter(SPECIAL_DATE, DateFilterType.ON).isFiltered(NOTE_UNDATED_UNTAGGED));
    }

    @Test
    public void isFiltered_ONwithDifferentDate_shouldFilter() {
        // GIVEN a DateFilter with ON type and a target date different from our test note:
        DateFilter filter = new DateFilter(SPECIAL_DATE, DateFilterType.ON);

        // WHEN we try to filter a note with a different date:
        boolean actual = filter.isFiltered(NOTE_JAN_1_2020);

        // THEN it should be filtered, because the Note's date does not match the target date:
        assertTrue(actual);
    }

    @Test
    public void isFiltered_ONwithExactMatch_shouldNotFilter() {
        // GIVEN a DateFilter with ON type and the exact date of our test note:
        DateFilter filter = new DateFilter(SPECIAL_DATE, DateFilterType.ON);

        // WHEN we try to filter the note:
        boolean actual = filter.isFiltered(NOTE_DATED_TAGGED);

        // THEN it should not be filtered, because the Note's date matches the target date:
        assertFalse(actual);
    }

    @Test
    public void isFiltered_BEFORE_EXCLUSIVEwithDateBeforeTarget_shouldNotFilter() {
        // GIVEN a DateFilter with BEFORE_EXCLUSIVE type:
        DateFilter filter = new DateFilter(JAN_1_2020, DateFilterType.BEFORE_EXCLUSIVE);

        // WHEN we try to filter a note that is before the target date:
        boolean actual = filter.isFiltered(NOTE_VERY_OLD);

        // THEN it should not be filtered, because strictly earlier dates are included:
        assertFalse(actual);
    }

    @Test
    public void isFiltered_BEFORE_EXCLUSIVEwithDateOnTarget_shouldFilter() {
        // GIVEN a DateFilter with BEFORE_EXCLUSIVE type:
        DateFilter filter = new DateFilter(JAN_1_2020, DateFilterType.BEFORE_EXCLUSIVE);

        // WHEN we try to filter a note that is exactly on the target date:
        boolean actual = filter.isFiltered(NOTE_JAN_1_2020);

        // THEN it should be filtered, because BEFORE_EXCLUSIVE rejects boundary matches:
        assertTrue(actual);
    }

    @Test
    public void isFiltered_BEFORE_INCLUSIVEwithDateOnTarget_shouldNotFilter() {
        // GIVEN a DateFilter with BEFORE_INCLUSIVE type:
        DateFilter filter = new DateFilter(JAN_1_2020, DateFilterType.BEFORE_INCLUSIVE);

        // WHEN we try to filter a note that is exactly on the target date:
        boolean actual = filter.isFiltered(NOTE_JAN_1_2020);

        // THEN it should not be filtered, because BEFORE_INCLUSIVE includes boundary matches:
        assertFalse(actual);
    }

    @Test
    public void isFiltered_BEFORE_INCLUSIVEwithDateAfterTarget_shouldFilter() {
        // GIVEN a DateFilter with BEFORE_INCLUSIVE type:
        DateFilter filter = new DateFilter(JAN_1_2020, DateFilterType.BEFORE_INCLUSIVE);

        // WHEN we try to filter a note that is after the target date:
        boolean actual = filter.isFiltered(NOTE_FEB_15_2020);

        // THEN it should be filtered, because later dates are excluded:
        assertTrue(actual);
    }

    @Test
    public void isFiltered_AFTER_INCLUSIVEwithDateOnTarget_shouldNotFilter() {
        // GIVEN a DateFilter with AFTER_INCLUSIVE type:
        DateFilter filter = new DateFilter(JAN_1_2020, DateFilterType.AFTER_INCLUSIVE);

        // WHEN we try to filter a note that is exactly on the target date:
        boolean actual = filter.isFiltered(NOTE_JAN_1_2020);

        // THEN it should not be filtered, because AFTER_INCLUSIVE includes boundary matches:
        assertFalse(actual);
    }

    @Test
    public void isFiltered_AFTER_INCLUSIVEwithDateBeforeTarget_shouldFilter() {
        // GIVEN a DateFilter with AFTER_INCLUSIVE type:
        DateFilter filter = new DateFilter(JAN_1_2020, DateFilterType.AFTER_INCLUSIVE);

        // WHEN we try to filter a note that is before the target date:
        boolean actual = filter.isFiltered(NOTE_VERY_OLD);

        // THEN it should be filtered, because earlier dates are excluded:
        assertTrue(actual);
    }

    @Test
    public void isFiltered_AFTER_EXCLUSIVEwithDateAfterTarget_shouldNotFilter() {
        // GIVEN a DateFilter with AFTER_EXCLUSIVE type:
        DateFilter filter = new DateFilter(JAN_1_2020, DateFilterType.AFTER_EXCLUSIVE);

        // WHEN we try to filter a note that is after the target date:
        boolean actual = filter.isFiltered(NOTE_FEB_15_2020);

        // THEN it should not be filtered, because strictly later dates are included:
        assertFalse(actual);
    }

    @Test
    public void isFiltered_AFTER_EXCLUSIVEwithDateOnTarget_shouldFilter() {
        // GIVEN a DateFilter with AFTER_EXCLUSIVE type:
        DateFilter filter = new DateFilter(JAN_1_2020, DateFilterType.AFTER_EXCLUSIVE);

        // WHEN we try to filter a note that is exactly on the target date:
        boolean actual = filter.isFiltered(NOTE_JAN_1_2020);

        // THEN it should be filtered, because AFTER_EXCLUSIVE rejects boundary matches:
        assertTrue(actual);
    }

}