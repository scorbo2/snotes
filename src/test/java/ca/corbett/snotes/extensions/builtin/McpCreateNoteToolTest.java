package ca.corbett.snotes.extensions.builtin;

import ca.corbett.snotes.model.Note;
import ca.corbett.snotes.model.YMDDate;
import ca.corbett.snotes.service.CollisionStrategy;
import ca.corbett.snotes.service.NoteService;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link McpCreateNoteTool}.
 * The NoteService is mocked (via the injected supplier), so no running
 * application is ever required.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 * @since Snotes 2.3
 */
public class McpCreateNoteToolTest {

    /**
     * Creates a tool wired to the given (mocked) service, via the lazy supplier.
     */
    private static McpCreateNoteTool newTool(NoteService service) {
        return new McpCreateNoteTool(() -> service);
    }

    @Test
    public void execute_validInput_shouldCreateAndSaveNoteWithDefaultAppendStrategy() throws Exception {
        // GIVEN a mocked service that hands out a blank scratch note:
        Note scratchNote = new Note();
        NoteService service = mock(NoteService.class);
        when(service.createScratchNote()).thenReturn(scratchNote);
        McpCreateNoteTool tool = newTool(service);

        // WHEN we execute the tool with tags and content, but no explicit strategy:
        String output = tool.execute(Map.of("tags", "work", "content", "Buy milk"));

        // THEN the scratch note should have been populated, saved with the default
        // APPEND strategy, and a success message returned:
        assertEquals("Buy milk", scratchNote.getText());
        assertTrue(scratchNote.hasTag("work"));
        verify(service).saveNote(eq(scratchNote), eq(CollisionStrategy.APPEND));
        assertTrue(output.contains("Note created successfully"));
    }

    @Test
    public void execute_explicitStrategyAndDate_shouldUseThem() throws Exception {
        // GIVEN a mocked service that hands out a blank scratch note:
        Note scratchNote = new Note();
        NoteService service = mock(NoteService.class);
        when(service.createScratchNote()).thenReturn(scratchNote);
        McpCreateNoteTool tool = newTool(service);

        // WHEN we execute the tool with an explicit (lowercase, to also cover
        // case-insensitivity) strategy and a valid date:
        String output = tool.execute(Map.of(
            "tags", "work",
            "date", "2025-08-24",
            "content", "Hi there",
            "collisionStrategy", "overwrite"
        ));

        // THEN the note should carry the parsed date, and be saved with OVERWRITE:
        assertEquals(YMDDate.fromJson("2025-08-24"), scratchNote.getDate());
        verify(service).saveNote(eq(scratchNote), eq(CollisionStrategy.OVERWRITE));
        assertTrue(output.contains("date=2025-08-24"));
    }

    @Test
    public void execute_blankContent_shouldThrowAndCreateNothing() throws Exception {
        // GIVEN a mocked service (the supplier will be evaluated, but no method on it
        // may be called before the blank-content check rejects the input):
        NoteService service = mock(NoteService.class);
        McpCreateNoteTool tool = newTool(service);

        // WHEN we execute the tool with blank content:
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> tool.execute(Map.of("tags", "work", "content", "   ")));

        // THEN the tool should reject it and never create or save anything:
        assertTrue(e.getMessage().contains("Content cannot be blank"));
        verify(service, never()).createScratchNote();
        verify(service, never()).saveNote(any(Note.class), any(CollisionStrategy.class));
    }

    @Test
    public void execute_nonStringContent_shouldThrowBeforeEvaluatingTheSupplier() {
        // GIVEN a tool whose supplier will blow up loudly if it is ever evaluated:
        McpCreateNoteTool tool = new McpCreateNoteTool(() -> {
            throw new AssertionError("The NoteService must not be accessed for invalid input.");
        });

        // WHEN we execute the tool with a numeric 'content' parameter
        // (an LLM can send any JSON type), THEN the type check should reject it
        // before the supplier is ever evaluated (proving the lazy contract):
        assertThrows(IllegalArgumentException.class,
            () -> tool.execute(Map.of("tags", "work", "content", 42)));
    }

    @Test
    public void execute_noTags_shouldThrowAndCreateNothing() {
        // GIVEN a mocked service:
        NoteService service = mock(NoteService.class);
        McpCreateNoteTool tool = newTool(service);

        // WHEN we execute the tool with no tags at all:
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> tool.execute(Map.of("tags", "", "content", "Hi")));

        // THEN the tool should reject it and never create anything:
        assertTrue(e.getMessage().contains("At least one tag"));
        verify(service, never()).createScratchNote();
    }

    @Test
    public void execute_invalidDate_shouldThrowAndCreateNothing() {
        // GIVEN a mocked service:
        NoteService service = mock(NoteService.class);
        McpCreateNoteTool tool = newTool(service);

        // WHEN we execute the tool with a malformed date:
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> tool.execute(Map.of("tags", "work", "content", "Hi", "date", "not-a-date")));

        // THEN the tool should reject it (rather than silently dropping the date):
        assertTrue(e.getMessage().contains("yyyy-MM-dd"));
        verify(service, never()).createScratchNote();
    }

    @Test
    public void execute_invalidCollisionStrategy_shouldThrowAndCreateNothing() {
        // GIVEN a mocked service:
        NoteService service = mock(NoteService.class);
        McpCreateNoteTool tool = newTool(service);

        // WHEN we execute the tool with a strategy value CollisionStrategy doesn't know:
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> tool.execute(Map.of("tags", "work", "content", "Hi", "collisionStrategy", "VIBRATE")));

        // THEN the tool should reject it and never create anything:
        assertTrue(e.getMessage().contains("Invalid collision strategy"));
        verify(service, never()).createScratchNote();
    }

    @Test
    public void execute_collisionWithAbortStrategy_shouldThrowWithOverrideHintAndDiscardScratchNote() throws Exception {
        // GIVEN a mocked service that aborts the save due to a collision:
        Note scratchNote = new Note();
        NoteService service = mock(NoteService.class);
        when(service.createScratchNote()).thenReturn(scratchNote);
        doThrow(new IOException("collision detected: file already exists")).when(service)
            .saveNote(any(Note.class), eq(CollisionStrategy.ABORT));
        McpCreateNoteTool tool = newTool(service);

        // WHEN we execute the tool with collisionStrategy=ABORT:
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> tool.execute(Map.of("tags", "work", "content", "Hi", "collisionStrategy", "ABORT")));

        // THEN the agent should be told how to override, and the scratch note discarded:
        assertTrue(e.getMessage().contains("Collision detected"));
        assertTrue(e.getMessage().contains("collisionStrategy=OVERWRITE or collisionStrategy=APPEND"));
        verify(service).discardScratchNote(scratchNote);
    }

    @Test
    public void execute_saveFails_shouldDiscardScratchAndWrapError() throws Exception {
        // GIVEN a mocked service whose save blows up for reasons other than a collision:
        Note scratchNote = new Note();
        NoteService service = mock(NoteService.class);
        when(service.createScratchNote()).thenReturn(scratchNote);
        doThrow(new IOException("disk on fire")).when(service)
            .saveNote(any(Note.class), any(CollisionStrategy.class));
        McpCreateNoteTool tool = newTool(service);

        // WHEN we execute the tool, THEN the failure should be wrapped with context
        // (the McpServer will convert it into an error result for the client):
        RuntimeException e = assertThrows(RuntimeException.class,
            () -> tool.execute(Map.of("tags", "work", "content", "Hi")));

        // AND the scratch note should have been discarded:
        assertTrue(e.getMessage().contains("Failed to create note"));
        assertTrue(e.getMessage().contains("disk on fire"));
        verify(service).discardScratchNote(scratchNote);
    }
}
