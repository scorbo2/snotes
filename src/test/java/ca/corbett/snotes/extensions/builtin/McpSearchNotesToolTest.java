package ca.corbett.snotes.extensions.builtin;

import ca.corbett.snotes.model.Note;
import ca.corbett.snotes.model.YMDDate;
import ca.corbett.snotes.service.NoteSearchRequest;
import ca.corbett.snotes.service.NoteService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link McpSearchNotesTool}.
 * The pure validation and formatting logic (buildRequest and formatResults)
 * is tested directly, while the execute() path is tested against a mocked
 * NoteService, so no running application is ever required.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 * @since Snotes 2.3
 */
public class McpSearchNotesToolTest {

    /**
     * Creates a tool wired to the given (mocked) service, via the lazy supplier.
     * Passing null is fine for tests that never invoke execute().
     */
    private static McpSearchNotesTool newTool(NoteService service) {
        return new McpSearchNotesTool(() -> service);
    }

    // -----------------------------------------------------------------
    // Tool metadata and input schema
    // -----------------------------------------------------------------

    @Test
    public void getName_shouldReturnSearchNotes() {
        // GIVEN a new tool instance:
        McpSearchNotesTool tool = newTool(null);

        // THEN it should advertise the canonical tool name:
        assertEquals("search_notes", tool.getName());
    }

    @Test
    public void getDescription_shouldBeNonBlank() {
        // GIVEN a new tool instance:
        McpSearchNotesTool tool = newTool(null);

        // THEN the description should be present for the benefit of LLM clients:
        assertFalse(tool.getDescription().isBlank());
    }

    @Test
    public void getInputSchema_shouldExposeFourOptionalStringParams() {
        // GIVEN a new tool instance:
        McpSearchNotesTool tool = newTool(null);

        // WHEN we ask for the input schema:
        Map<String, Object> schema = tool.getInputSchema();
        @SuppressWarnings("unchecked")
        Map<String, Object> properties = (Map<String, Object>)schema.get("properties");

        // THEN the schema should be a JSON object with all four parameters as strings,
        // and nothing should be individually required (the "at least one" rule is semantic):
        assertEquals("object", schema.get("type"));
        assertEquals(4, properties.size());
        for (String paramName : List.of("text", "tags", "startDate", "endDate")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> param = (Map<String, Object>)properties.get(paramName);
            assertEquals("string", param.get("type"), "Parameter " + paramName + " should be a string");
        }
        assertNull(schema.get("required"));
    }

    // -----------------------------------------------------------------
    // buildRequest: rejection of invalid input
    // -----------------------------------------------------------------

    @Test
    public void buildRequest_nullInput_shouldThrowIllegalArgumentException() {
        // WHEN buildRequest is called with a null input map:
        // THEN it should be treated as "no criteria at all" and rejected:
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> McpSearchNotesTool.buildRequest(null));
        assertTrue(e.getMessage().contains("At least one search criterion"));
    }

    @Test
    public void buildRequest_emptyInput_shouldThrowIllegalArgumentException() {
        // GIVEN an input map with no parameters at all:
        Map<String, Object> input = Map.of();

        // WHEN buildRequest is called, THEN it should be rejected:
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> McpSearchNotesTool.buildRequest(input));
        assertTrue(e.getMessage().contains("At least one search criterion"));
    }

    @Test
    public void buildRequest_allParamsBlank_shouldThrowIllegalArgumentException() {
        // GIVEN an input map where every parameter is present but blank
        // (including tags that are only separators - which split into zero tags):
        Map<String, Object> input = Map.of(
            "text", "   ",
            "tags", ", ",
            "startDate", "",
            "endDate", ""
        );

        // WHEN buildRequest is called, THEN no effective criterion exists, so it should be rejected:
        assertThrows(IllegalArgumentException.class, () -> McpSearchNotesTool.buildRequest(input));
    }

    @Test
    public void buildRequest_nonStringTextParam_shouldThrowIllegalArgumentException() {
        // GIVEN an input map whose 'text' parameter is a number (an LLM can send any JSON type):
        Map<String, Object> input = Map.of("text", 42);

        // WHEN buildRequest is called, THEN it should fail loudly rather than guess:
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> McpSearchNotesTool.buildRequest(input));
        assertTrue(e.getMessage().contains("'text'"));
        assertTrue(e.getMessage().contains("must be a string"));
    }

    @Test
    public void buildRequest_nonStringTagsParam_shouldThrowIllegalArgumentException() {
        // GIVEN an input map whose 'tags' parameter is a list rather than a comma-separated string:
        Map<String, Object> input = Map.of("tags", List.of("work"));

        // WHEN buildRequest is called, THEN it should fail loudly:
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> McpSearchNotesTool.buildRequest(input));
        assertTrue(e.getMessage().contains("'tags'"));
        assertTrue(e.getMessage().contains("must be a string"));
    }

    @Test
    public void buildRequest_nonStringStartDateParam_shouldThrowIllegalArgumentException() {
        // GIVEN an input map whose 'startDate' parameter is a number (eg. 20250824):
        Map<String, Object> input = Map.of("startDate", 20250824);

        // WHEN buildRequest is called, THEN it should fail loudly:
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> McpSearchNotesTool.buildRequest(input));
        assertTrue(e.getMessage().contains("'startDate'"));
        assertTrue(e.getMessage().contains("must be a string"));
    }

    @Test
    public void buildRequest_invalidStartDate_shouldThrowIllegalArgumentException() {
        // GIVEN an input map with a malformed startDate:
        Map<String, Object> input = Map.of("startDate", "not-a-date");

        // WHEN buildRequest is called, THEN the strict date parser should reject it
        // (rather than silently substituting today's date):
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> McpSearchNotesTool.buildRequest(input));
        assertTrue(e.getMessage().contains("startDate"));
        assertTrue(e.getMessage().contains("yyyy-MM-dd"));
    }

    @Test
    public void buildRequest_invalidEndDate_shouldThrowIllegalArgumentException() {
        // GIVEN an input map with an impossible endDate (month 13):
        Map<String, Object> input = Map.of("endDate", "2025-13-45");

        // WHEN buildRequest is called, THEN the strict date parser should reject it:
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> McpSearchNotesTool.buildRequest(input));
        assertTrue(e.getMessage().contains("endDate"));
    }

    @Test
    public void buildRequest_startDateAfterEndDate_shouldThrowIllegalArgumentException() {
        // GIVEN an input map whose date range is backwards:
        Map<String, Object> input = Map.of(
            "startDate", "2025-08-24",
            "endDate", "2025-08-20"
        );

        // WHEN buildRequest is called, THEN the NoteSearchRequest builder should reject it:
        assertThrows(IllegalArgumentException.class, () -> McpSearchNotesTool.buildRequest(input));
    }

    // -----------------------------------------------------------------
    // buildRequest: valid input translation
    // -----------------------------------------------------------------

    @Test
    public void buildRequest_textOnly_shouldSetTextCriterion() {
        // GIVEN an input map with only a text criterion:
        Map<String, Object> input = Map.of("text", "hello");

        // WHEN buildRequest is called:
        NoteSearchRequest request = McpSearchNotesTool.buildRequest(input);

        // THEN the request should carry exactly that criterion:
        assertFalse(request.isEmpty());
        assertEquals("hello", request.getText());
        assertTrue(request.getTags().isEmpty());
        assertNull(request.getStartDate());
        assertNull(request.getEndDate());
    }

    @Test
    public void buildRequest_commaSeparatedTags_shouldSplitIntoIndividualTags() {
        // GIVEN an input map with a comma-separated tag list (mixed spacing):
        Map<String, Object> input = Map.of("tags", "Work, Home  travel");

        // WHEN buildRequest is called:
        NoteSearchRequest request = McpSearchNotesTool.buildRequest(input);

        // THEN the raw tags should be split into three individual values, in order
        // (normalization is the NoteSearchRequest's job, not the tool's):
        assertEquals(List.of("Work", "Home", "travel"), request.getTags());
        assertNull(request.getText());
    }

    @Test
    public void buildRequest_singleDate_shouldTreatAsStartBoundOnly() {
        // GIVEN an input map with only a startDate (the "exact date" search):
        Map<String, Object> input = Map.of("startDate", "2025-08-24");

        // WHEN buildRequest is called:
        NoteSearchRequest request = McpSearchNotesTool.buildRequest(input);

        // THEN only the lower bound should be set, and it should be parsed as a YMDDate:
        assertEquals(YMDDate.fromJson("2025-08-24"), request.getStartDate());
        assertNull(request.getEndDate());
        assertFalse(request.isEmpty());
    }

    @Test
    public void buildRequest_dateRange_shouldSetBothBounds() {
        // GIVEN an input map with a full date range:
        Map<String, Object> input = Map.of(
            "startDate", "2025-08-20",
            "endDate", "2025-08-24"
        );

        // WHEN buildRequest is called:
        NoteSearchRequest request = McpSearchNotesTool.buildRequest(input);

        // THEN both inclusive bounds should be set:
        assertEquals(YMDDate.fromJson("2025-08-20"), request.getStartDate());
        assertEquals(YMDDate.fromJson("2025-08-24"), request.getEndDate());
    }

    @Test
    public void buildRequest_allCriteria_shouldSetAllCriteria() {
        // GIVEN an input map with every criterion at once:
        Map<String, Object> input = Map.of(
            "text", "meeting",
            "tags", "work",
            "startDate", "2025-08-01",
            "endDate", "2025-08-24"
        );

        // WHEN buildRequest is called:
        NoteSearchRequest request = McpSearchNotesTool.buildRequest(input);

        // THEN every criterion should be present and correct:
        assertEquals("meeting", request.getText());
        assertEquals(List.of("work"), request.getTags());
        assertEquals(YMDDate.fromJson("2025-08-01"), request.getStartDate());
        assertEquals(YMDDate.fromJson("2025-08-24"), request.getEndDate());
        assertFalse(request.isEmpty());
    }

    @Test
    public void buildRequest_jsonNullParams_shouldBeTreatedAsAbsent() {
        // GIVEN an input map where some parameters are explicit JSON nulls:
        Map<String, Object> input = new java.util.HashMap<>();
        input.put("text", null);
        input.put("startDate", null);
        input.put("tags", "work");

        // WHEN buildRequest is called:
        NoteSearchRequest request = McpSearchNotesTool.buildRequest(input);

        // THEN the nulls should be treated as absent, and the remaining criterion should work:
        assertNull(request.getText());
        assertNull(request.getStartDate());
        assertEquals(List.of("work"), request.getTags());
        assertFalse(request.isEmpty());
    }

    @Test
    public void buildRequest_tagsContainingDateLikeValue_shouldPreserveRawValue() {
        // GIVEN an input map whose tag list contains a yyyy-MM-dd value
        // (the NoteSearchRequest will later translate that into a DateTag,
        // matching the UI's simple search behavior):
        Map<String, Object> input = Map.of("tags", "2025-08-24, work");

        // WHEN buildRequest is called:
        NoteSearchRequest request = McpSearchNotesTool.buildRequest(input);

        // THEN the raw values should be preserved for the request to normalize:
        assertEquals(List.of("2025-08-24", "work"), request.getTags());
    }

    // -----------------------------------------------------------------
    // formatResults
    // -----------------------------------------------------------------

    @Test
    public void formatResults_nullList_shouldReturnNoNotesMessage() {
        // WHEN we format a null result list:
        String output = McpSearchNotesTool.formatResults(null);

        // THEN a friendly "nothing found" message should be returned:
        assertEquals("No notes found matching the search criteria.", output);
    }

    @Test
    public void formatResults_emptyList_shouldReturnNoNotesMessage() {
        // WHEN we format an empty result list:
        String output = McpSearchNotesTool.formatResults(List.of());

        // THEN a friendly "nothing found" message should be returned:
        assertEquals("No notes found matching the search criteria.", output);
    }

    @Test
    public void formatResults_singleDatedTaggedNote_shouldRenderTagLineAndContent() {
        // GIVEN a single dated, tagged note:
        Note note = new Note()
            .setDate(YMDDate.fromJson("2025-08-24"))
            .tag("work")
            .setText("Hello world");

        // WHEN we format the result list:
        String output = McpSearchNotesTool.formatResults(List.of(note));

        // THEN the output should contain the result count, the persistence tag line,
        // and the note's text content:
        assertEquals("Found 1 note(s) matching the search criteria.\n\n"
                   + "[1] #2025-08-24 #work\n"
                   + "Hello world", output);
    }

    @Test
    public void formatResults_multipleNotes_shouldNumberThemInOrder() {
        // GIVEN two notes with different dates and tags (already ordered oldest-first):
        Note first = new Note()
            .setDate(YMDDate.fromJson("2025-08-23"))
            .tag("home")
            .setText("First note");
        Note second = new Note()
            .setDate(YMDDate.fromJson("2025-08-24"))
            .tag("work")
            .setText("Second note");

        // WHEN we format the result list:
        String output = McpSearchNotesTool.formatResults(List.of(first, second));

        // THEN each note should be numbered sequentially, in list order:
        assertEquals("Found 2 note(s) matching the search criteria.\n\n"
                   + "[1] #2025-08-23 #home\n"
                   + "First note\n\n"
                   + "[2] #2025-08-24 #work\n"
                   + "Second note", output);
    }

    @Test
    public void formatResults_undatedUntaggedNote_shouldOmitTagLine() {
        // GIVEN a note with no date and no tags:
        Note note = new Note().setText("Orphan text");

        // WHEN we format the result list:
        String output = McpSearchNotesTool.formatResults(List.of(note));

        // THEN the (empty) tag line should be omitted, and the content still rendered:
        assertEquals("Found 1 note(s) matching the search criteria.\n\n"
                    + "[1] Orphan text", output);
    }

    // -----------------------------------------------------------------
    // execute() - end-to-end against a mocked NoteService
    // -----------------------------------------------------------------

    @Test
    public void execute_validInput_shouldPassBuiltRequestToServiceAndFormatResults() throws Exception {
        // GIVEN a dated, tagged note, and a mocked service that returns it for any search:
        Note note = new Note()
            .setDate(YMDDate.fromJson("2025-08-24"))
            .tag("work")
            .setText("Hello world");
        NoteService service = mock(NoteService.class);
        when(service.search(any(NoteSearchRequest.class))).thenReturn(List.of(note));
        McpSearchNotesTool tool = newTool(service);

        // WHEN we execute the tool with a valid text search:
        String output = tool.execute(Map.of("text", "hello"));

        // THEN the translated request should have been handed to the service,
        // and the results should be formatted for the MCP client:
        ArgumentCaptor<NoteSearchRequest> captor = ArgumentCaptor.forClass(NoteSearchRequest.class);
        verify(service).search(captor.capture());
        assertEquals("hello", captor.getValue().getText());
        assertEquals("Found 1 note(s) matching the search criteria.\n\n"
                   + "[1] #2025-08-24 #work\n"
                   + "Hello world", output);
    }

    @Test
    public void execute_noResults_shouldReturnNoNotesMessage() throws Exception {
        // GIVEN a mocked service that finds nothing:
        NoteService service = mock(NoteService.class);
        when(service.search(any(NoteSearchRequest.class))).thenReturn(List.of());
        McpSearchNotesTool tool = newTool(service);

        // WHEN we execute the tool:
        String output = tool.execute(Map.of("text", "nomatch"));

        // THEN a friendly "nothing found" message should be returned:
        assertEquals("No notes found matching the search criteria.", output);
    }

    @Test
    public void execute_serviceThrows_shouldWrapInRuntimeExceptionWithContext() {
        // GIVEN a mocked service that blows up during the search.
        // (search() declares no checked exceptions, so Mockito requires an unchecked one:
        // the tool's catch block is deliberately broad, so this is a fair simulation.)
        NoteService service = mock(NoteService.class);
        when(service.search(any(NoteSearchRequest.class))).thenThrow(new IllegalStateException("disk exploded"));
        McpSearchNotesTool tool = newTool(service);

        // WHEN we execute the tool, THEN the failure should be wrapped with context
        // (the McpServer will convert it into an error result for the client):
        RuntimeException e = assertThrows(RuntimeException.class, () -> tool.execute(Map.of("text", "x")));
        assertTrue(e.getMessage().contains("Note search failed"));
        assertTrue(e.getMessage().contains("disk exploded"));
    }

    @Test
    public void execute_invalidInput_shouldFailWithoutEvaluatingTheSupplier() {
        // GIVEN a tool whose supplier will blow up loudly if it is ever evaluated:
        Supplier<NoteService> failingSupplier = () -> {
            throw new AssertionError("The NoteService must not be accessed for invalid input.");
        };
        McpSearchNotesTool tool = new McpSearchNotesTool(failingSupplier);

        // WHEN we execute the tool with no criteria at all, THEN validation should
        // reject it before the supplier is ever evaluated (proving the lazy contract):
        assertThrows(IllegalArgumentException.class, () -> tool.execute(Map.of()));
    }
}
