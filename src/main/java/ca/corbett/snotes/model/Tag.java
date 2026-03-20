package ca.corbett.snotes.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Objects;

/**
 * A Tag is some string label that is attached to a Note, in order to categorize it.
 * Tags are primarily used for searching and filtering Notes.
 * <p>
 * Tags cannot contain spaces, forward slashes, or backslashes - these characters
 * are replaced with underscores. Tags are always converted to all lower case.
 * Leading and trailing spaces are trimmed. Tags always start with the "#" character,
 * and cannot contain that character.
 * </p>
 * <p>
 * Each Note can have zero or more Tags.
 * </p>
 * <p>
 * If a tag value fits the yyyy-MM-dd format, it is considered a "date" tag.
 * Each Note can have at most one date tag.
 * </p>
 * <p>
 * Tags are immutable.
 * </p>
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 * @since Snotes 1.0
 */
public class Tag implements Comparable<Tag> {

    protected final String tag;

    /**
     * Instantiate by providing any string value. The given value will be validated and
     * possibly adjusted to substitute invalid characters.
     *
     * @param tag The String value for this Tag.
     */
    @JsonCreator
    public Tag(String tag) {
        this.tag = validateTag(tag);
    }

    /**
     * Returns the raw Tag value as a String, NOT including the leading hash sign.
     * See toString() for the Snotes-friendly format of this Tag.
     *
     * @return The Tag value.
     */
    @JsonValue
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
     * has spaces, foreward/backslashes, and hash characters replaced with underscores.
     * If the input is null or blank, an IllegalArgumentException is thrown.
     * <p>
     * <b>Note</b>: the resulting tag value may not match what was passed in!
     * </p>
     * <pre>
     *     validateTag("Hello/World Tag ") -> "hello_world_tag"
     * </pre>
     *
     * @param s The candidate tag value.
     * @return The sanitized tag value.
     * @throws IllegalArgumentException if the input is null or blank.
     */
    protected String validateTag(String s) {
        if (s == null || s.isBlank()) {
            throw new IllegalArgumentException("Tag value cannot be null or blank.");
        }
        String validatedTag = s.toLowerCase().trim(); // Cannot have leading/trailing spaces
        return validatedTag.replaceAll(" ", "_") // Cannot contain spaces
                           .replaceAll("/", "_") // Cannot contain forward slashes
                           .replaceAll("\\\\", "_") // Cannot contain backslashes
                           .replaceAll("#", "_"); // Cannot contain hash signs
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) { return false; }
        Tag tag1 = (Tag)o;
        return Objects.equals(tag, tag1.tag);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(tag);
    }

    @Override
    public int compareTo(Tag other) {
        return this.tag.compareTo(other.tag);
    }
}