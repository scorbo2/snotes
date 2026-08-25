package ca.corbett.snotes.service;

import ca.corbett.snotes.model.Note;
import ca.corbett.snotes.model.Query;

import java.io.IOException;
import java.util.List;

/**
 * A UI-agnostic service for creating, saving, and searching Notes.
 * <p>
 * This is the intended entry point for all programmatic access to the note
 * lifecycle, and most notably for the upcoming MCP (Model Context Protocol)
 * tool handlers, which must not touch Swing code or {@code DataManager} directly.
 * The Swing UI obtains its instance from {@code MainWindow.getNoteService()};
 * non-UI consumers running in the same JVM can do the same, or construct their
 * own {@link DefaultNoteService} around a {@code DataManager} instance.
 * </p>
 * <p>
 * Contract notes for implementations:
 * </p>
 * <ul>
 *     <li>All method signatures must remain UI-agnostic - no Swing types allowed.</li>
 *     <li>Searches operate only on "real" Notes. Scratch notes are never returned.</li>
 *     <li>Search results are ordered by date, with the most recent Notes last.</li>
 *     <li>Search results respect the requested limit, returning only the most recent matches.</li>
 * </ul>
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 * @since Snotes 2.3
 */
public interface NoteService {

    /**
     * Creates a new, blank scratch Note.
     * <p>
     * The returned Note is not a "real" Note: it will not appear in search results
     * until it is saved with {@link #saveNote(Note)} or one of its overloads.
     * Scratch notes are persisted across application restarts.
     * </p>
     *
     * @return A new scratch Note.
     */
    Note createScratchNote();

    /**
     * Saves the given scratch Note in-place in the scratch directory.
     * This does NOT promote the Note to a "real" Note.
     *
     * @param note The scratch Note to save. Must not be null.
     * @throws IOException if the save fails.
     */
    void saveScratchNote(Note note) throws IOException;

    /**
     * Saves the given Note to its permanent home in the data directory.
     * If the Note is a scratch Note, it is promoted to a "real" Note.
     * Collisions are handled with the {@link CollisionStrategy#ABORT} strategy,
     * meaning the save fails with an IOException if the target file is in use.
     *
     * @param note The Note to save. Must not be null.
     * @throws IOException if the save fails, including due to a file collision.
     */
    void saveNote(Note note) throws IOException;

    /**
     * Saves the given Note to its permanent home in the data directory,
     * handling any file collision according to the given strategy.
     * If the Note is a scratch Note, it is promoted to a "real" Note.
     *
     * @param note     The Note to save. Must not be null.
     * @param strategy How to handle a collision, if one occurs. Must not be null.
     * @throws IOException if the save fails.
     */
    void saveNote(Note note, CollisionStrategy strategy) throws IOException;

    /**
     * Reports whether the given Note would collide with an existing Note if saved.
     * A collision occurs when the Note's computed save location (based on its
     * date and tags) is already occupied by a different Note.
     *
     * @param note Any Note to check. Must not be null.
     * @return true if saving the Note now would result in a file collision.
     */
    boolean hasCollision(Note note);

    /**
     * Reports whether the given Note is a scratch Note, as opposed to a "real" Note.
     * Scratch notes live in the scratch directory and do not appear in search results.
     *
     * @param note Any candidate Note. May be null (returns false).
     * @return true if and only if the Note is a scratch Note.
     */
    boolean isScratchNote(Note note);

    /**
     * Searches for Notes matching the given criteria.
     * Only "real" Notes are returned; scratch notes are excluded.
     * Results are ordered by date, most recent last, and limited to the
     * most recent {@link NoteSearchRequest#getLimit() limit} matches.
     *
     * @param request The search criteria. Must not be null.
     * @return A (possibly empty) list of matching Notes, never null.
     * @throws IllegalArgumentException if the request contains invalid criteria.
     */
    List<Note> search(NoteSearchRequest request);

    /**
     * Executes the given Query against all "real" Notes, with no result limit.
     * This is equivalent to {@link #search(Query, int) search(query, NoteSearchRequest.NO_LIMIT)}.
     *
     * @param query The Query to execute. Must not be null.
     * @return A (possibly empty) list of matching Notes, never null.
     */
    List<Note> search(Query query);

    /**
     * Executes the given Query against all "real" Notes, returning only the most
     * recent {@code limit} matches.
     *
     * @param query The Query to execute. Must not be null.
     * @param limit  The maximum number of results to return. Must be &gt;= 0.
     *               Use {@link NoteSearchRequest#NO_LIMIT} for no upper limit.
     * @return A (possibly empty) list of matching Notes, never null.
     * @throws IllegalArgumentException if limit is negative.
     */
    List<Note> search(Query query, int limit);
}
