package ca.corbett.snotes.model.filter;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TagFilterTest extends FilterTest {

    @Test
    public void constructor_withNullOrEmptyParameters_shouldThrow() {
        // WHEN we give garbage to the constructor, THEN it should immediately throw:
        assertThrows(IllegalArgumentException.class, () -> new TagFilter(null, TagFilter.FilterType.ALL));
        assertThrows(IllegalArgumentException.class, () -> new TagFilter(List.of(), TagFilter.FilterType.ALL));
        assertThrows(IllegalArgumentException.class, () -> new TagFilter(List.of(NON_DATE_TAG1), null));
    }

    @Test
    public void isFiltered_ALLwithNoTags_shouldFilter() {
        // GIVEN a TagFilter with ALL type and no tags:
        TagFilter filter = new TagFilter(List.of(NON_DATE_TAG1, NON_DATE_TAG2), TagFilter.FilterType.ALL);

        // WHEN we try to filter a Note with no tags:
        boolean actual = filter.isFiltered(NOTE_UNDATED_UNTAGGED);

        // THEN it should be filtered, because the Note does not have all of the required tags:
        assertTrue(actual);
    }

    @Test
    public void isFiltered_ALLwithSomeTags_shouldFilter() {
        // GIVEN a TagFilter with ALL type and some tags:
        TagFilter filter = new TagFilter(List.of(NON_DATE_TAG1, NON_DATE_TAG2), TagFilter.FilterType.ALL);

        // WHEN we try to filter a Note with only one of the required tags:
        boolean actual = filter.isFiltered(NOTE_DATED_TAGGED);

        // THEN it should be filtered, because the Note does not have all of the required tags:
        assertTrue(actual);
    }

    @Test
    public void isFiltered_ALLwithAllTags_shouldNotFilter() {
        // GIVEN a TagFilter with ALL type and some tags:
        TagFilter filter = new TagFilter(List.of(NON_DATE_TAG1, NON_DATE_TAG2), TagFilter.FilterType.ALL);

        // WHEN we try to filter a Note with all of the required tags:
        boolean actual = filter.isFiltered(NOTE_MULTIPLE_TAGS);

        // THEN it should not be filtered, because the Note has all of the required tags:
        assertFalse(actual);
    }

    @Test
    public void isFiltered_ANYwithNoTags_shouldFilter() {
        // GIVEN a TagFilter with ANY type and no tags:
        TagFilter filter = new TagFilter(List.of(NON_DATE_TAG1, NON_DATE_TAG2), TagFilter.FilterType.ANY);

        // WHEN we try to filter a Note with no tags:
        boolean actual = filter.isFiltered(NOTE_UNDATED_UNTAGGED);

        // THEN it should be filtered, because the Note does not have any of the required tags:
        assertTrue(actual);
    }

    @Test
    public void isFiltered_ANYwithSomeTags_shouldNotFilter() {
        // GIVEN a TagFilter with ANY type and some tags:
        TagFilter filter = new TagFilter(List.of(NON_DATE_TAG1, NON_DATE_TAG2), TagFilter.FilterType.ANY);

        // WHEN we try to filter a Note with only one of the required tags:
        boolean actual = filter.isFiltered(NOTE_DATED_TAGGED);

        // THEN it should not be filtered, because the Note has at least one of the required tags:
        assertFalse(actual);
    }

    @Test
    public void isFiltered_ANYwithAllTags_shouldNotFilter() {
        // GIVEN a TagFilter with ANY type and some tags:
        TagFilter filter = new TagFilter(List.of(NON_DATE_TAG1, NON_DATE_TAG2), TagFilter.FilterType.ANY);

        // WHEN we try to filter a Note with all of the required tags:
        boolean actual = filter.isFiltered(NOTE_MULTIPLE_TAGS);

        // THEN it should not be filtered, because the Note has at least one of the required tags:
        assertFalse(actual);
    }

    @Test
    public void isFiltered_NONEwithNoTags_shouldNotFilter() {
        // GIVEN a TagFilter with NONE type and no tags:
        TagFilter filter = new TagFilter(List.of(NON_DATE_TAG1, NON_DATE_TAG2), TagFilter.FilterType.NONE);

        // WHEN we try to filter a Note with no tags:
        boolean actual = filter.isFiltered(NOTE_UNDATED_UNTAGGED);

        // THEN it should not be filtered, because the Note does not have any of the forbidden tags:
        assertFalse(actual);
    }

    @Test
    public void isFiltered_NONEwithSomeTags_shouldFilter() {
        // GIVEN a TagFilter with NONE type and some tags:
        TagFilter filter = new TagFilter(List.of(NON_DATE_TAG1, NON_DATE_TAG2), TagFilter.FilterType.NONE);

        // WHEN we try to filter a Note with only one of the forbidden tags:
        boolean actual = filter.isFiltered(NOTE_DATED_TAGGED);

        // THEN it should be filtered, because the Note has at least one of the forbidden tags:
        assertTrue(actual);
    }

    @Test
    public void isFiltered_NONEwithAllTags_shouldFilter() {
        // GIVEN a TagFilter with NONE type and some tags:
        TagFilter filter = new TagFilter(List.of(NON_DATE_TAG1, NON_DATE_TAG2), TagFilter.FilterType.NONE);

        // WHEN we try to filter a Note with all of the forbidden tags:
        boolean actual = filter.isFiltered(NOTE_MULTIPLE_TAGS);

        // THEN it should be filtered, because the Note has at least one of the forbidden tags:
        assertTrue(actual);
    }
}