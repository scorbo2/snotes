package ca.corbett.snotes.model;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * A Template is a way to create a new Note with some metadata already filled in.
 * It is essentially a "preset" for creating new Notes.
 * <ul>
 *     <li>Templates have a unique, user-presentable name (default "Untitled Template").</li>
 *     <li>Templates can specify a date option that will be applied to the new Note. Possible
 *     values are "No date", "Yesterday", "Today", and "Tomorrow". The last three are dynamic,
 *     and will be computed at the time you create a new Note.</li>
 *     <li>You can optionally specify "context" for the new Note. If set to any option
 *     other than "No context", the specified number of most recent Notes with the same tags
 *     as this Template will be retrieved and displayed in a read-only preview alongside
 *     the edit window for the new Note. This provides you with context when writing a new
 *     note on a given subject.</li>
 *     <li>Templates can specify a list of tags to be applied automatically to the new Note.</li>
 * </ul>
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 * @since Snotes 2.0
 */
public class Template {

    private static final Logger log = Logger.getLogger(Template.class.getName());

    public static final String DEFAULT_NAME = "Untitled Template";
    public static final int NAME_LENGTH_LIMIT = 25;

    /**
     * For pre-filling the date field of a new Note. The dynamic options (Yesterday, Today, Tomorrow)
     * will be computed at the time the new Note is created.
     */
    public enum DateOption {
        NONE("No date"),
        YESTERDAY("Yesterday"),
        TODAY("Today"),
        TOMORROW("Tomorrow");

        private final String label;

        DateOption(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    /**
     * For optionally retrieving and displaying context in the edit window when
     * writing a new Note. The Query for retrieving context will be generated
     * at the time the Note is created, and is driven from the tags specified
     * in this Template. If no tags are specified in this Template, then the
     * most recent Notes with any tags will be retrieved.
     */
    public enum Context {
        NONE("No context"),
        ALL("All"),
        MOST_RECENT1("1 most recent"),
        MOST_RECENT3("3 most recent"),
        MOST_RECENT5("5 most recent"),
        MOST_RECENT10("10 most recent");

        private final String label;

        Context(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private String name;
    private DateOption dateOption;
    private Context context;
    private final List<Tag> tagList;
    private File sourceFile;
    private boolean isDirty;

    /**
     * Creates a new, empty, unnamed Template.
     */
    public Template() {
        this(DEFAULT_NAME);
    }

    /**
     * Creates a new, empty Template with the given name.
     *
     * @param name The user-presentable name for this Template. Will be truncated if too long. Must not be null/blank.
     */
    public Template(String name) {
        setName(name);
        this.dateOption = DateOption.NONE;
        this.context = Context.NONE;
        this.tagList = new ArrayList<>();
        this.sourceFile = null;
        isDirty = true;
    }

    public String getName() {
        return name;
    }

    /**
     * Sets a human-presentable name for this Template. The default name is "Untitled Template".
     * The name should be unique. Having multiple Templates with the same name will cause
     * trouble when saving them later.
     * <p>
     * If the given name is too long, it will be truncated to NAME_LENGTH_LIMIT characters.
     * If it is null or blank, the name will be set to DEFAULT_NAME.
     * </p>
     *
     * @param name A user-presentable name for this Template. Must not be null.
     */
    public void setName(String name) {
        if (name == null) {
            throw new IllegalArgumentException("name cannot be null");
        }
        if (name.length() > NAME_LENGTH_LIMIT) {
            name = name.substring(0, NAME_LENGTH_LIMIT);
        }
        this.name = name.isBlank() ? DEFAULT_NAME : name;
        isDirty = true;
    }

    /**
     * Returns the File from which this Query was loaded, or null if this Query has not yet been saved to disk.
     */
    public File getSourceFile() {
        return sourceFile;
    }

    /**
     * Sets the source File for this Template. Replaces any previous value.
     * This should generally only be called from DataManager - calling it directly may
     * result in the old file being left on disk.
     *
     * @param sourceFile The new sourceFile for this Template.
     */
    public void setSourceFile(File sourceFile) {
        this.sourceFile = sourceFile;
        isDirty = true;
    }

    public DateOption getDateOption() {
        return dateOption;
    }

    /**
     * Sets an optional date pre-fill for new Notes created from this Template. The default is "No date".
     *
     * @param dateOption One of the DateOption enum values. Must not be null.
     */
    public void setDateOption(DateOption dateOption) {
        if (dateOption == null) {
            throw new IllegalArgumentException("dateOption cannot be null");
        }
        this.dateOption = dateOption;
        isDirty = true;
    }

    public Context getContext() {
        return context;
    }

    /**
     * Sets an optional Context option for this Template. The default is "No context".
     * If set to any option other than "No context", the specified number of most recent
     * Notes with the same tags as this Template will be retrieved and displayed in a
     * read-only preview alongside the edit window for new Notes created from this Template.
     * This provides you with context when writing a new note on a given subject.
     *
     * @param context One of the Context enum values. Must not be null.
     */
    public void setContext(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("context cannot be null");
        }
        this.context = context;
        isDirty = true;
    }

    /**
     * Adds the given tag to the list of tags that will be applied to new Notes created from this Template.
     * The given value will be normalized according to the rules specified in the Tag class.
     *
     * @param tag Any string value, but must not be null or blank.
     */
    public void addTag(String tag) {
        if (tag == null || tag.isBlank()) {
            throw new IllegalArgumentException("tag cannot be null or blank");
        }
        tagList.add(new Tag(tag));
        isDirty = true;
    }

    /**
     * Removes all tags from this Template.
     */
    public void clearTags() {
        if (!tagList.isEmpty()) {
            isDirty = true;
        }
        tagList.clear();
    }

    /**
     * Returns a copy of the tag list in this Template.
     * Modifying the returned list will not affect the contents of this Template.
     */
    public List<Tag> getTagList() {
        return new ArrayList<>(tagList);
    }

    /**
     * Reports whether this Template has unsaved changes.
     */
    public boolean isDirty() {
        return isDirty;
    }

    /**
     * Marks this Template as clean, meaning that it has no unsaved changes.
     */
    public void markClean() {
        isDirty = false;
    }
}
