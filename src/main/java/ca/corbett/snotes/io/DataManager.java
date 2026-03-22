package ca.corbett.snotes.io;

import ca.corbett.extras.progress.MultiProgressDialog;
import ca.corbett.extras.progress.SimpleProgressAdapter;
import ca.corbett.snotes.model.Note;
import ca.corbett.snotes.model.Query;
import ca.corbett.snotes.model.Template;
import ca.corbett.snotes.ui.MainWindow;

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

    public DataManager() {
        this.notes = new CopyOnWriteArrayList<>();
        this.queries = new CopyOnWriteArrayList<>();
        this.templates = new CopyOnWriteArrayList<>();
        this.scratchNotes = new CopyOnWriteArrayList<>();
        loadProgress = new AtomicInteger(0);
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
     * An internal progress listener that we will hook onto our various worker threads.
     * When all worker threads have completed, we will notify the given LoadListener, if any.
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
                    listener.onLoadComplete(DataManager.this);
                }
            }
        }
    }
}
