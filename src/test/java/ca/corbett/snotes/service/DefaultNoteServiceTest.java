package ca.corbett.snotes.service;

import ca.corbett.snotes.io.DataManager;
import ca.corbett.snotes.model.Note;
import ca.corbett.snotes.model.Query;
import ca.corbett.snotes.model.Tag;
import ca.corbett.snotes.model.YMDDate;
import ca.corbett.snotes.model.filter.TagFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultNoteService}.
 * <p>
 * The DataManager is mocked so we can verify delegation without touching the filesystem.
 * Search tests execute real Queries against real Note fixtures - the filtering,
 * ordering, and limiting behaviors under test are Query's, and are covered here
 * to lock in the service's end-to-end search contract.
 * </p>
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 * @since Snotes 2.3
 */
public class DefaultNoteServiceTest {

    private static final YMDDate MARCH_1 = YMDDate.fromJson("2024-03-01");
    private static final YMDDate MARCH_15 = YMDDate.fromJson("2024-03-15");
    private static final YMDDate APRIL_1 = YMDDate.fromJson("2024-04-01");

    private DataManager dataManager;
    private DefaultNoteService service;
    private List<Note> notes;

    @BeforeEach
    public void setUp() {
        // GIVEN a mocked DataManager backing a fresh service:
        dataManager = Mockito.mock(DataManager.class);
        service = new DefaultNoteService(dataManager);

        // AND four notes with varying dates, tags, and text:
        notes = new ArrayList<>();
        notes.add(datedTaggedNote(MARCH_1, "The quick brown fox", "work", "project-alpha"));
        notes.add(datedTaggedNote(MARCH_15, "Lazy dogs sleep all day", "work"));
        notes.add(datedTaggedNote(APRIL_1, "The quick red car", "project-alpha"));
        notes.add(undatedNote("A quick undated note", "work"));
        when(dataManager.getNotes()).thenReturn(notes);
    }

    // -----------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------

    @Test
    public void constructor_nullDataManager_shouldThrowNullPointerException() {
        // WHEN we try to create a service with a null DataManager:
        assertThrows(NullPointerException.class, () -> new DefaultNoteService(null));
    }

    // -----------------------------------------------------------------
    // Note lifecycle delegation
    // -----------------------------------------------------------------

    @Test
    public void createScratchNote_shouldDelegateToDataManagerNewNote() {
        // GIVEN a stubbed newNote() returning a scratch note:
        Note scratch = new Note();
        when(dataManager.newNote()).thenReturn(scratch);

        // WHEN we ask the service to create a scratch note:
        Note created = service.createScratchNote();

        // THEN the exact note produced by the data manager should be returned:
        assertSame(scratch, created);
        verify(dataManager).newNote();
    }

    @Test
    public void saveScratchNote_shouldDelegateToDataManagerSaveScratch() throws IOException {
        // GIVEN a note:
        Note note = new Note();

        // WHEN we save it as scratch through the service:
        service.saveScratchNote(note);

        // THEN the data manager's saveScratch should be called with the same note:
        verify(dataManager).saveScratch(note);
    }

    @Test
    public void saveScratchNote_nullNote_shouldThrowNullPointerException() {
        // WHEN we try to save a null note:
        assertThrows(NullPointerException.class, () -> service.saveScratchNote(null));
    }

    @Test
    public void saveNote_withoutStrategy_shouldDelegateWithAbort() throws IOException {
        // GIVEN a note:
        Note note = new Note();

        // WHEN we save it without specifying a collision strategy:
        service.saveNote(note);

        // THEN the ABORT strategy should be used, matching DataManager.save(note) semantics:
        verify(dataManager).save(note, DataManager.CollisionStrategy.ABORT);
    }

    @Test
    public void saveNote_withEachStrategy_shouldDelegateWithMappedStrategy() throws IOException {
        // GIVEN a note:
        Note note = new Note();

        // WHEN we save it with each possible service-layer strategy:
        service.saveNote(note, CollisionStrategy.OVERWRITE);
        service.saveNote(note, CollisionStrategy.APPEND);
        service.saveNote(note, CollisionStrategy.ABORT);

        // THEN each should be mapped onto the DataManager's equivalent:
        verify(dataManager).save(note, DataManager.CollisionStrategy.OVERWRITE);
        verify(dataManager).save(note, DataManager.CollisionStrategy.APPEND);
        verify(dataManager).save(note, DataManager.CollisionStrategy.ABORT);
    }

    @Test
    public void saveNote_nullNote_shouldThrowNullPointerException() {
        // WHEN we try to save a null note:
        assertThrows(NullPointerException.class, () -> service.saveNote(null));
    }

    @Test
    public void saveNote_nullStrategy_shouldThrowNullPointerException() {
        // GIVEN a note:
        Note note = new Note();

        // WHEN we pass a null strategy:
        assertThrows(NullPointerException.class, () -> service.saveNote(note, null));
    }

    @Test
    public void hasCollision_whenDataManagerReportsCollision_shouldReturnTrue() {
        // GIVEN a note that the data manager reports as colliding:
        Note note = new Note();
        when(dataManager.hasCollision(note)).thenReturn(true);

        // WHEN we ask the service about it, THEN it should report a collision:
        assertTrue(service.hasCollision(note));
        verify(dataManager).hasCollision(note);
    }

    @Test
    public void hasCollision_nullNote_shouldThrowNullPointerException() {
        // WHEN we check a null note for collisions:
        assertThrows(NullPointerException.class, () -> service.hasCollision(null));
    }

    @Test
    public void isScratchNote_whenDataManagerReportsScratch_shouldReturnTrue() {
        // GIVEN a note that the data manager identifies as scratch:
        Note note = new Note();
        when(dataManager.isScratchNote(note)).thenReturn(true);

        // WHEN we ask the service, THEN it should agree:
        assertTrue(service.isScratchNote(note));
    }

    @Test
    public void isScratchNote_nullNote_shouldReturnFalse() {
        // GIVEN a null note (DataManager.isScratchNote is null-safe):
        when(dataManager.isScratchNote(null)).thenReturn(false);

        // THEN the service should return false rather than throw:
        assertFalse(service.isScratchNote(null));
    }

    // -----------------------------------------------------------------
    // search(NoteSearchRequest)
    // -----------------------------------------------------------------

    @Test
    public void search_nullRequest_shouldThrowNullPointerException() {
        // WHEN we pass a null request:
        assertThrows(NullPointerException.class, () -> service.search((NoteSearchRequest)null));
    }

    @Test
    public void search_noCriteria_shouldReturnAllNotesMostRecentLast() {
        // GIVEN an empty request (no criteria):
        NoteSearchRequest request = NoteSearchRequest.builder().build();

        // WHEN we search:
        List<Note> results = service.search(request);

        // THEN all four notes should be returned, sorted oldest first.
        // The undated note (no source file) sorts before all dated notes:
        assertEquals(4, results.size());
        assertEquals("A quick undated note", results.get(0).getText());
        assertEquals(MARCH_1, results.get(1).getDate());
        assertEquals(MARCH_15, results.get(2).getDate());
        assertEquals(APRIL_1, results.get(3).getDate());
    }

    @Test
    public void search_textCriterion_shouldMatchCaseInsensitively() {
        // GIVEN a request searching for "QUICK" in uppercase:
        NoteSearchRequest request = NoteSearchRequest.builder().withText("QUICK").build();

        // WHEN we search:
        List<Note> results = service.search(request);

        // THEN all three notes containing "quick" in any case should be returned:
        assertEquals(3, results.size());
        assertTrue(results.stream().allMatch(n -> n.getText().toLowerCase().contains("quick")));
    }

    @Test
    public void search_singleTag_shouldReturnOnlyNotesWithThatTag() {
        // GIVEN a request requiring the "work" tag:
        NoteSearchRequest request = NoteSearchRequest.builder().withTags("work").build();

        // WHEN we search:
        List<Note> results = service.search(request);

        // THEN exactly the three notes tagged "work" should be returned:
        assertEquals(3, results.size());
        assertTrue(results.stream().allMatch(n -> n.hasTag("work")));
    }

    @Test
    public void search_multipleTags_shouldRequireAllTags() {
        // GIVEN a request requiring BOTH "work" and "project-alpha":
        NoteSearchRequest request = NoteSearchRequest.builder().withTags("work", "project-alpha").build();

        // WHEN we search:
        List<Note> results = service.search(request);

        // THEN only the single note that has both tags should be returned:
        assertEquals(1, results.size());
        assertEquals("The quick brown fox", results.get(0).getText());
    }

    @Test
    public void search_dateRange_shouldUseInclusiveBoundsAndExcludeUndatedNotes() {
        // GIVEN a request for notes dated 2024-03-01 through 2024-03-15:
        NoteSearchRequest request = NoteSearchRequest.builder()
            .withStartDate("2024-03-01")
            .withEndDate("2024-03-15")
            .build();

        // WHEN we search:
        List<Note> results = service.search(request);

        // THEN exactly the two notes inside the range should be returned
        // (both bounds inclusive), and the undated note should be excluded:
        assertEquals(2, results.size());
        assertEquals(MARCH_1, results.get(0).getDate());
        assertEquals(MARCH_15, results.get(1).getDate());
    }

    @Test
    public void search_withLimit_shouldReturnOnlyMostRecentMatches() {
        // GIVEN a text search matching three notes, limited to 2 results:
        NoteSearchRequest request = NoteSearchRequest.builder()
            .withText("quick")
            .withLimit(2)
            .build();

        // WHEN we search:
        List<Note> results = service.search(request);

        // THEN only the two most recent matches should be returned, most recent last.
        // The undated match sorts first (treated as epoch), so it is the one dropped:
        assertEquals(2, results.size());
        assertEquals(MARCH_1, results.get(0).getDate());
        assertEquals(APRIL_1, results.get(1).getDate());
    }

    @Test
    public void search_zeroLimit_shouldReturnEmptyList() {
        // GIVEN a request with a limit of zero:
        NoteSearchRequest request = NoteSearchRequest.builder().withLimit(0).build();

        // WHEN we search, THEN no results should be returned:
        assertTrue(service.search(request).isEmpty());
    }

    // -----------------------------------------------------------------
    // search(Query) overloads
    // -----------------------------------------------------------------

    @Test
    public void search_queryWithoutLimit_shouldExecuteAgainstDataManagerNotes() {
        // GIVEN a query requiring the "work" tag:
        Query query = new Query();
        query.addFilter(new TagFilter(List.of(new Tag("work")), TagFilter.FilterType.ALL));

        // WHEN we search with the no-limit overload:
        List<Note> results = service.search(query);

        // THEN all matching notes should be returned:
        assertEquals(3, results.size());
        assertTrue(results.stream().allMatch(n -> n.hasTag("work")));
    }

    @Test
    public void search_queryWithLimit_shouldRespectLimit() {
        // GIVEN an empty query (which matches everything) and a limit of 1:
        List<Note> results = service.search(new Query(), 1);

        // THEN only the single most recent note should be returned:
        assertEquals(1, results.size());
        assertEquals(APRIL_1, results.get(0).getDate());
    }

    @Test
    public void search_queryWithNegativeLimit_shouldThrowIllegalArgumentException() {
        // WHEN we pass a negative limit:
        assertThrows(IllegalArgumentException.class, () -> service.search(new Query(), -1));
    }

    @Test
    public void search_nullQuery_shouldThrowNullPointerException() {
        // WHEN we pass a null query:
        assertThrows(NullPointerException.class, () -> service.search((Query)null));
    }

    // -----------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------

    private static Note datedTaggedNote(YMDDate date, String text, String... tags) {
        Note note = new Note();
        note.setDate(date);
        note.setText(text);
        for (String tag : tags) {
            note.tag(tag);
        }
        return note;
    }

    private static Note undatedNote(String text, String... tags) {
        return datedTaggedNote(null, text, tags);
    }
}
