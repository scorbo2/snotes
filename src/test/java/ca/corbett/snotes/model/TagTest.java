package ca.corbett.snotes.model;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TagTest {

    @Test
    public void constructor_withValidValue_shouldReturnAsIs() {
        // GIVEN a perfectly valid tag value:
        final String input = "valid_tag";

        // WHEN we pass it to the constructor:
        Tag actual = new Tag(input);

        // THEN the tag value should be identical to the input:
        assertEquals(input, actual.getTag());
    }

    @Test
    public void constructor_withLeadingTrailingWhitespace_shouldTrim() {
        // GIVEN a tag value with leading and trailing whitespace:
        final String input = "  valid_tag  ";

        // WHEN we pass it to the constructor:
        Tag actual = new Tag(input);

        // THEN the tag value should be trimmed of whitespace:
        assertEquals("valid_tag", actual.getTag());
    }

    @Test
    public void constructor_withIllegalCharacters_shouldReplaceWithUnderscores() {
        // GIVEN a tag value with spaces and slashes:
        final String input = "  invalid tag/with\\illegal characters  ";

        // WHEN we pass it to the constructor:
        Tag actual = new Tag(input);

        // THEN the tag value should have spaces and slashes replaced with underscores:
        assertEquals("invalid_tag_with_illegal_characters", actual.getTag());
    }

    @Test
    public void constructor_withNullOrEmptyValue_shouldThrow() {
        // GIVEN a null tag value:
        final String input = null;

        // WHEN we pass it to the constructor, THEN it should throw an IllegalArgumentException:
        assertThrows(IllegalArgumentException.class, () -> new Tag(input));
    }

    @Test
    public void constructor_withEmptyString_shouldThrow() {
        // GIVEN an empty tag value:
        final String input = "   ";

        // WHEN we pass it to the constructor, THEN it should throw an IllegalArgumentException:
        assertThrows(IllegalArgumentException.class, () -> new Tag(input));
    }

    @Test
    public void compareTo_withMultipleTags_shouldSort() {
        // GIVEN a list of tags in random order:
        Tag tag1 = new Tag("zzz_last");
        Tag tag2 = new Tag("eee_middle");
        Tag tag3 = new Tag("aaa_first");
        Tag tag4 = new Tag("ggg_random");

        // WHEN we sort them:
        List<Tag> tags = Arrays.asList(tag1, tag2, tag3, tag4);
        Collections.sort(tags);

        // THEN they should be sorted alphabetically by their tag value:
        assertEquals("aaa_first", tags.get(0).getTag());
        assertEquals("eee_middle", tags.get(1).getTag());
        assertEquals("ggg_random", tags.get(2).getTag());
        assertEquals("zzz_last", tags.get(3).getTag());
    }

    @Test
    public void toString_withValidTag_shouldReturnHashPrefixed() {
        // GIVEN a valid tag:
        Tag tag = new Tag("example");

        // WHEN we call toString():
        String actual = tag.toString();

        // THEN it should return the tag value prefixed with a hash sign:
        assertEquals("#example", actual);
    }
}