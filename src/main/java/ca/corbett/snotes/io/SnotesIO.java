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

        // Load order if present; default to 0 for older persisted Queries that lack the field:
        JsonNode orderNode = rootNode.get("order");
        if (orderNode != null && !orderNode.isNull() && orderNode.isNumber()) {
            query.setOrder(orderNode.asInt(0));
        }

        query.setSourceFile(sourceFile);
        query.markClean();
        return query;
    }

    /**
     * Attempts to persist the given Query and all of its Filters to the given file.
     * The save format is pretty-printed JSON. The name of the file is auto-computed
     * from the Query's name. This means that changing the name of a Query and then
     * saving it may move it to a new file. This method does NOT handle cleaning
     * the old file! It's up to the caller to manage the file move.
     * <p>
     * If the save succeeds, the Query's sourceFile is updated to the given targetFile, and the Query is marked clean.
     * </p>
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
        rootNode.put("order", query.getOrder());

        ArrayNode filtersArray = mapper.createArrayNode();
        for (Filter filter : query.getFilters()) {
            filtersArray.add(mapper.valueToTree(filter));
        }
        rootNode.set("filters", filtersArray);

        // The targetFile's path may not exist:
        File parentDir = targetFile.getParentFile();
        if (!parentDir.exists()) {
            if (!parentDir.mkdirs()) {
                throw new IOException("Failed to create directories for target file: " + targetFile.getAbsolutePath());
            }
        }

        mapper.writerWithDefaultPrettyPrinter().writeValue(targetFile, rootNode);

        // If we make it here, the Query is clean, and has a new source file:
        query.setSourceFile(targetFile);
        query.markClean();
    }

    /**
     * Attempts to persist the given Template in pretty-printed JSON form to disk.
     * The name of the file is auto-computed  from the Template's name. This means
     * that changing the name of a Template and then saving it may move it to a new file.
     * This method does NOT handle cleaning the old file! It's up to the caller to
     * manage the file move.
     * <p>
     * If the save succeeds, the Template's sourceFile is updated to the given targetFile, and it is marked clean.
     * </p>
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
        rootNode.put("order", template.getOrder());
        ArrayNode tagsArray = rootNode.putArray("tags");
        for (Tag tag : template.getTagList()) {
            tagsArray.add(tag.getTag()); // get raw tag without the '#' prefix for cleaner JSON
        }

        // The targetFile's path may not exist:
        File parentDir = targetFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            if (!parentDir.mkdirs()) {
                throw new IOException("Failed to create directories for target file: " + targetFile.getAbsolutePath());
            }
        }

        mapper.writerWithDefaultPrettyPrinter().writeValue(targetFile, rootNode);

        // If we make it here, the Template is clean, and it has a new source file:
        template.setSourceFile(targetFile);
        template.markClean();
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

        // Load order if present; default to 0 for older persisted Templates that lack the field:
        JsonNode orderNode = rootNode.get("order");
        if (orderNode != null && !orderNode.isNull() && orderNode.isNumber()) {
            template.setOrder(orderNode.asInt(0));
        }

        template.setSourceFile(sourceFile);
        template.markClean();
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
     * The sourceFile of the given Note is ignored in favor of the specified targetFile.
     * If the save succeeds, the Note's sourceFile is updated with this file. If the given targetFile
     * does not match the Note's existing sourceFile, this method does NOT handle cleaning up
     * the old file! It's up to the caller to handle the move.
     *
     * @param note The Note to save. Must not be null.
     * @param targetFile The save destination. Must not be null.
     * @throws IOException If anything at all goes wrong with the save.
     */
    static void saveNote(Note note, File targetFile) throws IOException {
        if (note == null) {
            throw new IllegalArgumentException("Cannot save a null Note.");
        }
        if (targetFile == null) {
            throw new IllegalArgumentException("Cannot save a Note with no target file.");
        }

        // The targetFile's path may not exist:
        File parentDir = targetFile.getParentFile();
        if (!parentDir.exists()) {
            if (!parentDir.mkdirs()) {
                throw new IOException("Failed to create directories for target file: " + targetFile.getAbsolutePath());
            }
        }

        List<String> lines = new ArrayList<>();
        lines.add(note.getPersistenceTagLine());
        lines.add(""); // blank line between tags and text is conventional.

        // We'll preserve whatever line separators are in the Note's text.
        // This might be none at all if line-wrapping is enabled in the editor, but that's fine.
        lines.add(note.getText());

        // Write it:
        FileSystemUtil.writeLinesToFile(lines, targetFile);

        // If we make it this far, the Note is clean, and has a new sourceFile:
        note.setSourceFile(targetFile);
        note.markClean();
    }

    /**
     * Returns true if the given file is located in the static directory,
     * which is where undated notes are stored by default.
     */
    static boolean isInStaticDir(File dataDirectory, File file) {
        if (file == null) {
            return false;
        }

        File staticDir = new File(dataDirectory, DataManager.STATIC_DIR);
        return file.getAbsolutePath().startsWith(staticDir.getAbsolutePath() + File.separator);
    }

    /**
     * Returns true if the given file is located in the scratch directory.
     */
    static boolean isInScratchDir(File dataDirectory, File file) {
        if (file == null) {
            return false;
        }

        File scratchDir = new File(dataDirectory, DataManager.SCRATCH_DIR);
        return file.getAbsolutePath().startsWith(scratchDir.getAbsolutePath() + File.separator);
    }

    /**
     * Automatically computes a suggested File for the given Note based
     * on its metadata, and based on the supplied data directory.
     *
     * @param dataDirectory the base directory where all Notes are stored. Must not be null.
     * @param note          Any non-null Note object.
     * @return A filename suitable for this Note.
     */
    static File computeFile(File dataDirectory, Note note) {
        if (dataDirectory == null) {
            throw new IllegalArgumentException("dataDirectory cannot be null");
        }
        if (note == null) {
            throw new IllegalArgumentException("note cannot be null");
        }

        // Dated notes are stored into a yyyy/mm/dd directory structure:
        String dirPath = dataDirectory.getAbsolutePath();
        if (note.hasDate()) {
            YMDDate date = note.getDate();
            dirPath += File.separator + date.getYearStr()
                + File.separator + date.getMonthStr()
                + File.separator + date.getDayStr();
        }

        // Undated notes go into the static directory by default.
        // One day the UI will allow this to be customized.
        else {
            // If this note has a source file that's already in the
            // static directory, we'll just save back to that file
            // instead of computing a location:
            if (note.getSourceFile() != null) {
                if (isInStaticDir(dataDirectory, note.getSourceFile())) {
                    return note.getSourceFile();
                }
                else {
                    // If we're not saving a scratch note, something is fishy...
                    if (!isInScratchDir(dataDirectory, note.getSourceFile())) {
                        // This could happen legitimately if the user edited a dated note and removed the date.
                        // It's worth logging a warning, as it's a bit unusual, and might not be intentional.
                        log.warning("Undated note has a source file that is not in the static directory. " +
                                        "Ignoring source file and computing new save location in static directory.");
                    }
                }
            }

            // Otherwise, it'll be a direct child of the static dir:
            dirPath += File.separator + DataManager.STATIC_DIR;
        }

        // Untagged notes get a boring default filename:
        String filename;
        List<Tag> tagList = note.getNonDateTags();
        if (tagList.isEmpty()) {
            filename = "untagged_note";
        }

        // Tagged notes are by default named after their (non-date) tags, separated by underscores.
        else {
            filename = tagList.get(0).getTag();
            for (int i = 1; i < tagList.size(); i++) {
                filename += "_" + tagList.get(i).getTag();
            }
        }

        // Make sure the filename is safe to use on all platforms by stripping out any illegal characters:
        filename = FileSystemUtil.sanitizeFilename(filename);

        return new File(dirPath, filename + ".txt");
    }

    /**
     * Automatically computes a suggested File for the given Template, based on its name.
     * Templates are saved in JSON format, but we use a "*.template" extension for them.
     * (Queries and Templates are stored in the same directory, so this lets us distinguish them).
     *
     * @param dataDirectory the base data directory. Must not be null.
     * @param template      Any non-null Template object.
     * @return A filename suitable for this Template.
     */
    static File computeFile(File dataDirectory, Template template) {
        if (dataDirectory == null) {
            throw new IllegalArgumentException("dataDirectory cannot be null");
        }
        if (template == null) {
            throw new IllegalArgumentException("template cannot be null");
        }

        String filename = template.getName();
        if (filename == null || filename.isBlank()) {
            // Template guards against empty names, so this SHOULD be impossible, but let's be safe:
            filename = Template.DEFAULT_NAME;
        }
        filename = FileSystemUtil.sanitizeFilename(filename);
        return new File(dataDirectory, DataManager.METADATA_DIR + File.separator + filename + ".template");
    }

    /**
     * Automatically computes a suggested File for the given Query, based on its name.
     * Queries are saved in JSON format, but we use a "*.query" extension for them
     * to distinguish them from Templates, which are stored in the same directory.
     *
     * @param dataDirectory the base data directory. Must not be null.
     * @param query         Any non-null Query object.
     * @return A filename suitable for this Query.
     */
    static File computeFile(File dataDirectory, Query query) {
        if (dataDirectory == null) {
            throw new IllegalArgumentException("dataDirectory cannot be null");
        }
        if (query == null) {
            throw new IllegalArgumentException("query cannot be null");
        }

        String filename = query.getName();
        if (filename == null || filename.isBlank()) {
            // Query guards against empty names, so this SHOULD be impossible, but let's be safe:
            filename = Query.DEFAULT_NAME;
        }

        filename = FileSystemUtil.sanitizeFilename(filename);
        return new File(dataDirectory, DataManager.METADATA_DIR + File.separator + filename + ".query");
    }
}
