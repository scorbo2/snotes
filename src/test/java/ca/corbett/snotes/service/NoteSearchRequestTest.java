package ca.corbett.snotes.service;

import ca.corbett.snotes.model.DateTag;
import ca.corbett.snotes.model.Query;
import ca.corbett.snotes.model.Tag;
import ca.corbett.snotes.model.YMDDate;
import ca.corbett.snotes.model.filter.DateFilter;
import ca.corbett.snotes.model.filter.DateFilterType;
import ca.corbett.snotes.model.filter.Filter;
import ca.corbett.snotes.model.filter.TagFilter;
import ca.corbett.snotes.model.filter.TextFilter;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link NoteSearchRequest} and its builder.
 * These tests are pure - they never touch a DataManager or the filesystem.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 * @since Snotes 2.3
 */
public class NoteSearchRequestTest {

    private static final YMDDate MARCH_1_2024 = YMDDate.fromJson("2024-03-01");
    private static final YMDDate MARCH_15_2024 = YMDDate.fromJson("2024-03-15");

    // -----------------------------------------------------------------
    // Builder defaults and validation
    // -----------------------------------------------------------------

    @Test
    public void builder_noCriteria_build_shouldBeEmpty() {
        // GIVEN a builder with no criteria set:
        NoteSearchRequest request = NoteSearchRequest.builder().build();

        // THEN the request should have all default values:
        assertTrue(request.isEmpty());
        assertNull(request.getText());
        assertTrue(request.getTags().isEmpty());
        assertNull(request.getStartDate());
        assertNull(request.getEndDate());
        assertEquals(NoteSearchRequest.NO_LIMIT, request.getLimit());
    }

    @Test
    public void builder_negativeLimit_build_shouldThrowIllegalArgumentException() {
        // GIVEN a builder with a negative limit:
        NoteSearchRequest.Builder builder = NoteSearchRequest.builder().withLimit(-1);

        // WHEN we try to build, THEN an IllegalArgumentException should be thrown:
        assertThrows(IllegalArgumentException.class, builder::build);
    }

    @Test
    public void builder_startDateAfterEndDate_build_shouldThrowIllegalArgumentException() {
        // GIVEN a builder whose start date is after its end date:
        NoteSearchRequest.Builder builder = NoteSearchRequest.builder()
            .withStartDate(MARCH_15_2024)
            .withEndDate(MARCH_1_2024);

        // WHEN we try to build, THEN an IllegalArgumentException should be thrown:
        assertThrows(IllegalArgumentException.class, builder::build);
    }

    @Test
    public void builder_startDateEqualToEndDate_build_shouldSucceed() {
        // GIVEN a builder with the same date for start and end (a one-day range):
        NoteSearchRequest request = NoteSearchRequest.builder()
            .withStartDate(MARCH_1_2024)
            .withEndDate(MARCH_1_2024)
            .build();

        // THEN both bounds should be preserved:
        assertEquals(MARCH_1_2024, request.getStartDate());
        assertEquals(MARCH_1_2024, request.getEndDate());
    }

    @Test
    public void builder_withTagsNullList_shouldClearTags() {
        // GIVEN a builder with some tags:
        NoteSearchRequest.Builder builder = NoteSearchRequest.builder().withTags("work");

        // WHEN we replace them with a null list:
        builder.withTags((List<String>)null);
        NoteSearchRequest request = builder.build();

        // THEN the tags should be cleared:
        assertTrue(request.getTags().isEmpty());
    }

    @Test
    public void builder_withTagsNullVarargs_shouldClearTags() {
        // GIVEN a builder with some tags:
        NoteSearchRequest.Builder builder = NoteSearchRequest.builder().withTags("work");

        // WHEN we replace them with a null varargs array:
        String[] nullVarargs = null;
        builder.withTags(nullVarargs);
        NoteSearchRequest request = builder.build();

        // THEN the tags should be cleared:
        assertTrue(request.getTags().isEmpty());
    }

    @Test
    public void builder_withTagsContainingNullAndBlank_shouldSkipInvalidEntries() {
        // GIVEN a builder asked to accept a mix of valid, blank, and null tags:
        NoteSearchRequest request = NoteSearchRequest.builder()
            .withTags("work", null, "   ", "project-alpha")
            .build();

        // THEN only the valid tags should be present, in order:
        assertEquals(List.of("work", "project-alpha"), request.getTags());
    }

    @Test
    public void builder_withTextNullOrBlank_shouldClearText() {
        // GIVEN a builder with text set:
        NoteSearchRequest.Builder builder = NoteSearchRequest.builder().withText("fox");

        // WHEN we replace the text with null, then with blank:
        builder.withText(null);
        NoteSearchRequest afterNull = builder.build();
        builder.withText("   ");
        NoteSearchRequest afterBlank = builder.build();

        // THEN both requests should have no text criterion:
        assertNull(afterNull.getText());
        assertNull(afterBlank.getText());
        assertTrue(afterNull.isEmpty());
        assertTrue(afterBlank.isEmpty());
    }

    @Test
    public void builder_withStartDateWrongFormat_shouldThrowIllegalArgumentException() {
        // GIVEN a builder asked to parse a real date in the wrong format:
        NoteSearchRequest.Builder builder = NoteSearchRequest.builder();

        // "03/01/2024" is a valid date, but not a valid yyyy-MM-dd string,
        // so strict parsing must reject it rather than silently misinterpret it:
        assertThrows(IllegalArgumentException.class, () -> builder.withStartDate("03/01/2024"));
    }

    @Test
    public void builder_withStartDateImpossibleDate_shouldThrowIllegalArgumentException() {
        // GIVEN a builder asked to parse an impossible calendar date:
        NoteSearchRequest.Builder builder = NoteSearchRequest.builder();

        // February 30th does not exist; STRICT parsing must reject it:
        assertThrows(IllegalArgumentException.class, () -> builder.withStartDate("2024-02-30"));
    }

    @Test
    public void builder_withEndDateInvalidString_shouldThrowIllegalArgumentException() {
        // GIVEN a builder asked to parse a malformed end date:
        NoteSearchRequest.Builder builder = NoteSearchRequest.builder();

        // WHEN we pass garbage, THEN an IllegalArgumentException should be thrown:
        assertThrows(IllegalArgumentException.class, () -> builder.withEndDate("not a date"));
    }

    @Test
    public void builder_withBlankDateString_shouldClearBound() {
        // GIVEN a builder with both date bounds set:
        NoteSearchRequest.Builder builder = NoteSearchRequest.builder()
            .withStartDate(MARCH_1_2024)
            .withEndDate(MARCH_15_2024);

        // WHEN we pass a blank string and a null string:
        builder.withStartDate("   ");
        builder.withEndDate((String)null); // cast disambiguates from the YMDDate overload
        NoteSearchRequest request = builder.build();

        // THEN both bounds should be cleared:
        assertNull(request.getStartDate());
        assertNull(request.getEndDate());
    }

    @Test
    public void builder_withValidDateString_shouldParseToEquivalentYMDDate() {
        // GIVEN a builder asked to parse a valid yyyy-MM-dd string:
        NoteSearchRequest request = NoteSearchRequest.builder().withStartDate("2024-03-01").build();

        // THEN it should equal the same date constructed as a YMDDate object:
        assertEquals(MARCH_1_2024, request.getStartDate());
    }

    @Test
    public void builder_allCriteria_build_shouldPreserveAllValues() {
        // GIVEN a fully populated builder:
        NoteSearchRequest request = NoteSearchRequest.builder()
            .withText("quick fox")
            .withTags("work", "project-alpha")
            .withStartDate(MARCH_1_2024)
            .withEndDate(MARCH_15_2024)
            .withLimit(7)
            .build();

        // THEN every value should round-trip:
        assertEquals("quick fox", request.getText());
        assertEquals(List.of("work", "project-alpha"), request.getTags());
        assertEquals(MARCH_1_2024, request.getStartDate());
        assertEquals(MARCH_15_2024, request.getEndDate());
        assertEquals(7, request.getLimit());
        assertFalse(request.isEmpty());
    }

    @Test
    public void builder_afterBuild_modifiesOriginalTagList_shouldNotAffectRequest() {
        // GIVEN a mutable list of tags:
        List<String> tagList = new ArrayList<>(List.of("work"));

        // WHEN we build a request from it and then mutate the original list:
        NoteSearchRequest request = NoteSearchRequest.builder().withTags(tagList).build();
        tagList.add("project-alpha");

        // THEN the request should still see only the original contents:
        assertEquals(List.of("work"), request.getTags());
    }

    @Test
    public void getTags_shouldReturnUnmodifiableList() {
        // GIVEN a request with tags:
        NoteSearchRequest request = NoteSearchRequest.builder().withTags("work").build();

        // WHEN the caller tries to modify the returned list,
        // THEN an UnsupportedOperationException should be thrown:
        assertThrows(UnsupportedOperationException.class, () -> request.getTags().add("extra"));
    }

    @Test
    public void isEmpty_withOnlyLimitSet_shouldBeTrue() {
        // GIVEN a request with a limit but no actual search criteria:
        NoteSearchRequest request = NoteSearchRequest.builder().withLimit(3).build();

        // THEN the limit should not count as a criterion:
        assertTrue(request.isEmpty());
    }

    // -----------------------------------------------------------------
    // toQuery()
    // -----------------------------------------------------------------

    @Test
    public void toQuery_emptyRequest_shouldProduceEmptyQuery() {
        // GIVEN an empty request:
        NoteSearchRequest request = NoteSearchRequest.builder().build();

        // WHEN we convert it to a Query, THEN the Query should have no filters:
        assertTrue(request.toQuery().isEmpty());
    }

    @Test
    public void toQuery_textOnly_shouldProduceSingleCaseInsensitiveTextFilter() {
        // GIVEN a request with only a text criterion:
        NoteSearchRequest request = NoteSearchRequest.builder().withText("quick fox").build();

        // WHEN we convert it to a Query:
        Query query = request.toQuery();

        // THEN it should contain exactly one case-insensitive TextFilter:
        assertEquals(1, query.size());
        TextFilter textFilter = assertInstanceOf(TextFilter.class, query.getFilters().get(0));
        assertEquals("quick fox", textFilter.getContains());
        assertFalse(textFilter.isCaseSensitive());
    }

    @Test
    public void toQuery_tagsOnly_shouldProduceSingleAllTagFilterWithNormalizedTags() {
        // GIVEN a request with two non-normalized tags:
        NoteSearchRequest request = NoteSearchRequest.builder().withTags("Work", "Project-Alpha").build();

        // WHEN we convert it to a Query:
        Query query = request.toQuery();

        // THEN it should contain exactly one TagFilter of type ALL with the normalized tag names:
        assertEquals(1, query.size());
        TagFilter tagFilter = assertInstanceOf(TagFilter.class, query.getFilters().get(0));
        assertEquals(TagFilter.FilterType.ALL, tagFilter.getFilterType());
        assertEquals(Set.of("work", "project-alpha"), tagNames(tagFilter));
    }

    @Test
    public void toQuery_dateRange_shouldProduceInclusiveDateFilters() {
        // GIVEN a request with a date range:
        NoteSearchRequest request = NoteSearchRequest.builder()
            .withStartDate(MARCH_1_2024)
            .withEndDate(MARCH_15_2024)
            .build();

        // WHEN we convert it to a Query:
        Query query = request.toQuery();

        // THEN the start bound should be AFTER_INCLUSIVE and the end bound BEFORE_INCLUSIVE,
        // so that notes dated exactly on either bound are included:
        assertEquals(2, query.size());
        assertEquals(DateFilterType.AFTER_INCLUSIVE, findDateFilter(query, MARCH_1_2024).getFilterType());
        assertEquals(DateFilterType.BEFORE_INCLUSIVE, findDateFilter(query, MARCH_15_2024).getFilterType());
    }

    @Test
    public void toQuery_allCriteria_shouldProduceOneFilterPerCriterion() {
        // GIVEN a fully populated request:
        NoteSearchRequest request = NoteSearchRequest.builder()
            .withText("fox")
            .withTags("work")
            .withStartDate(MARCH_1_2024)
            .withEndDate(MARCH_15_2024)
            .build();

        // WHEN we convert it to a Query, THEN it should have one filter per criterion
        // (two date filters, one for each bound):
        Query query = request.toQuery();
        assertEquals(4, query.size());
        assertEquals(1, query.getFilters().stream().filter(f -> f instanceof TextFilter).count());
        assertEquals(1, query.getFilters().stream().filter(f -> f instanceof TagFilter).count());
        assertEquals(2, query.getFilters().stream().filter(f -> f instanceof DateFilter).count());
    }

    @Test
    public void toQuery_dateLikeTagString_shouldProduceDateTag() {
        // GIVEN a request with one date-formatted tag string and one regular tag:
        NoteSearchRequest request = NoteSearchRequest.builder()
            .withTags("2024-03-01", "work")
            .build();

        // WHEN we convert it to a Query:
        Query query = request.toQuery();

        // THEN the TagFilter should contain a DateTag for the date string.
        // This preserves the simple-search UI behavior, where date-looking tag
        // strings are treated as date tags:
        assertEquals(1, query.size());
        TagFilter tagFilter = assertInstanceOf(TagFilter.class, query.getFilters().get(0));
        List<Tag> tags = tagFilter.getTagsToFilter();
        assertEquals(2, tags.size());
        DateTag dateTag = tags.stream()
            .filter(DateTag.class::isInstance)
            .map(DateTag.class::cast)
            .findFirst()
            .orElseThrow();
        assertEquals(MARCH_1_2024, dateTag.getDate());
        assertTrue(tags.stream().anyMatch(t -> "work".equals(t.getTag())));
    }

    // -----------------------------------------------------------------
    // Equality
    // -----------------------------------------------------------------

    @Test
    public void equals_identicalRequests_shouldBeEqual() {
        // GIVEN two requests built with identical criteria:
        NoteSearchRequest first = NoteSearchRequest.builder()
            .withText("fox")
            .withTags("work")
            .withStartDate(MARCH_1_2024)
            .build();
        NoteSearchRequest second = NoteSearchRequest.builder()
            .withText("fox")
            .withTags("work")
            .withStartDate(MARCH_1_2024)
            .build();

        // THEN they should be equal and have equal hash codes:
        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
    }

    @Test
    public void equals_differentCriteria_shouldNotBeEqual() {
        // GIVEN a baseline request:
        NoteSearchRequest base = NoteSearchRequest.builder().withText("fox").build();

        // WHEN we vary each criterion in turn,
        // THEN none of the resulting requests should equal the baseline:
        assertNotEquals(base, NoteSearchRequest.builder().withText("dog").build());
        assertNotEquals(base, NoteSearchRequest.builder().withTags("work").build());
        assertNotEquals(base, NoteSearchRequest.builder().withStartDate(MARCH_1_2024).build());
        assertNotEquals(base, NoteSearchRequest.builder().withEndDate(MARCH_1_2024).build());
        assertNotEquals(base, NoteSearchRequest.builder().withText("fox").withLimit(3).build());
        assertNotEquals(base, null);
    }

    // -----------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------

    private static Set<String> tagNames(TagFilter filter) {
        return filter.getTagsToFilter().stream().map(Tag::getTag).collect(Collectors.toSet());
    }

    private static DateFilter findDateFilter(Query query, YMDDate date) {
        // Deliberately written in if-statement form: this JDK build's javac
        // (Temurin 25.0.3+9) fails to bind instanceof pattern variables when
        // the pattern appears in a local variable initializer or method call
        // argument. Statement-form instanceof is unaffected.
        for (Filter f : query.getFilters()) {
            if (f instanceof DateFilter && date.equals(((DateFilter)f).getTargetDate())) {
                return (DateFilter)f;
            }
        }
        throw new AssertionError("No DateFilter found for " + date);
    }
}
