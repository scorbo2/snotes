package ca.corbett.snotes.model;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents a combination of some text and a list of tags that categorize that text.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 * @since Snotes 1.0
 */
public final class Note {

    private static final Logger log = Logger.getLogger(Note.class.getName());

    private final TagList tagList;
    private String text;
    private File sourceFile;
    private boolean isDirty;

    /**
     * Creates an empty, untagged Note.
     */
    public Note() {
        tagList = new TagList();
        text = "";
        sourceFile = null;
        isDirty = true;
    }

    /**
     * The persistence file for this Note, or null if this Note has
     * not yet been saved to disk.
     *
     * @param src The File from which this Note was loaded, if applicable.
     */
    public void setSourceFile(File src) {
        sourceFile = src;
        isDirty = true;
    }

    /**
     * The persistence file for this Note, or null if this Note has
     * not yet been saved to disk.
     *
     * @return The File from which this Note was loaded, or null if not applicable.
     */
    public File getSourceFile() {
        return sourceFile;
    }

    /**
     * Indicates whether this Note has a date tag associated with it or not.
     *
     * @return True if a date tag has been assigned to this Note.
     */
    public boolean hasDate() {
        return tagList.hasDate();
    }

    /**
     * Returns the YMDDate associated with this Note, if any.
     *
     * @return A YMDDate object, or null if this Note is not dated.
     */
    public YMDDate getDate() {
        return tagList.hasDate() ? tagList.getDateTag().getDate() : null;
    }

    /**
     * Sets the YMDDate for this Note, and replaces any previous date that was set.
     * Passing null will convert this Note to an undated one.
     *
     * @param date The new YMDDate for this Note, or null for no date.
     * @return This Note, for chaining.
     */
    public Note setDate(YMDDate date) {
        tagList.setDate(date);
        isDirty = true;
        return this;
    }

    /**
     * Replaces any existing text for this Note with the given text.
     *
     * @param newText The new text value for this Note.
     * @return This Note, for chaining.
     */
    public Note setText(String newText) {
        text = newText == null ? "" : newText;
        isDirty = true;
        return this;
    }

    /**
     * Appends the given text to the existing text of this Note.
     * Does not automatically add a newline - use newline() or embed
     * newline characters in newText.
     *
     * @param newText The text to append to this Note.
     * @return This Note, for chaining.
     */
    public Note append(String newText) {
        text += newText;
        isDirty = true;
        return this;
    }

    /**
     * Appends a System-specific newline character to this Note.
     *
     * @return This Note, for chaining.
     */
    public Note newline() {
        text += System.lineSeparator();
        isDirty = true;
        return this;
    }

    /**
     * Indicates whether any text has been set for this Note.
     *
     * @return True if any text exists here.
     */
    public boolean hasText() {
        return text != null && !text.isBlank();
    }

    /**
     * Returns the current text value of this Note. This does NOT include the
     * tag line at the beginning - only the actual text content is returned.
     * You may want getFullContent() instead.
     *
     * @return The text of this Note.
     */
    public String getText() {
        return text;
    }

    /**
     * Returns the human-presentable tag line for this Note.
     *
     * @return A human-readable tag line for this Note.
     */
    public String getHumanTagLine() {
        return tagList.toString();
    }

    /**
     * Returns the persistable tag line for this Note.
     *
     * @return A machine-readable tag line for this Note.
     */
    public String getPersistenceTagLine() {
        return tagList.getPersistenceString();
    }

    /**
     * Returns the full content of this Note - that is, the human-readable tag line, followed by a blank
     * line, followed by the text content of this Note. This call is equivalent to
     * the following:
     * <p>
     * getTagLine() + System.lineSeparator() + getText()
     * </p>
     *
     * @return The full text content of this Note as described above.
     */
    public String getFullContent() {
        return getHumanTagLine() + System.lineSeparator() + getText();
    }

    /**
     * Adds the specified Tag to this Note, if not already present. Duplicates are ignored.
     * If the given Tag is a DateTag, this is equivalent to calling setDate()
     *
     * @param tag The Tag to add.
     * @return This Note, for chaining.
     */
    public Note tag(Tag tag) {
        tagList.addTag(tag);
        isDirty = true;
        return this;
    }

    /**
     * Adds the specified tag value to this Note, if not already present. Duplicate tags are ignored.
     *
     * @param tag The tag value to add.
     * @return This Note, for chaining.
     */
    public Note tag(String tag) {
        tagList.addTag(tag);
        isDirty = true;
        return this;
    }

    /**
     * Removes the given tag from this Note, if present.
     *
     * @param tag The tag to remove.
     * @return This Note, for chaining.
     */
    public Note untag(String tag) {
        if (tagList.removeTag(tag)) {
            isDirty = true;
        }
        return this;
    }

    /**
     * Removes the given tag from this Note, if present.
     *
     * @param tag The tag to remove.
     * @return This Note, for chaining.
     */
    public Note untag(Tag tag) {
        if (tagList.removeTag(tag)) {
            isDirty = true;
        }
        return this;
    }

    /**
     * Removes all tags, including the date tag if present, from this Note.
     */
    public void clearAllTags() {
        if (tagList.size() != 0) {
            tagList.clear();
            isDirty = true;
        }
    }

    /**
     * Indicates whether or not the given tag value exists for this Note.
     *
     * @param tag The tag value to look for.
     * @return True if the given tag value exists here.
     */
    public boolean hasTag(String tag) {
        return tagList.hasTag(tag);
    }

    /**
     * Indicates whether or not the given tag exists for this Note.
     *
     * @param tag The Tag to look for.
     * @return True if the given Tag exists here.
     */
    public boolean hasTag(Tag tag) {
        return tagList.hasTag(tag);
    }

    /**
     * Returns a copy of the list of tags for this Note. If you want to make
     * modifications, use the tag() and untag() methods.
     *
     * @return A copy of the list of tags for this Note.
     */
    public List<Tag> getTags() {
        return tagList.getTags();
    }

    /**
     * Returns a list of all non-date tags for this Note.
     * The returned list is a defensive copy - making changes
     * to it will not affect the tag list for this Note.
     */
    public List<Tag> getNonDateTags() {
        return tagList.getNonDateTags();
    }

    /**
     * Returns true if this Note contains changes that have not been persisted to disk.
     */
    public boolean isDirty() {
        return isDirty;
    }

    /**
     * Marks this Note as clean, indicating that all changes have been persisted to disk.
     */
    public void markClean() {
        isDirty = false;
    }

    /**
     * A comparison between Notes will focus on the date of each Note. If a Note
     * is dated, its date will be used for comparison. Otherwise, the last modified
     * time of its source file is used. Undated Notes with no source file are treated
     * as having a date of 0 (the epoch).
     *
     * @param other The Note to compare this Note to.
     * @return negative, zero, or positive as this Note is less than, equal to, or greater than the specified Note.
     */
    public int compareTo(Note other) {
        if (other == null) {
            return 1; // Non-null Notes are considered greater than null Notes
        }
        long thisTime = hasDate() ? getDate().toEpochMilli() : (getSourceFile() != null ? getSourceFile().lastModified() : 0);
        long otherTime = other.hasDate() ? other.getDate().toEpochMilli() : (other.getSourceFile() != null ? other
            .getSourceFile().lastModified() : 0);
        return Long.compare(thisTime, otherTime);
    }

    /**
     * Given a Note, and the given data directory,
     * this method will return the path of this note's source file relative to that
     * data directory. For example, if the data directory is "/home/user/snotes-data",
     * and this Note's source file is "/home/user/snotes-data/2024/06/15/note1.txt",
     * then this method will return "2024/06/15/note1.txt".
     * <p>
     * If the Note's source file is not located within the data directory,
     * this method will return the absolute path of the source file instead.
     * <p>
     * <p>
     * If the given Note is null, or has no source file, an empty string is returned.
     * </p>
     *
     * @param note Any Note object. If this is null, or if note.getSourceFile() is null, an empty string is returned.
     * @param dataDir The data directory to which the returned path should be relative, if applicable. This should not be null.
     * @return A relative path string for the given Note, if it is within our data directory. Full path otherwise.
     */
    public static String getRelativePath(Note note, File dataDir) {
        if (dataDir == null) {
            throw new IllegalArgumentException("dataDir cannot be null");
        }
        if (note == null || note.getSourceFile() == null) {
            return "";
        }
        try {
            Path dataDirPath = dataDir.toPath().toAbsolutePath().normalize();
            Path sourceFilePath = note.getSourceFile().toPath().toAbsolutePath().normalize();
            if (sourceFilePath.startsWith(dataDirPath)) {
                return dataDirPath.relativize(sourceFilePath).toString();
            }
            else {
                return sourceFilePath.toString();
            }
        }
        catch (Exception e) {
            log.log(Level.WARNING, "getRelativePath: could not resolve paths", e);
            return "";
        }
    }
}