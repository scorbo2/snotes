package ca.corbett.snotes.io;

import ca.corbett.extras.io.FileSystemUtil;
import ca.corbett.snotes.model.Snote;
import ca.corbett.snotes.model.SnoteCollection;
import ca.corbett.snotes.model.Tag;
import ca.corbett.snotes.model.YMDDate;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Scans a given directory (recursively) for Snote text files and configuration,
 * and builds up a TagCache, a SnoteCache, and a list of Templates and Queries
 * that were found there.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 * @since Snotes 1.0, heavily rewritten for Snotes 2.0
 */
public final class Scanner {

    private static final Logger logger = Logger.getLogger(Scanner.class.getName());

    public Scanner() {
    }

    /**
     * Loads the SnoteCollection from the given root directory.
     * TODO why tf are we passing in a SimpleProgressListener if this isn't executing in a worker thread?
     * TODO number 2: why is this not being executed in a worker thread? This is potentially heavy IO!
     * Answering my own question: the old code actually was executing this in a worker thread,
     * and passing in the progress listener. But I think it makes more sense for this class to
     * create and manage its own worker thread internally, so the caller doesn't have to worry about it.
     * Collections could be loaded asynchronously, with some kind of callback interface to report
     * success/failure when done. So, the second parameter to this method should be that callback
     * interface, not a progress listener. Although, passing in a progress listener might make this
     * easier to unit test, instead of assuming a progress dialog will always be used. Needs more thought.
     * Also, we can't return a SnoteCollection here if we're doing this async...
     */
    public static SnoteCollection loadCollection(File rootDir) throws IOException {
        return new SnoteCollection(loadSnoteCache(rootDir),
                                  loadTemplateCache(rootDir),
                                  loadQueryCache(rootDir));
    }

    private static SnoteCache loadSnoteCache(File rootDir) throws IOException {
        long startTime = System.currentTimeMillis();
        SnoteCache cache = new SnoteCache();

        List<File> fileList = FileSystemUtil.findFiles(rootDir, true, "txt");

        for (int i = 0; i < fileList.size(); i++) {
            File file = fileList.get(i);

            // Special case! Skip any files in any .hg directory.
            if (file.getAbsolutePath().contains(File.separator + ".hg" + File.separator)) {
                continue;
            }

            // Special case: skip any files in the to_import directory.
            // (This one can be removed after the migration when that dir gets nuked).
            if (file.getAbsolutePath().contains(File.separator + "to_import" + File.separator)) {
                continue;
            }

            // Report progress and give the caller a chance to cancel:
            //if (!listener.progressUpdate(i, file.getAbsolutePath().replace(rootDir.getAbsolutePath(), ""))) {
            //    listener.progressCanceled();
            //  return;
            //}

            // Load this one and add it to the list:
            try {
                cache.add(loadSnote(file));
            }
            catch (IOException ioe) {
                // TODO one bad load shouldn't stop the whole operation. Log and continue?
                logger.log(Level.WARNING, "Problem loading Snote from file: " + file.getAbsolutePath(), ioe);
                //if (!listener.progressError(file.getAbsolutePath(), "Problem loading data.")) {
                //    listener.progressCanceled();
                //    return;
                //}
            }
        }

        long elapsedTime = System.currentTimeMillis() - startTime;
        logger.log(Level.INFO, "All data loaded in {0}ms: {1} tags, {2} snotes.",
                   new Object[]{elapsedTime, cache.getTagCount(), cache.size()});
        //listener.progressComplete();
        return cache;
    }

    private static QueryCache loadQueryCache(File rootDir) {

    }

    private static TemplateCache loadTemplateCache(File rootDir) {

    }

    /**
     * Executed internally to load a single Snote from file.
     */
    private static Snote loadSnote(File file) throws IOException {
        Snote snote = new Snote();
        snote.setSourceFile(file);
        List<String> lines = FileSystemUtil.readFileLines(file);
        if (lines.isEmpty()) {
            throw new IOException("File is empty.");
        }
        if (lines.size() < 2) {
            throw new IOException("File has no content.");
        }
        String tagLine = lines.get(0);
        if (!tagLine.contains("#")) {
            throw new IOException("File is malformed or missing tag list.");
        }
        String[] tags = tagLine.replaceAll("#", "").split(" ");
        int tagIndex = 0;
        if (Tag.isDateValue(tags[0])) {
            tagIndex = 1;
            snote.setDate(new YMDDate(tags[0]));
        }
        for (int i = tagIndex; i < tags.length; i++) {
            if (Tag.isDateValue(tags[i])) {
                throw new IOException("Multiple date tags found.");
            }
            snote.tag(tags[i]);
        }

        int lineIndex = 1;
        if (lines.get(1).trim().isEmpty()) {
            lineIndex++; // skip first blank line.
        }

        for (int i = lineIndex; i < lines.size(); i++) {
            snote.append(lines.get(i));
            snote.newline();
        }

        return snote;
    }
}