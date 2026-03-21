package ca.corbett.snotes.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
    }

    public String getName() {
        return name;
    }

    /**
     * Sets a human-presentable name for this Template. The default name is "Untitled Template".
     * No uniqueness check is done here, but you may receive an exception when trying to save
     * the note if the name is not unique. (TODO that's goofy - uniqueness should be enforced here).
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
    }

    /**
     * Removes all tags from this Template.
     */
    public void clearTags() {
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
     * Attempts to persist this Template in pretty-printed JSON form to disk.
     * The file will be created if it does not already exist, and overwritten if it does.
     * TODO the file location should be automatic, based on our name and some global save directory.
     *
     * @param targetFile Any writable file. Must not be null.
     * @throws IOException If the save fails for any reason, including if the target file is not writable.
     */
    public void save(File targetFile) throws IOException {
        if (targetFile == null) {
            throw new IllegalArgumentException("targetFile cannot be null");
        }
        if (targetFile.isDirectory() || (targetFile.exists() && !targetFile.canWrite())) {
            throw new IOException("Target file is not a writable file: " + targetFile.getAbsolutePath());
        }

        // We use Jackson to build up the JSON and write it out:
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode rootNode = mapper.createObjectNode();
        rootNode.put("name", name);
        rootNode.put("dateOption", dateOption.name());
        rootNode.put("context", context.name());
        ArrayNode tagsArray = rootNode.putArray("tags");
        for (Tag tag : tagList) {
            tagsArray.add(tag.getTag()); // get raw tag without the '#' prefix for cleaner JSON
        }
        mapper.writerWithDefaultPrettyPrinter().writeValue(targetFile, rootNode);
    }

    /**
     * Attempts to load a Template from the given file, which should be in the same format as produced by save().
     * TODO this should maybe be "loadAll" with no args, and we figure out how many templates are saved there.
     *
     * @param sourceFile Any file that was generated via the save() method in this class. Must not be null.
     * @return A populated Template instance.
     * @throws IOException If the load fails for any reason, including if the source file does not exist/isn't readable.
     */
    public static Template load(File sourceFile) throws IOException {
        if (sourceFile == null) {
            throw new IllegalArgumentException("sourceFile cannot be null");
        }
        if (!sourceFile.exists() || !sourceFile.canRead()) {
            throw new IOException("Source file does not exist or is not readable: " + sourceFile.getAbsolutePath());
        }

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode rootNode = (ObjectNode)mapper.readTree(sourceFile);
        Template template = new Template(rootNode.get("name").asText());
        template.setDateOption(DateOption.valueOf(rootNode.get("dateOption").asText()));
        template.setContext(Context.valueOf(rootNode.get("context").asText()));
        ArrayNode tagsArray = (ArrayNode)rootNode.get("tags");
        for (int i = 0; i < tagsArray.size(); i++) {
            template.addTag(tagsArray.get(i).asText());
        }
        return template;
    }
}
