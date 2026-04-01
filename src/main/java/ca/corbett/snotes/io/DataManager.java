package ca.corbett.snotes.io;

import ca.corbett.extras.io.FileSystemUtil;
import ca.corbett.extras.progress.MultiProgressDialog;
import ca.corbett.extras.progress.SimpleProgressAdapter;
import ca.corbett.snotes.AppConfig;
import ca.corbett.snotes.model.Note;
import ca.corbett.snotes.model.Query;
import ca.corbett.snotes.model.Template;
import ca.corbett.snotes.ui.MainWindow;

import javax.swing.SwingUtilities;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * Provides an abstraction around loading and saving model objects within the application.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 * @since Snotes 2.0
 */
public class DataManager {

    public static final String METADATA_DIR = ".snotes"; // Not currently configurable
    public static final String STATIC_DIR = "static"; // Not currently configurable
    public static final String SCRATCH_DIR = ".scratch"; // Not currently configurable

    private static final Logger log = Logger.getLogger(DataManager.class.getName());

    /**
     * Can be used with save(Note) to decide what to do if a Note has
     * a collision with an existing Note.
     */
    public enum CollisionStrategy {OVERWRITE, APPEND, ABORT}

    /**
     * Callers can implement this to be notified when loadAll() completes.
     */
    @FunctionalInterface
    public interface LoadListener {
        /**
         * The given DataManager has completed a loadAll() operation.
         */
        void onLoadComplete(DataManager dataManager);
    }

    /**
     * Callers can implement this to be notified when a Note is deleted from the DataManager.
     */
    public interface NoteDeletionListener {
        /**
         * The given Note has been deleted from the DataManager.
         * This is informational only - the delete can't be overridden by callers.
         * The given Note is now "dead" - attempting to save() it will throw an IOException.
         *
         * @param note The Note that was deleted. This Note is now effectively "dead" and should not be used for anything.
         */
        void onNoteDeleted(Note note);
    }

    private final List<NoteDeletionListener> noteDeletionListeners;
    private final List<Note> notes;
    private final List<Query> queries;
    private final List<Template> templates;
    private final List<Note> scratchNotes;
    private final AtomicInteger loadProgress;

    private final File dataDir;
    private final File metadataDir;
    private final File staticDir;
    private final File scratchDir;

    /**
     * Creates a new DataManager with empty caches and a data directory
     * taken from AppConfig.
     */
    public DataManager() {
        this(AppConfig.getInstance().getDataDirectory());
    }

    /**
     * Package-private constructor for unit tests. Allows a custom data directory to be specified
     * directly, bypassing AppConfig entirely so that the full application need not be initialized.
     */
    DataManager(File dataDir) {
        this.noteDeletionListeners = new CopyOnWriteArrayList<>();
        this.notes = new CopyOnWriteArrayList<>();
        this.queries = new CopyOnWriteArrayList<>();
        this.templates = new CopyOnWriteArrayList<>();
        this.scratchNotes = new CopyOnWriteArrayList<>();
        loadProgress = new AtomicInteger(0);
        this.dataDir = dataDir;
        // These directories may not exist, but that's okay... loadAll() will deal with it:
        this.metadataDir = new File(dataDir, METADATA_DIR);
        this.staticDir = new File(dataDir, STATIC_DIR);
        this.scratchDir = new File(dataDir, SCRATCH_DIR);
    }

    /**
     * Creates a new Note with a new temporary scratch file as its source.
     * Invoking save() with this Note object will move it from the scratch
     * directory into the data directory, and update its source file accordingly.
     * Note: scratch notes are not "real" notes until they are saved. This means
     * that they will not show up in Query results. They will, however, be
     * persisted across application restarts. Invoking save() on a scratch
     * note will give it a proper home in the data directory and will
     * promote it to a "real" Note. Invoking saveScratch() on a scratch
     * note will save it in-place in the scratch directory, but will not
     * promote it to a "real" Note.
     */
    public Note newNote() {
        Note note = new Note();
        note.setSourceFile(newScratchFile()); // okay if null - it just won't get persisted, is all.
        scratchNotes.add(note);
        return note;
    }

    /**
     * Reports whether the given Note has a filename collision with an existing Note.
     * This can happen when changing the date or the tags for a given Note.
     * The changes result in the given Note moving to a new save location, which may
     * already be in use, resulting in a collision.
     *
     * @param note Any Note that we want to check for collisions. Must not be null.
     * @return True if the given Note has a filename collision with an existing Note, false otherwise.
     */
    public boolean hasCollision(Note note) {
        File savePath = SnotesIO.computeFile(dataDir, note);
        return savePath.exists()
            && (note.getSourceFile() == null || !savePath.equals(note.getSourceFile()));
    }

    /**
     * Saves the given Note to disk. The save location is determined dynamically based
     * on the metadata of the Note. If a file collision results from this save,
     * an IOException is thrown and the save is aborted. To handle collisions
     * differently, use the overloaded save(Note, CollisionStrategy) method and specify a CollisionStrategy.
     * <p>
     * If the given Note is a scratch note, it will be moved from the scratch directory
     * to its proper home in the data directory.
     * <p>
     * The idea here is that the caller never has to specify (or care about) where
     * the Note will be saved. The Note itself, and this Manager class, figure that out.
     * </p>
     *
     * @param note The Note object to save.
     */
    public void save(Note note) throws IOException {
        save(note, CollisionStrategy.ABORT);
    }

    /**
     * Saves the given Note to disk. The save location is determined dynamically based
     * on the metadata of the Note. Collisions are handled according to the given strategy.
     * <p>
     * If the given Note is a scratch note, it will be moved from the scratch directory
     * to its proper home in the data directory.
     * <p>
     * The idea here is that the caller never has to specify (or care about) where
     * the Note will be saved. The Note itself, and this Manager class, figure that out.
     * </p>
     *
     * @param note The Note object to save.
     */
    public void save(Note note, CollisionStrategy collisionStrategy) throws IOException {
        // If the given note is not in our cache, something is wrong (it was deleted?)
        if (!notes.contains(note) && !scratchNotes.contains(note)) {
            throw new IOException("Request to save a Note that is not in cache: " + note);
        }

        File savePath = SnotesIO.computeFile(dataDir, note);
        // Capture the old source file BEFORE saveNote() updates it, so we can clean it up afterwards
        // regardless of which code path we take below:
        File oldSourceFile = note.getSourceFile();

        if (hasCollision(note)) {
            if (savePath.exists()) {
                handleNoteCollision(note, savePath, collisionStrategy);
            }

            // At this point, there's either no existing file to worry about, or the collision
            // has been dealt with above. Either way, we can proceed with the save.

            // Ensure the target directory exists before attempting to write:
            File targetDir = savePath.getParentFile();
            if (targetDir != null && !targetDir.exists() && !targetDir.mkdirs()) {
                throw new IOException("Failed to create directory for note: " + targetDir.getAbsolutePath());
            }

            SnotesIO.saveNote(note, savePath); // updates the Note's source file to the new location + marks it clean.
        }
        else {
            // Either the Note's source file is already at the computed save path (in-place overwrite),
            // or savePath doesn't exist yet (first-time save with no collision). Save to the computed path.
            SnotesIO.saveNote(note, savePath);
        }

        // Clean up the old source file if it differs from where we just saved.
        // This covers both scratch note promotion (no collision path above) and note relocation
        // (collision path above). Without this step outside the collision block, a scratch file
        // would survive on disk any time the first save has no collision.
        if (oldSourceFile != null && oldSourceFile.exists() && !oldSourceFile.equals(savePath)) {
            if (!oldSourceFile.delete()) {
                // This is not fatal, but it is wonky... warn but proceed:
                log.warning("Failed to delete old source file for note: " + oldSourceFile.getAbsolutePath());
            }
        }

        // If this was a scratch note, move it from the scratch list to the main notes list:
        if (scratchNotes.remove(note)) {
            notes.add(note); // the note is now "real" and will show up in Query results.
        }

        log.info("Saved note: " + Note.getRelativePath(note, dataDir));
    }

    /**
     * Saves a scratch Note in-place in the scratch directory. This is mostly used by the auto-save feature,
     * so that we don't lose scratch notes if the application exits before the user actually saves it.
     * If the given Note is not a scratch note, this method does nothing.
     *
     * @param note Any scratch Note. Must not be null. If this Note is not a scratch note, this method does nothing.
     * @throws IOException If an error occurs while saving the Note.
     */
    public void saveScratch(Note note) throws IOException {
        if (!scratchNotes.contains(note)) {
            // Not a scratch note, so we can just return here. It is suspicious, though, so let's log a warning:
            log.warning("saveScratch() invoked on a non-scratch Note: " + note.getSourceFile().getAbsolutePath());
            return;
        }

        // This shouldn't happen, but it is technically possible for our scratch note
        // to have no sourceFile if something went wrong with scratch file creation.
        if (note.getSourceFile() == null) {
            throw new IOException("Scratch note has no source file: " + note);
        }

        SnotesIO.saveNote(note, note.getSourceFile()); // save in-place in the scratch directory
        log.info("Saved scratch note: " + note.getSourceFile().getName());
    }
    
    /**
     * Returns a defensive copy of the list of Notes currently loaded in memory.
     * Modifying this list will not affect the DataManager's internal state.
     */
    public List<Note> getNotes() {
        return new ArrayList<>(notes);
    }

    /**
     * Returns a List of all scratch notes currently loaded.
     * Modifying this list will not affect the DataManager's internal state.
     */
    public List<Note> getScratchNotes() {
        return new ArrayList<>(scratchNotes);
    }

    /**
     * Returns a defensive copy of the list of Queries currently loaded in memory.
     * Modifying this list will not affect the DataManager's internal state.
     */
    public List<Query> getQueries() {
        return new ArrayList<>(queries);
    }

    /**
     * Returns a defensive copy of the list of Templates currently loaded in memory.
     * Modifying this list will not affect the DataManager's internal state.
     */
    public List<Template> getTemplates() {
        return new ArrayList<>(templates);
    }

    /**
     * Returns true if the given query name is not already in use by another Query.
     * The search is done case-insensitively, so "my query" and "My Query" would be considered the same name.
     *
     * @param name Any non-null name to check for availability.
     * @return True if the given name is not already in use by another Query, false otherwise.
     */
    public boolean isQueryNameAvailable(String name) {
        return isQueryNameAvailable(name, null);
    }

    /**
     * Returns true if the given query name is not already in use by another Query,
     * excluding the Query with the given name. This is handy if you are editing an
     * existing Query and want to check if the new name is available, but you want to
     * ignore the fact that the old name is already in use by the Query you are editing.
     *
     * @param name             Any non-null name to check for availability.
     * @param excludingThisOne The name of a Query to exclude from the search. Can be null meaning "no exclusion".
     * @return True if the given name is not already in use by another Query (other than the one with the given name).
     */
    public boolean isQueryNameAvailable(String name, String excludingThisOne) {
        if (name == null) {
            throw new IllegalArgumentException("Query name cannot be null.");
        }
        for (Query query : queries) {
            // If it matches (case-insensitively), then the name is not available:
            if (query.getName().equalsIgnoreCase(name)) {
                // Unless we were given one to exclude, and this is it:
                if (query.getName().equalsIgnoreCase(excludingThisOne)) { // equalsIgnoreCase handles nulls
                    continue; // This is the one we're excluding, so ignore this match and keep searching.
                }
                return false;
            }
        }
        return true;
    }

    /**
     * Returns true if the given template name is not already in use by another Template.
     * The search is done case-insensitively, so "my template" and "My Template" would be considered the same name.
     *
     * @param name Any non-null name to check for availability.
     * @return True if the given name is not already in use by another Template, false otherwise.
     */
    public boolean isTemplateNameAvailable(String name) {
        return isTemplateNameAvailable(name, null);
    }

    /**
     * Returns true if the given template name is not already in use by another Template,
     * excluding the Template with the given name. This is handy if you are editing an
     * existing Template and want to check if the new name is available, but you want to
     * ignore the fact that the old name is already in use by the Template you are editing.
     *
     * @param name             Any non-null name to check for availability.
     * @param excludingThisOne The name of a Template to exclude from the search. Can be null meaning "no exclusion".
     * @return True if the given name is not already in use by another Template (other than the one with the given name).
     */
    public boolean isTemplateNameAvailable(String name, String excludingThisOne) {
        if (name == null) {
            throw new IllegalArgumentException("Template name cannot be null.");
        }
        for (Template template : templates) {
            // If it matches (case-insensitively), then the name is not available:
            if (template.getName().equalsIgnoreCase(name)) {
                // Unless we were given one to exclude, and this is it:
                if (template.getName().equalsIgnoreCase(excludingThisOne)) { // equalsIgnoreCase handles nulls
                    continue; // This is the one we're excluding, so ignore this match and keep searching.
                }
                return false;
            }
        }
        return true;
    }

    /**
     * Deletes the given Note's source file (if it had one) and removes it from our internal cache.
     *
     * @param note Any Note instance.
     */
    public void delete(Note note) {
        if (note == null) {
            throw new IllegalArgumentException("Cannot delete null Note.");
        }
        boolean wasRealNote = notes.remove(note);
        boolean wasScratchNote = scratchNotes.remove(note);
        if (note.getSourceFile() != null) {
            if (!note.getSourceFile().delete()) {
                // This is not fatal, but it is wonky... warn but proceed:
                log.warning("Failed to delete source file for note: " + note.getSourceFile().getAbsolutePath());
            }
        }
        else {
            log.warning("Note has no source file to delete: " + note);
            return;
        }

        // It's possible we were given a Note that was not in our cache.
        // This is very strange, but whatever:
        if (!wasRealNote && !wasScratchNote) {
            log.warning("Deleted uncached note: " + Note.getRelativePath(note, dataDir));
            return;
        }

        // Otherwise, just log it:
        String noteType = wasRealNote ? "note" : "scratch note";
        log.info("Deleted " + noteType + ": " + Note.getRelativePath(note, dataDir));
    }

    /**
     * Removes the specified Query from our in-memory cache, and deletes its source file
     * from disk if it existed.
     *
     * @param query The Query to delete. Must not be null.
     */
    public void delete(Query query) {
        if (query == null) {
            throw new IllegalArgumentException("Cannot delete null Query.");
        }

        queries.remove(query);
        if (query.getSourceFile() != null && query.getSourceFile().exists()) {
            if (!query.getSourceFile().delete()) {
                // This is not fatal, but it is wonky... warn but proceed:
                log.warning("Failed to delete source file for query: " + query.getSourceFile().getAbsolutePath());
            }
        }
    }

    /**
     * Removes the specified Template from our in-memory cache, and deletes its source file
     * from disk if it existed. Note that this does not affect any Notes that may have been
     * created using this Template as a template.
     *
     * @param template The Template to delete. Must not be null.
     */
    public void delete(Template template) {
        if (template == null) {
            throw new IllegalArgumentException("Cannot delete null Template.");
        }

        templates.remove(template);
        if (template.getSourceFile() != null && template.getSourceFile().exists()) {
            if (!template.getSourceFile().delete()) {
                // This is not fatal, but it is wonky... warn but proceed:
                log.warning("Failed to delete source file for template: " + template.getSourceFile().getAbsolutePath());
            }
        }
    }

    /**
     * Saves the given Template to disk. If this is a first-time save, a filename and location
     * is automatically selected within our data directory. Otherwise, if the Template has
     * not been renamed since it was loaded, it is saved back to the same file.
     *
     * @param template Any non-null Template to save.
     * @throws IOException If an error occurs while saving the Template, or if the given Template is null.
     */
    public void saveTemplate(Template template) throws IOException {
        if (template == null) {
            throw new IOException("Cannot save null Template.");
        }
        File targetFile = SnotesIO.computeFile(dataDir, template);
        if (template.getSourceFile() != null
            && !Files.isSameFile(template.getSourceFile().toPath(), targetFile.toPath())) {
            if (targetFile.exists()) {
                // User had a Template named "X" and they renamed Template "Y" to "X".
                // That's dumb, and now we have a problem.
                // Templates are considered lighter and more expendable than Notes, so we will just overwrite.
                // We'll log what we're doing so the user is aware of this.
                log.warning("Overwriting existing template file at " + targetFile.getAbsolutePath()
                                + " with template from " + template.getSourceFile().getAbsolutePath());
            }

            // Remove the old file if it still exists:
            if (template.getSourceFile().exists()) {
                if (!template.getSourceFile().delete()) {
                    // This is not fatal, but it is wonky... warn but proceed:
                    log.warning("Failed to delete old source file for template: "
                                    + template.getSourceFile().getAbsolutePath());
                }
            }
        }

        // Now save this Template to its new location. This will update its source file and mark it clean.
        SnotesIO.saveTemplate(template, targetFile);

        // If this Template wasn't already in our cache, add it now:
        if (!templates.contains(template)) {
            templates.add(template);
        }

        // Re-apply our template ordering, in case it has changed:
        applyTemplateOrdering();
    }

    /**
     * Saves the given Query to disk. If this is a first-time save, a filename and location
     * is automatically selected within our data directory. Otherwise, if the Query has
     * not been renamed since it was loaded, it is saved back to the same file.
     *
     * @param query Any non-null Query to save.
     * @throws IOException If an error occurs while saving the Query, or if the given Query is null.
     */
    public void saveQuery(Query query) throws IOException {
        if (query == null) {
            throw new IOException("Cannot save null Query.");
        }
        File targetFile = SnotesIO.computeFile(dataDir, query);
        if (query.getSourceFile() != null
            && !Files.isSameFile(query.getSourceFile().toPath(), targetFile.toPath())) {
            if (targetFile.exists()) {
                // User had a Query named "X" and they renamed Query "Y" to "X".
                // That's dumb, and now we have a problem.
                // Queries are considered lighter and more expendable than Notes, so we will just overwrite.
                // We'll log what we're doing so the user is aware of this.
                log.warning("Overwriting existing query file at " + targetFile.getAbsolutePath()
                                + " with query from " + query.getSourceFile().getAbsolutePath());
            }

            // Remove the old file if it still exists:
            if (query.getSourceFile().exists()) {
                if (!query.getSourceFile().delete()) {
                    // This is not fatal, but it is wonky... warn but proceed:
                    log.warning("Failed to delete old source file for query: "
                                    + query.getSourceFile().getAbsolutePath());
                }
            }
        }

        // Now save this Query to its new location. This will update the Query's source file and mark it clean.
        SnotesIO.saveQuery(query, targetFile);

        // If this Query wasn't already in our cache, add it now:
        if (!queries.contains(query)) {
            queries.add(query);
        }

        // Re-apply our query ordering, in case it has changed:
        applyQueryOrdering();
    }

    /**
     * Saves all Notes, Queries, and Templates to the given data directory, if they are marked as
     * needing to be saved.
     *
     * @throws IOException If any save fails.
     */
    public void saveAll() throws IOException {
        // Save all scratch notes in-place (this list should be quite small):
        for (Note scratchNote : scratchNotes) {
            if (scratchNote.isDirty()) {
                saveScratch(scratchNote);
            }
        }

        // Go through all notes and save the dirty ones.
        // The assumption here is that only a small number of notes will be dirty at any given time.
        // So, this is done on whatever thread invokes this method.
        // We may want to consider moving this to a worker thread in future.
        // The advantage of this approach is that IOExceptions can be surfaced to the caller immediately.
        // The drawback is that if ANY note fails to save, all subsequent notes remain dirty.
        for (Note note : notes) {
            if (note.isDirty()) {
                save(note);
            }
        }

        // Save all dirty Query instances:
        for (Query query : queries) {
            if (query.isDirty()) {
                saveQuery(query);
            }
        }

        // Save all dirty Template instances:
        for (Template template : templates) {
            if (template.isDirty()) {
                saveTemplate(template);
            }
        }
    }

    /**
     * Loads all Notes, Queries, and Templates from the given data directory, without specifying
     * a callback listener. This is for unit tests.
     */
    void loadAll() throws IOException {
        loadAll(null);
    }

    /**
     * Loads all Notes, Queries, and Templates from our directory, and reports to the
     * given LoadListener when complete. The loading is done in parallel in several worker threads.
     * The listener is not notified until ALL worker threads have completed. Long-running threads
     * will show a progress dialog as needed. The user has the option of canceling via the "cancel"
     * button on the progress dialog. If the user cancels, or if any thread encounters an error,
     * the listener will be notified, but the search results may be incomplete.
     * <p>
     * Note: The given listener is notified on the Swing EDT, and NOT on the worker thread,
     * so it's safe to update the UI from the listener.
     * </p>
     *
     * @param listener The LoadListener to be notified when loading is complete. Can be null.
     * @throws IOException If something is wrong with the given dataDir.
     */
    public void loadAll(LoadListener listener) throws IOException {
        if (dataDir == null) {
            throw new IOException("Data directory is null.");
        }
        if (!dataDir.exists()) {
            if (!dataDir.mkdirs()) {
                throw new IOException("Failed to create data directory: " + dataDir.getAbsolutePath());
            }
        }
        else if (!dataDir.isDirectory()) {
            throw new IOException("Data directory is not a directory: " + dataDir.getAbsolutePath());
        }

        if (!metadataDir.exists()) {
            if (!metadataDir.mkdirs()) {
                throw new IOException("Failed to create metadata directory: " + metadataDir.getAbsolutePath());
            }
        }
        else if (!metadataDir.isDirectory()) {
            throw new IOException("Metadata directory is not a directory: " + metadataDir.getAbsolutePath());
        }

        if (!staticDir.exists()) {
            if (!staticDir.mkdirs()) {
                throw new IOException("Failed to create static directory: " + staticDir.getAbsolutePath());
            }
        }
        else if (!staticDir.isDirectory()) {
            throw new IOException("Static directory is not a directory: " + staticDir.getAbsolutePath());
        }

        if (!scratchDir.exists()) {
            if (!scratchDir.mkdirs()) {
                throw new IOException("Failed to create scratch directory: " + scratchDir.getAbsolutePath());
            }
        }
        else if (!scratchDir.isDirectory()) {
            throw new IOException("Scratch directory is not a directory: " + scratchDir.getAbsolutePath());
        }

        // Do a quick pre-check of the scratch directory, and delete any completely empty files (length==0).
        List<File> scratchFiles = FileSystemUtil.findFiles(scratchDir, false, "txt");
        for (File scratchFile : scratchFiles) {
            if (scratchFile.length() == 0) {
                if (!scratchFile.delete()) {
                    log.warning("Failed to delete empty scratch file: " + scratchFile.getAbsolutePath());
                }
            }
        }

        // This is our countdown latch for tracking our four worker threads:
        LoaderThread<Note> noteThread;
        LoaderThread<Note> scratchThread;
        LoaderThread<Query> queryThread;
        LoaderThread<Template> templateThread;
        loadProgress.set(4);

        // Our loader threads will load all Notes, scratch Notes, Queries, and Templates in the data directory:
        noteThread = new LoaderThread<>("Notes", dataDir, "txt", true, SnotesIO::loadNote);
        addSkipDirectories(noteThread); // Don't waste time scanning directories we know won't contain Notes.
        noteThread.addProgressListener(new ThreadListener<>(noteThread, listener, this::setNotes));
        scratchThread = new LoaderThread<>("Scratch notes", scratchDir, "txt", false, SnotesIO::loadNote);
        scratchThread.addProgressListener(new ThreadListener<>(scratchThread, listener, this::setScratchNotes));
        queryThread = new LoaderThread<>("Queries", metadataDir, "query", false, SnotesIO::loadQuery);
        queryThread.addProgressListener(new ThreadListener<>(queryThread, listener, this::setQueries));
        templateThread = new LoaderThread<>("Templates", metadataDir, "template", false, SnotesIO::loadTemplate);
        templateThread.addProgressListener(new ThreadListener<>(templateThread, listener, this::setTemplates));

        // We'll configure the progress dialogs with a half-second delay so they don't show for quick loads:
        MultiProgressDialog dialog1 = new MultiProgressDialog(MainWindow.getInstance(), "Loading notes...");
        dialog1.setInitialShowDelayMS(500);
        dialog1.runWorker(noteThread, true);
        MultiProgressDialog dialog2 = new MultiProgressDialog(MainWindow.getInstance(), "Loading scratch notes...");
        dialog2.setInitialShowDelayMS(500);
        dialog2.runWorker(scratchThread, true);
        MultiProgressDialog dialog3 = new MultiProgressDialog(MainWindow.getInstance(), "Loading queries...");
        dialog3.setInitialShowDelayMS(500);
        dialog3.runWorker(queryThread, true);
        MultiProgressDialog dialog4 = new MultiProgressDialog(MainWindow.getInstance(), "Loading templates...");
        dialog4.setInitialShowDelayMS(500);
        dialog4.runWorker(templateThread, true);
    }

    public void addNoteDeletionListener(NoteDeletionListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("NoteDeletionListener cannot be null.");
        }
        noteDeletionListeners.add(listener);
    }

    public void removeNoteDeletionListener(NoteDeletionListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("NoteDeletionListener cannot be null.");
        }
        noteDeletionListeners.remove(listener);
    }

    /**
     * Reports whether the given Note has a source file in our "scratch" directory.
     * Such "scratch" notes are persisted across application restarts, but they don't count
     * as "real" notes, and will not appear in any search results. The user must explicitly
     * save them to promote them to "real" notes.
     *
     * @param note Any candidate Note instance.
     * @return true if and only if the source file is non-null and exists within our scratch directory.
     */
    public boolean isScratchNote(Note note) {
        if (note == null || note.getSourceFile() == null) {
            return false;
        }
        return note.getSourceFile().getAbsolutePath().startsWith(scratchDir.getAbsolutePath());
    }

    /**
     * Invoked internally to add certain known directories to be skipped when scanning for Note objects.
     * This will be made configurable in a future ticket. Extensions will also be able to contribute
     * to this list. Note that these exclusions only apply to the Note loader thread, since the
     * other threads only search known subdirectories under the main data directory.
     */
    private void addSkipDirectories(LoaderThread<Note> loaderThread) {
        loaderThread.addDirectoryToSkip(METADATA_DIR); // There are no Notes in the metadata directory.
        loaderThread.addDirectoryToSkip(".hg"); // Don't scan the top-level Mercurial directory.
        loaderThread.addDirectoryToSkip("images"); // There are no Notes in the images directory.
        loaderThread.addDirectoryToSkip(SCRATCH_DIR); // Scratch notes will be loaded separately.
    }

    private void setNotes(List<Note> notes) {
        this.notes.clear();
        this.notes.addAll(notes);
    }

    void setQueries(List<Query> queries) {
        this.queries.clear();
        this.queries.addAll(queries);
        applyQueryOrdering();
    }

    void setTemplates(List<Template> templates) {
        this.templates.clear();
        this.templates.addAll(templates);
        applyTemplateOrdering();
    }

    private void setScratchNotes(List<Note> scratchNotes) {
        this.scratchNotes.clear();
        this.scratchNotes.addAll(scratchNotes);
    }

    /**
     * Invoked whenever Query instances are added. We order Queries first
     * by their "order" attribute, and then alphabetically by their source filename as a tiebreaker.
     */
    private void applyQueryOrdering() {
        queries.sort(Comparator
                         .comparingInt(Query::getOrder)
                         .thenComparing(q -> q.getSourceFile() != null ? q.getSourceFile().getName() : ""));
    }

    /**
     * Invoked whenever Template instances are added. We order Templates first
     * by their "order" attribute, and then alphabetically by their source filename as a tiebreaker.
     */
    private void applyTemplateOrdering() {
        templates.sort(Comparator
                           .comparingInt(Template::getOrder)
                           .thenComparing(t -> t.getSourceFile() != null ? t.getSourceFile().getName() : ""));
    }

    /**
     * Will attempt to create and return a new temporary file in our scratch directory.
     * If the scratch directory does not already exist, it will be silently created.
     * If the scratch directory cannot be created, we fall back to the system
     * temp directory. This means that scratch files will not be persisted
     * across application restarts. If the scratch directory cannot be
     * created in the system temp directory, then we give up and return null.
     *
     * @return A new temporary File in our scratch directory, or null if we failed to create one.
     */
    private File newScratchFile() {
        File scratchDir = new File(dataDir, SCRATCH_DIR);
        if (!scratchDir.exists()) {
            if (!scratchDir.mkdirs()) {
                log.warning("Failed to create scratch directory: "
                                + scratchDir.getAbsolutePath()
                                + " - Falling back to system temp directory.");
                // Fall back to system temp dir if we can't create our own scratch dir.
                scratchDir = new File(System.getProperty("java.io.tmpdir"), SCRATCH_DIR);
                if (!scratchDir.exists() && !scratchDir.mkdirs()) {
                    log.severe("Failed to create scratch directory: " + scratchDir.getAbsolutePath());
                    return null;
                }
            }
        }
        else if (!scratchDir.isDirectory()) {
            log.warning("Scratch directory is not a directory: " + scratchDir.getAbsolutePath());
            return null;
        }
        try {
            return File.createTempFile("scratch", ".txt", scratchDir);
        }
        catch (IOException e) {
            log.warning("Failed to create scratch file: " + e.getMessage());
            return null;
        }
    }

    /**
     * Invoked internally to handle a Note collision according to the given strategy.
     * This method does not save the given Note! We just deal with the conflicting Note.
     *
     * @param note     The Note that we're trying to save.
     * @param savePath The computed save path for this Note.
     * @param strategy The strategy to use for handling this collision.
     * @throws IOException if strategy is ABORT, or if an error occurs while handling the collision.
     */
    private void handleNoteCollision(Note note, File savePath, CollisionStrategy strategy) throws IOException {
        Note existingNote = null;
        for (Note toCheck : notes) {
            if (toCheck.getSourceFile() != null && toCheck.getSourceFile().equals(savePath)) {
                existingNote = toCheck;
                break;
            }
        }

        switch (strategy) {
            case OVERWRITE:
                if (existingNote != null) {
                    String sourcePath = note.getSourceFile() != null ? note.getSourceFile().getAbsolutePath() : "null";
                    log.warning("Overwriting existing note file at " + savePath.getAbsolutePath()
                                    + " with note from " + sourcePath);
                    delete(existingNote); // this will remove it from our cache and delete the file from disk.
                    fireNoteDeletedEvent(existingNote); // close any open WriterFrame for this guy
                    break; // we found the one we were looking for, so we can stop searching.
                }
                break;
            case APPEND:
                if (existingNote != null) {
                    String sourcePath = note.getSourceFile() != null ? note.getSourceFile().getAbsolutePath() : "null";
                    log.warning("Appending to existing note file at " + savePath.getAbsolutePath()
                                    + " with note from " + sourcePath);
                    String existingText = existingNote.getText();
                    if (!existingText.isBlank()) {
                        // We want exactly one blank line between old content and new content:
                        if (existingText.endsWith("\n\n")) {
                            note.setText(existingText + note.getText());
                        }
                        else if (existingText.endsWith("\n")) {
                            note.setText(existingText + "\n" + note.getText());
                        }
                        else {
                            note.setText(existingText + "\n\n" + note.getText());
                        }
                    }

                    // We'll treat it as a deletion of the existing one, just to get it out of our cache:
                    delete(existingNote);
                    fireNoteDeletedEvent(existingNote);
                }
                return; // we're done - no need to move the source file or update the Note's source file reference.
            case ABORT:
                throw new IOException("Note filename collision detected at " + savePath.getAbsolutePath()
                                          + " for note: " + note);
            default:
                throw new IllegalArgumentException("Unknown collision strategy: " + strategy);
        }
    }

    private void fireNoteDeletedEvent(Note note) {
        for (NoteDeletionListener listener : new ArrayList<>(noteDeletionListeners)) {
            listener.onNoteDeleted(note);
        }
    }

    /**
     * An internal progress listener that we will hook onto our various worker threads.
     * When all worker threads have completed, we will notify the given LoadListener, if any.
     * <p>
     *     If a LoadListener is specified, it will only be notified once after ALL threads
     *     have completed. This is true regardless of whether the threads complete normally
     *     or are canceled by the user. Note that the listener will NOT be notified
     *     on the worker thread! The notification happens on the EDT, so it's safe to
     *     update the UI from the listener.
     * </p>
     */
    private class ThreadListener<T> extends SimpleProgressAdapter {

        private final LoadListener listener;
        private final LoaderThread<T> loaderThread;
        private final Consumer<List<T>> setFunction;

        public ThreadListener(LoaderThread<T> loaderThread, LoadListener listener, Consumer<List<T>> setFunction) {
            this.loaderThread = loaderThread;
            this.listener = listener;
            this.setFunction = setFunction;
        }

        @Override
        public boolean progressError(String errorSource, String errorDescription) {
            log.severe("LoaderThread error: " + errorSource + " - " + errorDescription);
            acceptResults(); // Likely nothing to accept, but we have to decrement the latch.
            return false; // irrelevant return; our LoaderThread doesn't check this
        }

        @Override
        public void progressComplete() {
            acceptResults();
        }

        @Override
        public void progressCanceled() {
            log.warning("A loader thread was canceled by the user. Results may be incomplete.");
            acceptResults();
        }

        private void acceptResults() {
            // Always store THIS thread's results, regardless of whether others are done:
            setFunction.accept(loaderThread.getSearchResults());
            if (loaderThread.hadErrors()) {
                log.warning("A loader thread encountered errors. Results may be incomplete.");
            }
            // Only notify the listener when ALL loader threads are done:
            if (loadProgress.decrementAndGet() == 0) {
                if (listener != null) {
                    // We're on the worker thread! Marshall this back to the EDT before notifying:
                    SwingUtilities.invokeLater(() -> listener.onLoadComplete(DataManager.this));
                }
            }
        }
    }
}
