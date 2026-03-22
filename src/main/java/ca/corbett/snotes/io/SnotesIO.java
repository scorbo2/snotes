package ca.corbett.snotes.io;

import ca.corbett.extras.io.FileSystemUtil;
import ca.corbett.snotes.model.Note;
import ca.corbett.snotes.model.Query;
import ca.corbett.snotes.model.Tag;
import ca.corbett.snotes.model.Template;
import ca.corbett.snotes.model.YMDDate;
import ca.corbett.snotes.model.filter.Filter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * An internal utility class for loading and saving model objects.
 * All methods in this class are package-private, and are not intended
 * to be invoked directly by application code outside of this package,
 * or from application extensions. Go through the DataManager class
 * instead, as it provides a layer of abstraction on top of the
 * actual IO, and because it handles logic around where the files
 * should live.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
class SnotesIO {

    private static final Logger log = Logger.getLogger(SnotesIO.class.getName());

    /**
     * Attempts to load a Query and its Filters from the given file,
     * which should be in the same format as produced by saveQuery().
     *
     * @param sourceFile Any query file that was generated via the saveQuery() method in this class.
     * @return A populated Query instance.
     * @throws IOException If the load fails.
     */
    static Query loadQuery(File sourceFile) throws IOException {
        if (sourceFile == null) {
            throw new IllegalArgumentException("sourceFile cannot be null");
        }
        if (!sourceFile.exists() || sourceFile.isDirectory() || !sourceFile.canRead()) {
            throw new IOException("Source file is not a readable file: " + sourceFile.getAbsolutePath());
        }

        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readTree(sourceFile);
        if (rootNode == null || rootNode.isNull() || rootNode.isMissingNode() || !rootNode.isObject()) {
            // This can happen with empty/blank files, JSON arrays, bare scalars, or other non-object content:
            throw new IOException("Failed to parse Query from file: " + sourceFile.getAbsolutePath());
        }

        Query query = new Query(); // Gets DEFAULT_NAME and an empty filter list by default

        // Set query name if present in the JSON:
        JsonNode nameNode = rootNode.get("name");
        if (nameNode != null && !nameNode.isNull() && nameNode.isTextual()) {
            String nameText = nameNode.asText();
            if (nameText != null && !nameText.trim().isEmpty()) {
                query.setName(nameText);
            }
        }

        // Load up filters if any are here:
        JsonNode filtersNode = rootNode.get("filters");
        if (filtersNode != null && filtersNode.isArray()) {
            for (JsonNode filterNode : filtersNode) {
                Filter filter = mapper.treeToValue(filterNode, Filter.class);
                query.addFilter(filter);
            }
        }

        return query;
    }

    /**
     * Attempts to persist the given Query and all of its Filters to the given file.
     * The save format is pretty-printed JSON.
     *
     * @param targetFile Any writable file. If the file already exists, it will be overwritten.
     * @throws IOException If the save fails.
     */
    static void saveQuery(Query query, File targetFile) throws IOException {
        if (query == null) {
            throw new IllegalArgumentException("query cannot be null");
        }
        if (targetFile == null) {
            throw new IllegalArgumentException("targetFile cannot be null");
        }
        if (targetFile.isDirectory() || (targetFile.exists() && !targetFile.canWrite())) {
            throw new IOException("Target file is not a writable file: " + targetFile.getAbsolutePath());
        }

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode rootNode = mapper.createObjectNode();
        rootNode.put("name", query.getName());

        ArrayNode filtersArray = mapper.createArrayNode();
        for (Filter filter : query.getFilters()) {
            filtersArray.add(mapper.valueToTree(filter));
        }
        rootNode.set("filters", filtersArray);

        mapper.writerWithDefaultPrettyPrinter().writeValue(targetFile, rootNode);
    }

    /**
     * Attempts to persist the given Template in pretty-printed JSON form to disk.
     *
     * @param targetFile Any writable file. Must not be null.
     * @throws IOException If the save fails for any reason, including if the target file is not writable.
     */
    static void saveTemplate(Template template, File targetFile) throws IOException {
        if (template == null) {
            throw new IllegalArgumentException("template cannot be null");
        }
        if (targetFile == null) {
            throw new IllegalArgumentException("targetFile cannot be null");
        }
        if (targetFile.isDirectory() || (targetFile.exists() && !targetFile.canWrite())) {
            throw new IOException("Target file is not a writable file: " + targetFile.getAbsolutePath());
        }

        // We use Jackson to build up the JSON and write it out:
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode rootNode = mapper.createObjectNode();
        rootNode.put("name", template.getName());
        rootNode.put("dateOption", template.getDateOption().name());
        rootNode.put("context", template.getContext().name());
        ArrayNode tagsArray = rootNode.putArray("tags");
        for (Tag tag : template.getTagList()) {
            tagsArray.add(tag.getTag()); // get raw tag without the '#' prefix for cleaner JSON
        }
        mapper.writerWithDefaultPrettyPrinter().writeValue(targetFile, rootNode);
    }

    /**
     * Attempts to load a Template from the given file, which should be in the same format as saved by saveTemplate().
     *
     * @param sourceFile Any file that was generated via the saveTemplate() method in this class. Must not be null.
     * @return A populated Template instance.
     * @throws IOException If the load fails for any reason, including if the source file does not exist/isn't readable.
     */
    static Template loadTemplate(File sourceFile) throws IOException {
        if (sourceFile == null) {
            throw new IllegalArgumentException("sourceFile cannot be null");
        }
        if (!sourceFile.exists() || !sourceFile.canRead()) {
            throw new IOException("Source file does not exist or is not readable: " + sourceFile.getAbsolutePath());
        }

        ObjectMapper mapper = new ObjectMapper();
        JsonNode parsedNode = mapper.readTree(sourceFile);
        if (parsedNode == null || parsedNode.isNull() || parsedNode.isMissingNode() || !parsedNode.isObject()) {
            // This can happen with empty/blank files, JSON arrays, bare scalars, or other non-object content:
            throw new IOException("Source file does not contain a valid JSON object: " + sourceFile.getAbsolutePath());
        }
        ObjectNode rootNode = (ObjectNode)parsedNode;

        // Set name if present:
        String name = Template.DEFAULT_NAME;
        JsonNode nameNode = rootNode.get("name");
        if (nameNode != null && !nameNode.isNull() && nameNode.isTextual()) {
            name = nameNode.asText();
            if (name.isBlank()) {
                log.warning("Template name is blank in file: "
                                + sourceFile.getAbsolutePath()
                                + ". Setting to default name.");
                name = Template.DEFAULT_NAME;
            }
        }

        // Try to parse out a date option:
        Template.DateOption dateOption = Template.DateOption.NONE;
        JsonNode dateNode = rootNode.get("dateOption");
        if (dateNode != null && !dateNode.isNull() && dateNode.isTextual()) {
            try {
                dateOption = Template.DateOption.valueOf(dateNode.asText());
            }
            catch (IllegalArgumentException iae) {
                log.warning("Invalid date option \"" + dateNode.asText() + "\" in template file: "
                                + sourceFile.getAbsolutePath()
                                + ". Setting to default (no date).");
            }
        }

        // Try to parse out a context option:
        Template.Context context = Template.Context.NONE;
        JsonNode contextNode = rootNode.get("context");
        if (contextNode != null && !contextNode.isNull() && contextNode.isTextual()) {
            try {
                context = Template.Context.valueOf(contextNode.asText());
            }
            catch (IllegalArgumentException iae) {
                log.warning("Invalid context option \"" + contextNode.asText() + "\" in template file: "
                                + sourceFile.getAbsolutePath()
                                + ". Setting to default (no context).");
            }
        }

        // Parse out tag list if present:
        List<String> tagList = new ArrayList<>();
        JsonNode tagsNode = rootNode.get("tags");
        if (tagsNode != null && !tagsNode.isNull() && tagsNode.isArray()) {
            for (JsonNode tagNode : tagsNode) {
                if (tagNode.isTextual()) {
                    tagList.add(tagNode.asText());
                }
                else {
                    log.warning("Invalid tag value in template file: "
                                    + sourceFile.getAbsolutePath()
                                    + ". Skipping invalid tag.");
                }
            }
        }

        Template template = new Template(name);
        template.setDateOption(dateOption);
        template.setContext(context);
        for (String tag : tagList) {
            template.addTag(tag);
        }
        return template;
    }

    /**
     * Attempts to load a Note from the given file, and will throw an IOException
     * if something goes wrong.
     *
     * @param file The file to load the Note from. Must be a readable file that exists on disk.
     * @return A Note object representing the content of the given file.
     * @throws IOException If anything at all goes wrong with the load.
     */
    static Note loadNote(File file) throws IOException {
        if (file == null || !file.exists() || !file.isFile() || !file.canRead()) {
            throw new IOException("File does not exist or is not a readable file.");
        }
        Note note = new Note();
        note.setSourceFile(file);
        List<String> lines = FileSystemUtil.readFileLines(file);
        if (lines.isEmpty()) {
            throw new IOException("File is empty.");
        }

        // The first line is the tag line:
        String tagLine = lines.get(0);
        if (!tagLine.contains("#")) {
            // This used to be considered a fatal error, but eh...
            // let's just log it as a warning. It's fine.
            log.warning("Note " + file.getAbsolutePath() + " has no tags.");
        }

        else {
            // Split the tags and load each one:
            String cleanedTagLine = tagLine.replaceAll("#", "").trim();
            if (!cleanedTagLine.isEmpty()) {
                String[] tags = cleanedTagLine.split("\\s+");
                int tagIndex = 0;
                if (tags.length > 0 && YMDDate.isValidYMD(tags[0])) { // If there's a date tag, it'll be the first one
                    tagIndex = 1;
                    note.setDate(new YMDDate(tags[0]));
                }
                for (int i = tagIndex; i < tags.length; i++) {
                    if (YMDDate.isValidYMD(tags[i])) {
                        // This makes no sense and is certainly an error:
                        throw new IOException("Multiple date tags found in Note " + file.getAbsolutePath());
                    }
                    note.tag(tags[i]);
                }
            }
            else {
                log.warning("Note " + file.getAbsolutePath() + " has no parsable tags.");
            }
        }

        if (lines.size() < 2) {
            // No content, but that's fine - we'll just return an empty Note.
            note.markClean();
            return note;
        }

        int lineIndex = 1;
        if (lines.get(1).trim().isEmpty()) {
            lineIndex++; // skip first blank line.
        }

        for (int i = lineIndex; i < lines.size(); i++) {
            note.append(lines.get(i));
            note.newline();
        }

        note.markClean(); // ignore all those changes we just made to the model object - it's in sync with disk.
        return note;
    }

    /**
     * Attempts to save the given Note to disk, and will throw an IOException if something goes wrong.
     *
     * @param note The Note to save. Must not be null, and must have a non-null source file.
     * @throws IOException If anything at all goes wrong with the save.
     */
    static void saveNote(Note note) throws IOException {
        if (note == null) {
            throw new IllegalArgumentException("Cannot save a null Note.");
        }
        if (note.getSourceFile() == null) {
            throw new IllegalArgumentException("Cannot save a Note with no source file.");
        }

        // TODO this is wrong!
        //      Dated notes require special handling, as they automatically go into folders
        //      based on the date, like 2021/01/09/note.txt
        //      If it's a first-time save, we have to compute that path.
        //      If it's an existing note, we have to check to see if the location is still good, and update it if not.
        //      Undated notes typically go into a "static" or "undated" folder, but we are not yet
        //      exposing that config property.
        //      All Notes, dated or not, get an automatic filename based on their tag list.
        //      Need to circle back to this in a future ticket! We're not handling any of it right now!
        //      And don't forget the "append if existing" option from the original application!
        //      e.g. create a new dated note with a tag of "tag" and save it when one already exists for that date...
        //           user should be prompted to append to or overwrite the existing note, and the code should be
        //           smart enough to do the right thing.
        //      The code for all of the above exists in the old Mercurial repo. Dig it up and reuse it!

        List<String> lines = new ArrayList<>();
        lines.add(note.getPersistenceTagLine());
        lines.add(""); // blank line between tags and text is conventional.

        // We'll preserve whatever line separators are in the Note's text.
        // This might be none at all if line-wrapping is enabled in the editor, but that's fine.
        lines.add(note.getText());

        // Write it:
        FileSystemUtil.writeLinesToFile(lines, note.getSourceFile());

        // If we make it this far, the Note is clean:
        note.markClean();
    }
}
