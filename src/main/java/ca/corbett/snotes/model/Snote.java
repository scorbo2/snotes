package ca.corbett.snotes.model;

import java.io.File;
import java.util.List;

/**
 * Represents a combination of some text and a list of tags that categorize that text.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 * @since Snotes 1.0
 */
public final class Snote {

    private final TagList tagList;
    private String text;
    private File sourceFile;

    /**
     * Creates an empty, untagged Snote.
     */
    public Snote() {
        tagList = new TagList();
        text = "";
        sourceFile = null;
    }

    /**
     * This is intended for classes in the io package - other callers can ignore this.
     *
     * @param src The File from which this Snote was loaded, if applicable.
     */
    public void setSourceFile(File src) {
        sourceFile = src;
    }

    /**
     * This is intended for classes in the io package - other callers can ignore this.
     *
     * @return The File from which this Snote was loaded, or null.
     */
    public File getSourceFile() {
        return sourceFile;
    }

    /**
     * Indicates whether this Snote has a date tag associated with it or not.
     *
     * @return True if a date tag has been assigned to this Snote.
     */
    public boolean hasDate() {
        return tagList.hasDate();
    }

    /**
     * Returns the YMDDate associated with this Snote, if any.
     *
     * @return A YMDDate object, or null if this Snote is not dated.
     */
    public YMDDate getDate() {
        return tagList.hasDate() ? tagList.getDateTag().getDate() : null;
    }

    /**
     * Sets the YMDDate for this Snote, and replaces any previous date that was set.
     * Passing null will convert this Snote to an undated one.
     *
     * @param date The new YMDDate for this Snote, or null for no date.
     */
    public void setDate(YMDDate date) {
        tagList.setDate(date);
    }

    /**
     * Replaces any existing text for this Snote with the given text.
     *
     * @param newText The new text value for this Snote.
     */
    public void setText(String newText) {
        text = newText == null ? "" : newText;
    }

    /**
     * Appends the given text to the existing text of this Snote.
     * Does not automatically add a newline - use newline() or embed
     * newline characters in newText.
     *
     * @param newText The text to append to this Snote.
     */
    public void append(String newText) {
        text += newText;
    }

    /**
     * Appends a System-specific newline character to this Snote.
     */
    public void newline() {
        text += System.lineSeparator();
    }

    /**
     * Indicates whether any text has been set for this Snote.
     *
     * @return True if any text exists here.
     */
    public boolean hasText() {
        return (text != null) && (!text.trim().isEmpty());
    }

    /**
     * Returns the current text value of this Snote. This does NOT include the
     * tag line at the beginning - only the actual text content is returned.
     * You may want getFullContent() instead.
     *
     * @return The text of this Snote.
     */
    public String getText() {
        return text;
    }

    /**
     * Returns the tag line for this Snote as written in the file.
     *
     * @return A human readable tag line for this Snote.
     */
    public String getTagLine() {
        StringBuilder builder = new StringBuilder();
        if (hasDate()) {
            builder.append("#");
            builder.append(tagList.getDateTag().getDate().toString());
            builder.append(" (");
            builder.append(tagList.getDateTag().getDate().getDayName());
            builder.append(")");

            if (tagList.size() != 0) {
                builder.append(" ");
            }
        }
        for (Tag tag : tagList.getTags()) {
            if (tag instanceof DateTag) {
                continue;
            }
            builder.append("#");
            builder.append(tag.getTag());
            builder.append(" ");
        }
        builder.append(System.lineSeparator());
        return builder.toString();
    }

    /**
     * Returns the full content of this Snote - that is, the tag line followed by a blank
     * line followed by the text content of this Snote. This call is equivalent to
     * the following:
     * <p>
     * getTagLine() + System.lineSeparator() + getText()
     * </p>
     *
     * @return The full text content of this Snote as described above.
     */
    public String getFullContent() {
        return getTagLine() + System.lineSeparator() + getText();
    }

    /**
     * Adds the specified Tag to this Snote. Duplicates are ignored.
     * If the given Tag is a DateTag, this is equivalent to calling setDate()
     *
     * @param tag The Tag to add.
     */
    public void tag(Tag tag) {
        tagList.addTag(tag);
    }

    /**
     * Adds the specified tag value to this Snote. Duplicate tags are ignored.
     *
     * @param tag The tag value to add.
     */
    public void tag(String tag) {
        tagList.addTag(tag);
    }

    /**
     * Removes the given tag from this Snote, if present.
     *
     * @param tag The tag to remove.
     */
    public void untag(String tag) {
        tagList.removeTag(tag);
    }

    /**
     * Removes the given tag from this Snote, if present.
     *
     * @param tag The tag to remove.
     */
    public void untag(Tag tag) {
        tagList.removeTag(tag);
    }

    /**
     * Indicates whether or not the given tag value exists for this Snote.
     *
     * @param tag The tag value to look for.
     * @return True if the given tag value exists here.
     */
    public boolean hasTag(String tag) {
        return tagList.hasTag(tag);
    }

    /**
     * Indicates whether or not the given tag exists for this Snote.
     *
     * @param tag The Tag to look for.
     * @return True if the given Tag exists here.
     */
    public boolean hasTag(Tag tag) {
        return tagList.hasTag(tag);
    }

    /**
     * Returns a copy of the list of tags for this Snote. If you want to make
     * modifications, use the tag() and untag() methods.
     *
     * @return A copy of the list of tags for this Snote.
     */
    public List<Tag> getTags() {
        return tagList.getTags();
    }

    /**
     * Returns a list of all non-date tags for this Snote.
     */
    public List<Tag> getNonDateTags() {
        return tagList.getNonDateTags();
    }

}