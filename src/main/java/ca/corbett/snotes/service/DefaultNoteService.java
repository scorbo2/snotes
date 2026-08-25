package ca.corbett.snotes.service;

import ca.corbett.snotes.io.DataManager;
import ca.corbett.snotes.model.Note;
import ca.corbett.snotes.model.Query;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

/**
 * The standard {@link NoteService} implementation, delegating all persistence
 * to a {@link DataManager} and translating search criteria into {@link Query}
 * executions against the DataManager's note cache.
 * <p>
 * This class is a thin facade: it deliberately contains no domain logic of its own.
 * The interesting behaviors - collision handling, note ordering, filtering -
 * all live in DataManager and the filter classes, and are covered by their own tests.
 * </p>
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 * @since Snotes 2.3
 */
public class DefaultNoteService implements NoteService {

    private final DataManager dataManager;

    /**
     * Creates a new DefaultNoteService backed by the given DataManager.
     *
     * @param dataManager The DataManager to delegate to. Must not be null.
     */
    public DefaultNoteService(DataManager dataManager) {
        this.dataManager = Objects.requireNonNull(dataManager, "dataManager cannot be null");
    }

    @Override
    public Note createScratchNote() {
        return dataManager.newNote();
    }

    @Override
    public void saveScratchNote(Note note) throws IOException {
        dataManager.saveScratch(requireNote(note));
    }

    @Override
    public void saveNote(Note note) throws IOException {
        saveNote(note, CollisionStrategy.ABORT);
    }

    @Override
    public void saveNote(Note note, CollisionStrategy strategy) throws IOException {
        dataManager.save(requireNote(note), toDataManagerStrategy(requireStrategy(strategy)));
    }

    @Override
    public boolean hasCollision(Note note) {
        return dataManager.hasCollision(requireNote(note));
    }

    @Override
    public boolean isScratchNote(Note note) {
        // DataManager.isScratchNote() is already null-safe.
        return dataManager.isScratchNote(note);
    }

    @Override
    public List<Note> search(NoteSearchRequest request) {
        Objects.requireNonNull(request, "request cannot be null");
        return request.toQuery().execute(dataManager.getNotes(), request.getLimit());
    }

    @Override
    public List<Note> search(Query query) {
        return search(query, NoteSearchRequest.NO_LIMIT);
    }

    @Override
    public List<Note> search(Query query, int limit) {
        Objects.requireNonNull(query, "query cannot be null");
        return query.execute(dataManager.getNotes(), limit);
    }

    /**
     * Maps a service-layer CollisionStrategy onto DataManager's equivalent.
     * The two enums have identical names by design, but the service must not
     * leak DataManager's types to its callers.
     */
    private static DataManager.CollisionStrategy toDataManagerStrategy(CollisionStrategy strategy) {
        return switch (strategy) {
            case OVERWRITE -> DataManager.CollisionStrategy.OVERWRITE;
            case APPEND -> DataManager.CollisionStrategy.APPEND;
            case ABORT -> DataManager.CollisionStrategy.ABORT;
        };
    }

    private static Note requireNote(Note note) {
        return Objects.requireNonNull(note, "note cannot be null");
    }

    private static CollisionStrategy requireStrategy(CollisionStrategy strategy) {
        return Objects.requireNonNull(strategy, "strategy cannot be null");
    }
}
