package ca.corbett.snotes.extensions.builtin;

import ca.corbett.mcp.McpTool;
import ca.corbett.snotes.model.Note;
import ca.corbett.snotes.service.NoteSearchRequest;
import ca.corbett.snotes.service.NoteService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.function.Supplier;

/**
 * Exposes basic note searching as an MCP tool. This tool allows searching for notes based on tags, date, and content.
 * A single date can be specified (only startDate supplied), in which case, only notes with that exact date are returned.
 * A date range can be specified (both startDate and endDate supplied), in which case, only notes with dates within that
 * range (inclusive) are returned. If neither startDate nor endDate are supplied, then notes of any date (or
 * also undated notes) are returned.
 * <p>
 * One or more tags may be specified. Only notes that have ALL the specified tags are returned.
 * </p>
 * <p>
 * Text content may be specified. Only notes that contain the specified text (case-insensitive) are returned.
 * </p>
 * <p>
 * At least one of these filter parameters must be specified! If you attempt to invoke this tool with no
 * filters, you will receive an error message. This is to prevent accidental large search results.
 * </p>
 * <p><b>Future idea:</b> add a way for callers to limit and/or paginate the result set. Not for this release.
 * We'll add a big disclaimer in the README to be careful with large searches (they will quickly fill context).</p>
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
public class McpSearchNotesTool implements McpTool {

    private static final Logger log = Logger.getLogger(McpSearchNotesTool.class.getName());

    /**
     * Supplies the NoteService on demand, when the tool is invoked.
     * This is a supplier rather than a direct reference because the service is
     * reached via MainWindow, which only exists once the application has finished
     * loading and activating all extensions - so it must be fetched lazily,
     * per invocation, which is exactly what a supplier guarantees.
     */
    private final Supplier<NoteService> noteServiceProvider;

    /**
     * Creates a new McpSearchNotesTool.
     *
     * @param noteServiceProvider Supplies the NoteService when the tool is invoked. Must not be null.
     */
    public McpSearchNotesTool(Supplier<NoteService> noteServiceProvider) {
        this.noteServiceProvider = Objects.requireNonNull(noteServiceProvider, "noteServiceProvider cannot be null");
    }

    @Override
    public String getName() {
        return "search_notes";
    }

    @Override
    public String getDescription() {
        return "Searches for notes matching the given criteria: text content, tags, a specific date, "
             + "and/or a date range. All specified criteria must match (AND logic). "
             + "At least one criterion (text, tags, startDate, or endDate) is required; a search with "
             + "no criteria is rejected. Returns each matching note's tags and full text content, "
             + "ordered oldest first. There is no cap on the number of results, so narrow the search "
             + "if the result set could be large.";
    }

    /**
     * Invoked when the tool is called via the MCP server.
     * <p>
     *     In our case, we support the following parameters:
     * </p>
     * <ul>
     *     <li>text: optional. Only notes containing this text (case-insensitive) are returned.</li>
     *     <li>tags: optional. A comma-separated list of tags. Only notes that have ALL the specified tags are returned.</li>
     *     <li>startDate: optional. A yyyy-MM-dd formatted date string. Only notes dated on or after this date are returned.</li>
     *     <li>endDate: optional. A yyyy-MM-dd formatted date string. Only notes dated on or before this date are returned.</li>
     * </ul>
     * <p>
     *     At least one of these parameters must be specified. Any date criterion (startDate or endDate)
     *     automatically excludes undated notes, matching Snotes' standard date-filter semantics.
     * </p>
     */
    @Override
    public Map<String, Object> getInputSchema() {
        Map<String, Object> inputSchema = new HashMap<>();
        inputSchema.put("type", "object");
        Map<String, Object> properties = new HashMap<>();
        properties.put("text", Map.of(
                "type", "string",
                "description", "Optional. Only notes whose text content contains this value (case-insensitive) are returned."
        ));
        properties.put("tags", Map.of(
                "type", "string",
                "description", "Optional. A comma-separated list of tags. Only notes that have ALL of these tags are returned. "
                               + "A tag value in yyyy-MM-dd format is treated as a date tag, matching notes with that exact date."
        ));
        properties.put("startDate", Map.of(
                "type", "string",
                "description", "Optional. Inclusive start of a date range, in yyyy-MM-dd format. "
                               + "Only notes dated on or after this date are returned. "
                               + "Specifying any date criterion excludes undated notes."
        ));
        properties.put("endDate", Map.of(
                "type", "string",
                "description", "Optional. Inclusive end of a date range, in yyyy-MM-dd format. "
                               + "Only notes dated on or before this date are returned. "
                               + "Specifying any date criterion excludes undated notes."
        ));
        inputSchema.put("properties", properties);
        // Intentionally no "required" entry: all parameters are individually optional,
        // but at least one must be provided (enforced at runtime in buildRequest()).
        return inputSchema;
    }

    @Override
    public String execute(Map<String, Object> input) throws Exception {
        // Validate the input first: buildRequest() is pure logic that needs no application
        // state, so bad parameters fail fast with an actionable error message:
        NoteSearchRequest request = buildRequest(input);

        // The supplier is evaluated lazily, here and only here (see field Javadoc for why):
        NoteService noteService = noteServiceProvider.get();

        log.info("MCP: searchNotes(" + request + ")");
        List<Note> results;
        try {
            results = noteService.search(request);
        }
        catch (Exception e) {
            log.log(Level.SEVERE, "MCP: note search failed: " + request, e);
            throw new RuntimeException("Note search failed: " + ((e.getMessage() == null) ? "Unknown error" : e.getMessage()), e);
        }

        log.info("MCP: searchNotes(" + request + ") returned " + results.size() + " note(s)");
        return formatResults(results);
    }

    /**
     * Translates raw MCP tool input into a validated {@link NoteSearchRequest}.
     * <p>
     * Date parsing is delegated to the NoteSearchRequest builder, which uses strict
     * yyyy-MM-dd validation (malformed dates throw rather than silently becoming today's date),
     * and which also rejects a startDate that is after the endDate.
     * </p>
     * <p>
     * This method is package-private (and static, for good measure) so it can be
     * unit-tested without a running application.
     * </p>
     *
     * @param input The raw tool input as provided by the MCP server. May be null.
     * @return A validated NoteSearchRequest with at least one criterion set.
     * @throws IllegalArgumentException if any parameter is present but not a String, if a date
     *                                  is malformed, if startDate is after endDate, or if no
     *                                  criteria are specified at all.
     */
    static NoteSearchRequest buildRequest(Map<String, Object> input) {
        Map<String, Object> params = (input != null) ? input : Map.of();
        String text = optionalString(params, "text");
        String tags = optionalString(params, "tags");
        String startDate = optionalString(params, "startDate");
        String endDate = optionalString(params, "endDate");

        NoteSearchRequest request = NoteSearchRequest.builder()
                .withText(text)
                .withTags(splitTags(tags))
                .withStartDate(startDate)
                .withEndDate(endDate)
                .build();

        // A request with no criteria at all would match EVERY note in the data set.
        // That is a very large and very expensive surprise for an LLM client,
        // so we explicitly reject it:
        if (request.isEmpty()) {
            throw new IllegalArgumentException(
                "At least one search criterion is required. Provide 'text', 'tags', 'startDate', and/or 'endDate'.");
        }
        return request;
    }

    /**
     * Splits a raw comma/space-separated tag string into individual tag values,
     * using the same separator rules as {@code TagList.fromRawString()}.
     * <p>
     * The actual tag normalization (lower-casing, character substitution, date-tag
     * detection) is deliberately left to {@link NoteSearchRequest}, so that this tool
     * behaves exactly like the UI's simple search.
     * </p>
     *
     * @param rawTags The raw tags parameter value. Null or blank yields an empty list.
     * @return The individual (untrimmed-except-whitespace-around-tokens) tag values, in order.
     */
    private static List<String> splitTags(String rawTags) {
        if (rawTags == null || rawTags.isBlank()) {
            return List.of();
        }
        List<String> tags = new ArrayList<>();
        for (String token : rawTags.split("[\\s,]+")) {
            if (!token.isBlank()) {
                tags.add(token.trim());
            }
        }
        return tags;
    }

    /**
     * Returns the given parameter as a String, or null if it was not provided.
     * The MCP server hands us raw JSON, so the caller (usually an LLM) may send
     * any JSON type. Rather than guess, we fail loudly with an actionable message:
     *
     * @param params The tool input parameters.
     * @param name The parameter name, used in the error message.
     * @return The parameter value as a String, or null if absent (including explicit JSON nulls).
     * @throws IllegalArgumentException if the parameter is present but not a String.
     */
    private static String optionalString(Map<String, Object> params, String name) {
        Object value = params.get(name);
        if (value == null) {
            return null;
        }
        if (value instanceof String s) {
            return s;
        }
        throw new IllegalArgumentException(
            "The '" + name + "' parameter must be a string, but was " + value.getClass().getSimpleName() + ".");
    }

    /**
     * Renders the search results as a single text string for the MCP client.
     * Each note is rendered as an index, its persistence tag line (omitted for
     * undated/untagged notes), and its text content.
     * <p>
     * Note: there is deliberately NO cap on the number of notes rendered here.
     * Very large searches can overwhelm an LLM's context window; that is a
     * known limitation of this release (see the class Javadoc).
     * </p>
     *
     * @param notes The search results, already ordered oldest-first by the service. May be null or empty.
     * @return A human/LLM-readable rendering of the results, never null.
     */
    static String formatResults(List<Note> notes) {
        if (notes == null || notes.isEmpty()) {
            return "No notes found matching the search criteria.";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Found ").append(notes.size()).append(" note(s) matching the search criteria.\n\n");
        for (int i = 0; i < notes.size(); i++) {
            Note note = notes.get(i);
            sb.append("[").append(i + 1).append("] ");
            String tagLine = note.getPersistenceTagLine();
            if (!tagLine.isBlank()) {
                sb.append(tagLine).append('\n');
            }
            sb.append(note.getText()).append("\n\n");
        }
        return sb.toString().stripTrailing();
    }
}
