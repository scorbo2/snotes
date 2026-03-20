package ca.corbett.snotes.model;

import ca.corbett.snotes.model.filter.DateFilter;
import ca.corbett.snotes.model.filter.DayOfMonthFilter;
import ca.corbett.snotes.model.filter.DayOfWeekFilter;
import ca.corbett.snotes.model.filter.FilterTest;
import ca.corbett.snotes.model.filter.MonthFilter;
import ca.corbett.snotes.model.filter.TagFilter;
import ca.corbett.snotes.model.filter.TextFilter;
import ca.corbett.snotes.model.filter.UndatedFilter;
import ca.corbett.snotes.model.filter.YearFilter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.DayOfWeek;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class QueryTest extends FilterTest {

    @TempDir
    File tempDir;

    @Test
    public void filter_withEmptyQuery_shouldFilterNothing() {
        // GIVEN an empty Query:
        Query query = new Query();

        // WHEN we try to filter our test list:
        List<Note> results = query.filter(unfilteredList);

        // THEN nothing should have been filtered:
        assertNotNull(results);
        assertEquals(unfilteredList.size(), results.size());
    }

    @Test
    public void filter_singleDateFilterMatches_shouldSucceed() {
        // GIVEN a query with a single DateFilter that is matched in our test set:
        Query query = new Query();
        query.addFilter(new DateFilter(SPECIAL_DATE, DateFilter.FilterType.ON));

        // WHEN we try to filter our test list:
        List<Note> results = query.filter(unfilteredList);

        // THEN we should get back our two test notes that have this date:
        assertNotNull(results);
        assertEquals(2, results.size());
        assertEquals(SPECIAL_DATE, results.get(0).getDate());
        assertEquals(SPECIAL_DATE, results.get(1).getDate());
    }

    @Test
    public void filter_YearAndMonthFilterMatches_shouldSucceed() {
        // GIVEN a query with a YearFilter and a MonthFilter that are matched in our test set:
        Query query = new Query();
        query.addFilter(new YearFilter(1997, YearFilter.FilterType.ON));
        query.addFilter(new MonthFilter(4, MonthFilter.FilterType.IS));

        // WHEN we try to filter our test list:
        List<Note> results = query.filter(unfilteredList);

        // THEN we should get back our two test Notes that have this date:
        assertNotNull(results);
        assertEquals(2, results.size());
        assertEquals(SPECIAL_DATE, results.get(0).getDate());
        assertEquals(SPECIAL_DATE, results.get(1).getDate());
    }

    @Test
    public void filter_YearAndMonthFilterMatchesTextFilterNoMatch_shouldReturnNothing() {
        // GIVEN a query with a YearFilter and a MonthFilter that are matched in our test set:
        Query query = new Query();
        query.addFilter(new YearFilter(1997, YearFilter.FilterType.ON));
        query.addFilter(new MonthFilter(4, MonthFilter.FilterType.IS));

        // AND GIVEN a text filter that does NOT match any of our test notes:
        query.addFilter(new TextFilter("blah de blah blah"));

        // WHEN we try to filter our test list:
        List<Note> results = query.filter(unfilteredList);

        // THEN we should find that all results are filtered, because no Note matches all of our filters:
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    public void filter_betweenInclusiveWithMatches_shouldSucceed() {
        // GIVEN a query with two DateFilters that describe a BETWEEN condition:
        Query query = new Query();
        query.addFilter(new DateFilter(JAN_1_2020, DateFilter.FilterType.AFTER_INCLUSIVE));
        query.addFilter(new DateFilter(FEB_15_2020, DateFilter.FilterType.BEFORE_INCLUSIVE));

        // WHEN we try to filter our test list:
        List<Note> results = query.filter(unfilteredList);

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
    public void filter_betweenExclusiveWithoutMatches_shouldSucceed() {
        // GIVEN a query with two DateFilters that describe a BETWEEN condition:
        Query query = new Query();
        query.addFilter(new DateFilter(JAN_1_2020, DateFilter.FilterType.AFTER_EXCLUSIVE));
        query.addFilter(new DateFilter(FEB_15_2020, DateFilter.FilterType.BEFORE_EXCLUSIVE));

        // WHEN we try to filter our test list:
        List<Note> results = query.filter(unfilteredList);

        // THEN our two test notes that are right on the boundary of the range should be filtered out:
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    public void filter_withMultipleMatchingFilters_shouldSucceed() {
        // GIVEN a ridiculous setup with four filters that specify an exact date:
        //   (obviously it would be easier to use a single DateFilter, but let's just try it)
        Query query = new Query();
        query.addFilter(new YearFilter(1997, YearFilter.FilterType.ON));
        query.addFilter(new MonthFilter(4, MonthFilter.FilterType.IS));
        query.addFilter(new DayOfMonthFilter(21, DayOfMonthFilter.FilterType.IS));
        query.addFilter(new DayOfWeekFilter(DayOfWeek.MONDAY, DayOfWeekFilter.FilterType.IS));

        // WHEN we try to filter our test list:
        List<Note> results = query.filter(unfilteredList);

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
    public void save_roundTrip_shouldLoad() {
        // GIVEN a Query with some filters and a name:
        Query query = new Query();
        query.setName("My Test Query");
        query.addFilter(new YearFilter(1997, YearFilter.FilterType.ON));
        query.addFilter(new MonthFilter(4, MonthFilter.FilterType.IS));
        query.addFilter(new TextFilter("test"));
        query.addFilter(new DateFilter(JAN_1_2020, DateFilter.FilterType.AFTER_INCLUSIVE));
        query.addFilter(new DayOfWeekFilter(DayOfWeek.MONDAY, DayOfWeekFilter.FilterType.IS));
        query.addFilter(new DayOfMonthFilter(21, DayOfMonthFilter.FilterType.IS));
        query.addFilter(new TagFilter(List.of(new Tag("test-tag")), TagFilter.FilterType.ALL));
        query.addFilter(new UndatedFilter());

        try {
            // WHEN we save it to a file and then load it back:
            File savedFile = File.createTempFile("test", ".query", tempDir);
            query.save(savedFile);
            assertNotNull(savedFile);
            assertTrue(savedFile.exists());
            Query loadedQuery = Query.load(savedFile);

            // THEN the loaded Query should have the same name and filters as the original:
            assertNotNull(loadedQuery);
            assertEquals(query.getName(), loadedQuery.getName());
            assertEquals(query.size(), loadedQuery.size());

            // AND the filters should be present in the same order, so let's go through them and verify types and values
            assertInstanceOf(YearFilter.class, loadedQuery.getFilters().get(0));
            YearFilter loadedYearFilter = (YearFilter)loadedQuery.getFilters().get(0);
            assertEquals(1997, loadedYearFilter.getTargetYear());
            assertEquals(YearFilter.FilterType.ON, loadedYearFilter.getFilterType());
            assertInstanceOf(MonthFilter.class, loadedQuery.getFilters().get(1));
            MonthFilter loadedMonthFilter = (MonthFilter)loadedQuery.getFilters().get(1);
            assertEquals(4, loadedMonthFilter.getTargetMonth());
            assertEquals(MonthFilter.FilterType.IS, loadedMonthFilter.getFilterType());
            assertInstanceOf(TextFilter.class, loadedQuery.getFilters().get(2));
            TextFilter loadedTextFilter = (TextFilter)loadedQuery.getFilters().get(2);
            assertEquals("test", loadedTextFilter.getContains());
            assertInstanceOf(DateFilter.class, loadedQuery.getFilters().get(3));
            DateFilter loadedDateFilter = (DateFilter)loadedQuery.getFilters().get(3);
            assertEquals(JAN_1_2020, loadedDateFilter.getTargetDate());
            assertEquals(DateFilter.FilterType.AFTER_INCLUSIVE, loadedDateFilter.getFilterType());
            assertInstanceOf(DayOfWeekFilter.class, loadedQuery.getFilters().get(4));
            DayOfWeekFilter loadedDayOfWeekFilter = (DayOfWeekFilter)loadedQuery.getFilters().get(4);
            assertEquals(DayOfWeek.MONDAY, loadedDayOfWeekFilter.getDayOfWeek());
            assertEquals(DayOfWeekFilter.FilterType.IS, loadedDayOfWeekFilter.getFilterType());
            assertInstanceOf(DayOfMonthFilter.class, loadedQuery.getFilters().get(5));
            DayOfMonthFilter loadedDayOfMonthFilter = (DayOfMonthFilter)loadedQuery.getFilters().get(5);
            assertEquals(21, loadedDayOfMonthFilter.getDayOfMonth());
            assertEquals(DayOfMonthFilter.FilterType.IS, loadedDayOfMonthFilter.getFilterType());
            assertInstanceOf(TagFilter.class, loadedQuery.getFilters().get(6));
            TagFilter loadedTagFilter = (TagFilter)loadedQuery.getFilters().get(6);
            assertEquals(1, loadedTagFilter.getTagsToFilter().size());
            assertEquals("test-tag", loadedTagFilter.getTagsToFilter().get(0).getTag());
            assertEquals(TagFilter.FilterType.ALL, loadedTagFilter.getFilterType());
            assertInstanceOf(UndatedFilter.class, loadedQuery.getFilters().get(7));
        }
        catch (IOException ioe) {
            fail("IOException thrown during save/load: " + ioe.getMessage());
        }
    }

    @Test
    public void load_withMalformedDate_shouldThrowIOException() throws IOException {
        // GIVEN a Query saved with a valid DateFilter:
        Query query = new Query();
        query.setName("Malformed Date Test");
        query.addFilter(new DateFilter(JAN_1_2020, DateFilter.FilterType.ON));
        File savedFile = File.createTempFile("test-malformed", ".query", tempDir);
        query.save(savedFile);

        // WHEN we corrupt the saved file by replacing the valid date with a malformed one:
        String content = Files.readString(savedFile.toPath(), StandardCharsets.UTF_8);
        String corrupted = content.replace("2020-01-01", "not-a-real-date");
        Files.writeString(savedFile.toPath(), corrupted, StandardCharsets.UTF_8);

        // THEN Query.load() should throw an IOException rather than silently
        // loading a filter with today's date substituted in:
        assertThrows(IOException.class, () -> Query.load(savedFile));
    }
}

