package ca.corbett.snotes.model;

import ca.corbett.snotes.model.filter.BooleanFilterType;
import ca.corbett.snotes.model.filter.DateFilter;
import ca.corbett.snotes.model.filter.DateFilterType;
import ca.corbett.snotes.model.filter.DayOfMonthFilter;
import ca.corbett.snotes.model.filter.DayOfWeekFilter;
import ca.corbett.snotes.model.filter.FilterTest;
import ca.corbett.snotes.model.filter.MonthFilter;
import ca.corbett.snotes.model.filter.TextFilter;
import ca.corbett.snotes.model.filter.YearFilter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.time.DayOfWeek;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class QueryTest extends FilterTest {

    @TempDir
    File tempDir;

    @Test
    public void execute_withEmptyQuery_shouldExecuteNothing() {
        // GIVEN an empty Query:
        Query query = new Query();

        // WHEN we try to filter our test list:
        List<Note> results = query.execute(unfilteredList);

        // THEN nothing should have been filtered:
        assertNotNull(results);
        assertEquals(unfilteredList.size(), results.size());
    }

    @Test
    public void execute_singleDateFilterMatches_shouldSucceed() {
        // GIVEN a query with a single DateFilter that is matched in our test set:
        Query query = new Query();
        query.addFilter(new DateFilter(SPECIAL_DATE, DateFilterType.ON));

        // WHEN we try to filter our test list:
        List<Note> results = query.execute(unfilteredList);

        // THEN we should get back our two test notes that have this date:
        assertNotNull(results);
        assertEquals(2, results.size());
        assertEquals(SPECIAL_DATE, results.get(0).getDate());
        assertEquals(SPECIAL_DATE, results.get(1).getDate());
    }

    @Test
    public void execute_YearAndMonthFilterMatches_shouldSucceed() {
        // GIVEN a query with a YearFilter and a MonthFilter that are matched in our test set:
        Query query = new Query();
        query.addFilter(new YearFilter(1997, DateFilterType.ON));
        query.addFilter(new MonthFilter(4, BooleanFilterType.IS));

        // WHEN we try to filter our test list:
        List<Note> results = query.execute(unfilteredList);

        // THEN we should get back our two test Notes that have this date:
        assertNotNull(results);
        assertEquals(2, results.size());
        assertEquals(SPECIAL_DATE, results.get(0).getDate());
        assertEquals(SPECIAL_DATE, results.get(1).getDate());
    }

    @Test
    public void execute_YearAndMonthFilterMatchesTextFilterNoMatch_shouldReturnNothing() {
        // GIVEN a query with a YearFilter and a MonthFilter that are matched in our test set:
        Query query = new Query();
        query.addFilter(new YearFilter(1997, DateFilterType.ON));
        query.addFilter(new MonthFilter(4, BooleanFilterType.IS));

        // AND GIVEN a text filter that does NOT match any of our test notes:
        query.addFilter(new TextFilter("blah de blah blah"));

        // WHEN we try to filter our test list:
        List<Note> results = query.execute(unfilteredList);

        // THEN we should find that all results are filtered, because no Note matches all of our filters:
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    public void execute_betweenInclusiveWithMatches_shouldSucceed() {
        // GIVEN a query with two DateFilters that describe a BETWEEN condition:
        Query query = new Query();
        query.addFilter(new DateFilter(JAN_1_2020, DateFilterType.AFTER_INCLUSIVE));
        query.addFilter(new DateFilter(FEB_15_2020, DateFilterType.BEFORE_INCLUSIVE));

        // WHEN we try to filter our test list:
        List<Note> results = query.execute(unfilteredList);

        // THEN we should find the two test Notes that have dates between (and including) our two boundary dates:
        assertNotNull(results);
        assertEquals(2, results.size());
        YMDDate actualDate1 = results.get(0).getDate();
        YMDDate actualDate2 = results.get(1).getDate();
        assertNotNull(actualDate1);
        assertNotNull(actualDate2);

        // We can't guarantee the order in which Notes are returned from a search,
        // but we should be able to confirm that our two expected dates are present in the results:
        assertTrue(JAN_1_2020.equals(actualDate1) || JAN_1_2020.equals(actualDate2));
        assertTrue(FEB_15_2020.equals(actualDate1) || FEB_15_2020.equals(actualDate2));
    }

    @Test
    public void execute_betweenExclusiveWithoutMatches_shouldSucceed() {
        // GIVEN a query with two DateFilters that describe a BETWEEN condition:
        Query query = new Query();
        query.addFilter(new DateFilter(JAN_1_2020, DateFilterType.AFTER_EXCLUSIVE));
        query.addFilter(new DateFilter(FEB_15_2020, DateFilterType.BEFORE_EXCLUSIVE));

        // WHEN we try to filter our test list:
        List<Note> results = query.execute(unfilteredList);

        // THEN our two test notes that are right on the boundary of the range should be filtered out:
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    public void execute_withMultipleMatchingFilters_shouldSucceed() {
        // GIVEN a ridiculous setup with four filters that specify an exact date:
        //   (obviously it would be easier to use a single DateFilter, but let's just try it)
        Query query = new Query();
        query.addFilter(new YearFilter(1997, DateFilterType.ON));
        query.addFilter(new MonthFilter(4, BooleanFilterType.IS));
        query.addFilter(new DayOfMonthFilter(21, BooleanFilterType.IS));
        query.addFilter(new DayOfWeekFilter(DayOfWeek.MONDAY, BooleanFilterType.IS));

        // WHEN we try to filter our test list:
        List<Note> results = query.execute(unfilteredList);

        // THEN we should get back the two test Notes that have that exact date:
        assertNotNull(results);
        assertEquals(2, results.size());
        assertEquals(SPECIAL_DATE, results.get(0).getDate());
        assertEquals(SPECIAL_DATE, results.get(1).getDate());
    }

    @Test
    public void constructor_withNoName_shouldCreateUnnamedQuery() {
        // WHEN we construct a Query with no name:
        Query query = new Query();

        // THEN the Query should have the default name:
        assertNotNull(query.getName());
        assertEquals(Query.DEFAULT_NAME, query.getName());
    }

    @Test
    public void setName_withNullOrBlank_shouldSetDefaultName() {
        // GIVEN a Query with a non-default name:
        Query query = new Query();
        query.setName("My Custom Query");
        assertEquals("My Custom Query", query.getName());

        // WHEN we set the name to null:
        query.setName((String)null);

        // THEN the name should be reset to the default:
        assertEquals(Query.DEFAULT_NAME, query.getName());

        // WHEN we set the name to blank:
        query.setName("   ");

        // THEN the name should again be reset to the default:
        assertEquals(Query.DEFAULT_NAME, query.getName());
    }

    @Test
    public void setName_withNameTooLong_shouldTruncateName() {
        // GIVEN a Query:
        Query query = new Query();

        // WHEN we set the name to a string that is longer than the limit:
        String longName = "This is a very long query name that exceeds the limit";
        query.setName(longName);

        // THEN the name should be truncated to the limit:
        assertEquals(Query.NAME_LENGTH_LIMIT, query.getName().length());
        assertEquals(longName.substring(0, Query.NAME_LENGTH_LIMIT), query.getName());
    }

    @Test
    public void setName_withValidName_shouldSet() {
        // GIVEN a Query:
        Query query = new Query();

        // WHEN we set the name to a valid string that is within the limit:
        String validName = "My Valid Query Name";
        query.setName(validName);

        // THEN the name should be set correctly:
        assertEquals(validName, query.getName());
    }

    @Test
    public void execute_withLimit_shouldReturnMostRecentN() {
        // GIVEN an empty Query that will return all notes from the unfiltered list:
        Query query = new Query();

        // WHEN we execute with a limit of 1:
        List<Note> results = query.execute(unfilteredList, 1);

        // THEN we should get back only the single most recent Note (NOTE_VERY_FUTURE):
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(VERY_FUTURE_DATE, results.get(0).getDate());
    }

    @Test
    public void execute_withLimitLargerThanResults_shouldReturnAll() {
        // GIVEN a Query with a date filter that returns exactly 2 notes:
        Query query = new Query();
        query.addFilter(new DateFilter(SPECIAL_DATE, DateFilterType.ON));

        // WHEN we execute with a limit larger than the number of matching notes:
        List<Note> results = query.execute(unfilteredList, 100);

        // THEN all matching notes should be returned (no truncation):
        assertNotNull(results);
        assertEquals(2, results.size());
    }

    @Test
    public void execute_withZeroLimit_shouldReturnAll() {
        // GIVEN an empty Query that will return all notes:
        Query query = new Query();

        // WHEN we execute with a limit of 0 (meaning no limit):
        List<Note> results = query.execute(unfilteredList, 0);

        // THEN all notes should be returned, just as if no limit was specified:
        assertNotNull(results);
        assertEquals(unfilteredList.size(), results.size());
    }

    @Test
    public void execute_withNegativeLimit_shouldReturnAll() {
        // GIVEN an empty Query that will return all notes:
        Query query = new Query();

        // WHEN we execute with a negative limit (meaning no limit):
        List<Note> results = query.execute(unfilteredList, -1);

        // THEN all notes should be returned, just as if no limit was specified:
        assertNotNull(results);
        assertEquals(unfilteredList.size(), results.size());
    }
}

