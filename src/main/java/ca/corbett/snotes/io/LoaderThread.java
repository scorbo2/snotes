package ca.corbett.snotes.io;

import ca.corbett.extras.io.FileSystemUtil;
import ca.corbett.extras.progress.SimpleProgressWorker;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A worker thread that will scan a given directory, and return an in-memory
 * cache of all T model objects found there. As this is a potentially heavy
 * IO operation, it gets its own worker thread, and should never be executed
 * on the Swing EDT.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 * @since Snotes 2.0
 */
class LoaderThread<T> extends SimpleProgressWorker {

    private static final Logger log = Logger.getLogger(LoaderThread.class.getName());

    @FunctionalInterface
    public interface FileLoader<T> {
        T load(File file) throws IOException;
    }

    private final List<T> searchResults;
    private final List<String> directoriesToSkip;
    private final File directory;
    private final String searchExtension;
    private final FileLoader<T> loadFunction;
    private final boolean isRecursive;
    private boolean wasCanceled;
    private boolean hadErrors;

    /**
     * Creates a LoaderThread that will scan the given directory for T files.
     * You can optionally invoke addDirectoryToSkip() on this thread to specify named
     * directories to be skipped during the loading process.
     *
     * @param directory The directory to scan for T files. Must be a readable directory that exists on disk.
     * @param extension The file extension to look for, without the dot. For example: "txt"
     * @param isRecursive Whether to scan subdirectories recursively or not.
     * @param loadFunction A function that can take a File and return a T. Presumably from SnotesIO.
     */
    public LoaderThread(File directory, String extension, boolean isRecursive, FileLoader<T> loadFunction) {
        this.directory = directory;
        this.isRecursive = isRecursive;
        this.searchExtension = extension;
        this.loadFunction = loadFunction;
        this.directoriesToSkip = new ArrayList<>();
        this.searchResults = new ArrayList<>();
    }

    /**
     * You can optionally specify named directories to be skipped during the loading process.
     * The given dirName can either be a fully qualified path, or just the name of the directory.
     * For example, to skip all Mercurial directories: addDirectoryToSkip(".hg")
     *
     * @param dirName The name of the directory to skip. Can be a fully qualified path, or just a directory name.
     * @return This LoaderThread, for chaining.
     */
    public LoaderThread<T> addDirectoryToSkip(String dirName) {
        directoriesToSkip.add(dirName);
        return this;
    }

    /**
     * Returns true if the search was canceled partway through by the user.
     * The searchResults will be incomplete in this case.
     */
    public boolean wasCanceled() {
        return wasCanceled;
    }

    /**
     * Reports whether any IOExceptions were encountered while searching.
     */
    public boolean hadErrors() {
        return hadErrors;
    }

    /**
     * Returns the results of the search. May be empty, but will never be null.
     * <B>NOTE:</B> if the search was canceled by the user, this list will be incomplete.
     * <p>
     * Results are returned in whatever order we found them on the filesystem.
     * It's up to the caller to make sense of the resulting list.
     * </p>
     */
    public List<T> getSearchResults() {
        return new ArrayList<>(searchResults); // return a copy in case we are re-run and clear our list
    }

    @Override
    public void run() {
        searchResults.clear();
        wasCanceled = false;
        hadErrors = false;

        // Sanity check our settings:
        if (directory == null || !directory.exists() || !directory.isDirectory()) {
            log.severe("Skipping invalid directory specified for LoaderThread: " + directory);
            hadErrors = true;
            return;
        }
        if (searchExtension == null || searchExtension.isBlank()) {
            log.severe("Skipping search with invalid extension specified for LoaderThread: " + searchExtension);
            hadErrors = true;
            return;
        }

        // Force the progress bar to appear while we enumerate the files, since this can be a heavy operation:
        fireProgressBegins(1); // 1 is just a dummy value until we know how many files there are
        try {
            List<File> fileList = FileSystemUtil.findFiles(directory, isRecursive, searchExtension);

            // Now we can set the actual progress bounds:
            fireProgressBegins(fileList.size());

            for (int i = 0; i < fileList.size(); i++) {
                File file = fileList.get(i);

                // Check if this file is in a directory we want to skip:
                if (shouldSkipFile(file)) {
                    // No need to log this, just silently skip it:
                    continue;
                }

                try {
                    searchResults.add(loadFunction.load(file));
                }
                catch (IOException ioe) {
                    // Log the error and keep going. One bad file shouldn't stop the whole operation.
                    log.log(Level.SEVERE, "Problem loading object: " + file.getAbsolutePath(), ioe);
                    hadErrors = true;
                }

                // Update progress and check for user cancellation:
                if (!fireProgressUpdate(i, file.getAbsolutePath())) {
                    wasCanceled = true;
                    break;
                }
            }
        }
        finally {
            // Ensure the progress bar is closed, one way or another:
            if (wasCanceled) {
                log.warning("LoaderThread was canceled by the user.");
                fireProgressCanceled();
            }
            else {
                fireProgressComplete();
            }
        }
    }

    /**
     * Invoked internally to determine if the given file is in any of our "skip" directories.
     */
    private boolean shouldSkipFile(File file) {
        for (String dirToSkip : directoriesToSkip) {
            // This is a bit of a hack that only works on Linux, but the contract
            // for addDirectoryToSkip() allows the caller to specify a fully qualified path.
            // So, if this looks like an absolute path, convert it to something that will work below.
            if (dirToSkip.startsWith(File.separator)) {
                dirToSkip = dirToSkip.substring(1);
            }

            // Now we can simply check if the file's absolute path contains this directory name,
            // with separators on either side to avoid false positives.
            if (file.getAbsolutePath().contains(File.separator + dirToSkip + File.separator)) {
                log.fine("Skipping file by request: " + file.getAbsolutePath());
                return true;
            }
        }

        // And if we get here, nothing matched, so the file should NOT be skipped:
        return false;
    }
}
