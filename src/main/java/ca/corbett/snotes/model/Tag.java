package ca.corbett.snotes.model;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * A Tag is some string label that is attached to a Snote to categorize it.
 * Tags cannot contain spaces or certain other punctuation characters.
 * Tags are always converted to all lower case.
 * Leading and trailing spaces are trimmed.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 * @Since Snotes 1.0
 */
public class Tag implements Comparable {

    private static final String DEFAULT_TAG = "untagged"; // TODO why
    private static final Pattern DATE_PATTERN = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");

    protected final String tag;

    /**
     * Instantiate by providing any string value. The given value will be validated and
     * possibly adjusted to substitute invalid characters.
     *
     * @param tag The String value for this Tag.
     */
    public Tag(String tag) {
        this.tag = validateTag(tag);
    }

    /**
     * Returns the Tag value as a String.
     *
     * @return The Tag value.
     */
    public String getTag() {
        return tag;
    }

    /**
     * Returns the Tag value in a Snotes-friendly format. This is a hash sign followed
     * by the Tag string value.
     *
     * @return The Tag value in a Snotes-ready format (eg. "#tagvalue")
     */
    @Override
    public String toString() {
        return "#" + tag;
    }

    /**
     * Performs validation on the input tag value and returns a sanitized version of it.
     * "Sanitized" here means that the tag is unconditionally lower-cased, trimmed, and
     * has spaces and slashes replaced with underscores. If the input is null or
     * empty (after trimming), the default tag value is returned.
     *
     * @param s The candidate tag value.
     * @return The sanitized tag value.
     */
    protected String validateTag(String s) {
        if (s == null || s.trim().isEmpty()) {
            return DEFAULT_TAG;
        }
        String validatedTag = s.toLowerCase().trim(); // Cannot have leading/trailing spaces
        return validatedTag.replaceAll(" ", "_") // Cannot contain spaces
                           .replaceAll("/", "_") // Cannot contain forward slashes
                           .replaceAll("\\\\", "_"); // Cannot contain backslashes
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Tag tag1 = (Tag)o;
        return Objects.equals(tag, tag1.tag);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(tag);
    }

    @Override
    public int compareTo(Object o) {
        if (o == null || !(o instanceof Tag)) {
            return 0;
        }

        return this.tag.compareTo(((Tag)o).tag);
    }

    /**
     * Checks the given string value to see if it fits a YMD date pattern (yyyy-mm-dd).
     *
     * @param value The candidate value
     * @return True if it looks like a date.
     */
    public static boolean isDateValue(String value) {
        return DATE_PATTERN.matcher(value).matches();
    }

}