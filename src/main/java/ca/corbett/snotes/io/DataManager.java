package ca.corbett.snotes.io;

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
import java.util.ArrayList;
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
     * Callers can implement this to be notified when loadAll() completes.
     */
    @FunctionalInterface
    public interface LoadListener {
        /**
         * The given DataManager has completed a loadAll() operation.
         */
        void onLoadComplete(DataManager dataManager);
    }

    private final List<Note> notes;
    private final List<Query> queries;
    private final List<Template> templates;
    private final List<Note> scratchNotes;
    private final AtomicInteger loadProgress;

    private File dataDir;

    public DataManager() {
        this.notes = new CopyOnWriteArrayList<>();
        this.queries = new CopyOnWriteArrayList<>();
        this.templates = new CopyOnWriteArrayList<>();
        this.scratchNotes = new CopyOnWriteArrayList<>();
        loadProgress = new AtomicInteger(0);
        dataDir = AppConfig.getInstance().getDataDirectory();
    }

    /**
     * Creates a new Note with a new temporary scratch file as its source.
     * Invoking save() with this Note object will move it from the scratch
     * directory into the data directory, and update its source file accordingly.
     */
    public Note newNote() {
        Note note = new Note();
        note.setSourceFile(newScratchFile()); // okay if null - it just won't get persisted, is all.
        scratchNotes.add(note);
        notes.add(note);
        return note;
    }

    /**
     * Saves the given Note to disk. The save location is determined dynamically based
     * on the metadata of the Note. This may mean that an existing Note is moved within the data
     * directory as a result of this save. If the given Note is a scratch note, it will
     * be moved from the scratch directory to its proper home in the data directory.
     * <p>
     * The idea here is that the caller never has to specify (or care about) where
     * the Note will be saved. The Note itself, and this Manager class, figure that out.
     * </p>
     *
     * @param note The Note object to save.
     */
    public void save(Note note) throws IOException {
        // TODO: pseudocode:
        // 1. Compute the save path based on Note metadata (specifically: date and tags).
        //    a. Code for this exists in the old Mercurial repo for Snotes v1... dig it up and reuse it!
        // 2. If the Note is a scratch note, move it from the scratch directory to its proper home.
        //    a. The scratch file may not exist! newScratchFile() is not guaranteed to return something.
        //    b. The scratch file may not be where you expect it! There's a possible fallback to system temp dir.
        //    c. If the scratch file does exist, make sure it gets deleted from the scratch directory AFTER the save.
        // 3. If it's not a scratch note, check if the new save location is different from the old one.
        //    a. This is not an error! It's possible that the Note was re-dated or the tags changed. Just move it.
        //    b. If the source file hasn't changed, just overwrite the original.
        //
        // Questions:
        // - Should we throw IOExceptions here to the caller (UI)? Probably yes, so errors can be displayed.
        // - Should we update the Note's source file immediately after the save, or only after a successful save?
        //   Probably only after a successful save.
    }
    
    /**
     * Returns a defensive copy of the list of Notes currently loaded in memory.
     * Modifying this list will not affect the DataManager's internal state.
     */
    public List<Note> getNotes() {
        return new ArrayList<>(notes);
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

    public void loadAll(File dataDir) throws IOException {
        loadAll(dataDir, null);
    }

    public void loadAll(File dataDir, LoadListener listener) throws IOException {
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

        File metadataDir = new File(dataDir, METADATA_DIR);
        if (!metadataDir.exists()) {
            if (!metadataDir.mkdirs()) {
                throw new IOException("Failed to create metadata directory: " + metadataDir.getAbsolutePath());
            }
        }
        else if (!metadataDir.isDirectory()) {
            throw new IOException("Metadata directory is not a directory: " + metadataDir.getAbsolutePath());
        }

        File staticDir = new File(dataDir, STATIC_DIR);
        if (!staticDir.exists()) {
            if (!staticDir.mkdirs()) {
                throw new IOException("Failed to create static directory: " + staticDir.getAbsolutePath());
            }
        }
        else if (!staticDir.isDirectory()) {
            throw new IOException("Static directory is not a directory: " + staticDir.getAbsolutePath());
        }

        // This is our countdown latch for tracking our three worker threads:
        loadProgress.set(3);

        // Our 3 threads will load all Notes, Queries, and Templates in the data directory:
        LoaderThread<Note> noteThread = new LoaderThread<>(dataDir, "txt", true, SnotesIO::loadNote);
        addSkipDirectories(noteThread); // Don't waste time scanning directories we know won't contain Notes.
        noteThread.addProgressListener(new ThreadListener<>(noteThread, listener, this::setNotes));
        LoaderThread<Query> queryThread = new LoaderThread<>(metadataDir, "query", false, SnotesIO::loadQuery);
        queryThread.addProgressListener(new ThreadListener<>(queryThread, listener, this::setQueries));
        LoaderThread<Template> templateThread = new LoaderThread<>(metadataDir, "template", false,
                                                                   SnotesIO::loadTemplate);
        templateThread.addProgressListener(new ThreadListener<>(templateThread, listener, this::setTemplates));

        // We'll configure the progress dialogs with a half-second delay so they don't show for quick loads:
        MultiProgressDialog dialog1 = new MultiProgressDialog(MainWindow.getInstance(), "Loading notes...");
        dialog1.setInitialShowDelayMS(500);
        dialog1.runWorker(noteThread, true);
        MultiProgressDialog dialog2 = new MultiProgressDialog(MainWindow.getInstance(), "Loading queries...");
        dialog2.setInitialShowDelayMS(500);
        dialog2.runWorker(queryThread, true);
        MultiProgressDialog dialog3 = new MultiProgressDialog(MainWindow.getInstance(), "Loading templates...");
        dialog3.setInitialShowDelayMS(500);
        dialog3.runWorker(templateThread, true);
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

        // Note that we DON'T exclude the scratch directory - if the application exited
        // previously with unsaved Notes, the user can resume editing them as scratch
        // files if we load them here. This is why we don't use the system temp dir as our scratch dir!
    }

    private void setNotes(List<Note> notes) {
        this.notes.clear();
        this.notes.addAll(notes);
    }

    private void setQueries(List<Query> queries) {
        this.queries.clear();
        this.queries.addAll(queries);
    }

    private void setTemplates(List<Template> templates) {
        this.templates.clear();
        this.templates.addAll(templates);
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
            // Only notify the listener when ALL three threads are done:
            if (loadProgress.decrementAndGet() == 0) {
                if (listener != null) {
                    // We're on the worker thread! Marshall this back to the EDT before notifying:
                    SwingUtilities.invokeLater(() -> listener.onLoadComplete(DataManager.this));
                }
            }
        }
    }
}
