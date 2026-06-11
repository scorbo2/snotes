package ca.corbett.snotes.model;

/**
 * Represents a tag along with the number of non-scratch notes that use it.
 * Used by {@link ca.corbett.snotes.io.DataManager#getUniqueTags()} to provide
 * tag usage statistics for the TagListDialog.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 * @since Snotes 2.2
 */
public record TagUsage(Tag tag, int count) {
}
