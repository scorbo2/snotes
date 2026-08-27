package ca.corbett.snotes.extensions.builtin;

import ca.corbett.mcp.McpTool;
import ca.corbett.snotes.model.Note;
import ca.corbett.snotes.model.Tag;
import ca.corbett.snotes.model.TagList;
import ca.corbett.snotes.model.YMDDate;
import ca.corbett.snotes.service.CollisionStrategy;
import ca.corbett.snotes.service.NoteService;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.function.Supplier;

/**
 * Exposes basic note creation as an MCP tool. This tool does NOT allow creation of scratch notes,
 * only real notes. The new note can have an optional date - if omitted, the note is created as an undated note.
 * At least one tag must be specified, and the new note must have non-blank content.
 * <p>
 *     <b>Collision detection: </b> If a note with the same tags and date already exists, the behavior
 *     is driven by the collisionStrategy parameter, which can be one of OVERWRITE, APPEND, or ABORT.
 *     These are the same options presented to the user in the UI. The default behavior is APPEND if the
 *     parameter is omitted.
 * </p>
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
public class McpCreateNoteTool implements McpTool {

    private static final Logger log = Logger.getLogger(McpCreateNoteTool.class.getName());

    /**
     * Supplies the NoteService on demand, when the tool is invoked.
     * This is a supplier rather than a direct reference because the service is
     * reached via MainWindow, which only exists once the application has finished
     * loading and activating all extensions - so it must be fetched lazily,
     * per invocation, which is exactly what a supplier guarantees.
     */
    private final Supplier<NoteService> noteServiceProvider;

    /**
     * Creates a new McpCreateNoteTool.
     *
     * @param noteServiceProvider Supplies the NoteService when the tool is invoked. Must not be null.
     */
    public McpCreateNoteTool(Supplier<NoteService> noteServiceProvider) {
        this.noteServiceProvider = Objects.requireNonNull(noteServiceProvider, "noteServiceProvider cannot be null");
    }

    @Override
    public String getName() {
        return "create_note";
    }

    @Override
    public String getDescription() {
        return "Creates a new note with the specified content, tags (required), and optional date. "
             + "If a note with the same tags and date already exists, collisionStrategy controls whether to OVERWRITE, APPEND, or ABORT.";
    }

    /**
     * Invoked when the tool is called via the MCP server.
     * <p>
     *     In our case, we support the following parameters:
     * </p>
     * <ul>
     *     <li>tags: a comma-separated list of tags. A note must have at least one tag.</li>
     *     <li>date: a yyyy-MM-dd formatted date string (optional).</li>
     *     <li>content: any text content for the new note. Must not be blank!</li>
     *     <li>collisionStrategy: one of OVERWRITE, APPEND, or ABORT. Defaults to APPEND if not specified.
     *         This decides what to do if the new note collides with an existing note (i.e., same tags and date).</li>
     * </ul>
     */
    @Override
    public Map<String, Object> getInputSchema() {
        Map<String, Object> inputSchema = new HashMap<>();
        inputSchema.put("type", "object");
        Map<String, Object> properties = new HashMap<>();
        properties.put("tags", Map.of(
                "type", "string",
                "description", "A comma-separated list of tags. A note must have at least one tag."
        ));
        properties.put("date", Map.of(
                "type", "string",
                "description", "A yyyy-MM-dd formatted date string (optional)."
        ));
        properties.put("content", Map.of(
                "type", "string",
                "description", "Any text content for the new note. Must not be blank!"
        ));
        properties.put("collisionStrategy", Map.of(
                "type", "string",
                "enum", new String[]{"OVERWRITE", "APPEND", "ABORT"},
                "description", "One of OVERWRITE, APPEND, or ABORT. Defaults to APPEND if not specified. "
                + "This decides what to do if the new note collides with an existing note (i.e., same tags and date)."
        ));
        inputSchema.put("properties", properties);
        inputSchema.put("required", new String[]{"tags", "content"});
        return inputSchema;
    }

    @Override
    public String execute(Map<String, Object> input) throws Exception {
        if (!(input.get("content") instanceof String)) {
            throw new IllegalArgumentException("Missing or invalid 'content' parameter. Must be a non-blank string.");
        }

        // The supplier is evaluated lazily, here and only here (see field Javadoc for why):
        NoteService noteService = noteServiceProvider.get();

        String content = (String) input.getOrDefault("content", "");
        String tags = (String) input.getOrDefault("tags", "");
        String date = (String) input.getOrDefault("date", "");
        String rawStrategy = (String) input.getOrDefault("collisionStrategy", "APPEND");
        CollisionStrategy strategy = CollisionStrategy.fromString(rawStrategy);
        TagList tagList = TagList.fromRawString(tags);

        log.info("MCP: createNote(tags=\""+tagList.getPersistenceString()+"\", "
                 + "date=\""+date+"\", "
                 + "content length="+content.trim().length()+", "
                 + "collisionStrategy="+strategy+")");

        if (tagList.getTags().isEmpty()) {
            throw new IllegalArgumentException("At least one tag is required to create a note.");
        }
        if (content.isBlank()) {
            throw new IllegalArgumentException("Content cannot be blank.");
        }
        YMDDate ymdDate = null;
        if (YMDDate.isValidYMD(date)) {
            ymdDate = new YMDDate(date);
        }
        else if (! date.isBlank()) { // blank is fine, non-blank but invalid is not.
            throw new IllegalArgumentException("Dates must be in yyyy-MM-dd format. Invalid date: " + date);
        }
        if (strategy == null) {
            // Technically possible if the LLM gives us a garbage value:
            throw new IllegalArgumentException("Invalid collision strategy: " + rawStrategy);
        }

        // Create a scratch note:
        Note note = noteService.createScratchNote();
        note.setText(content);
        note.setDate(ymdDate); // okay if null
        for (Tag tag : tagList.getTags()) {
            note.tag(tag);
        }

        // Now save it:
        try {
            noteService.saveNote(note, strategy);
            return "Note created successfully: tags=["
                + tagList.getPersistenceString()+"], date="
                + (ymdDate==null?"<none>":ymdDate.toString());
        }
        catch (Exception e) {
            noteService.discardScratchNote(note); // clean up the scratch note if save fails

            // Check for ABORTs due to collisions, and give the agent some instructions on how to override that behavior:
            if (e.getMessage() != null && e.getMessage().contains("collision detected") && strategy == CollisionStrategy.ABORT) {
                log.log(Level.WARNING, "MCP: collision detected while creating note with tags=["
                        + tagList.getPersistenceString()+"], date="
                        + (ymdDate==null?"<none>":ymdDate.toString())
                        + ". Aborting as instructed. Use collisionStrategy=OVERWRITE or collisionStrategy=APPEND to override this behavior.");
                throw new IllegalArgumentException("Collision detected: a note with the same tags and date already exists. "
                    + "Aborting as instructed. Use collisionStrategy=OVERWRITE or collisionStrategy=APPEND to override this behavior.");
            }

            // For any other exception, just throw a generic error back to the agent:
            log.log(Level.SEVERE, "MCP: failed to create note with tags=["
                    + tagList.getPersistenceString()+"], date="
                    + (ymdDate==null?"<none>":ymdDate.toString())
                    + ". Error: " + e.getMessage(), e);
            throw new RuntimeException("Failed to create note: " + ((e.getMessage() == null) ? "Unknown error" : e.getMessage()));
        }
    }
}
