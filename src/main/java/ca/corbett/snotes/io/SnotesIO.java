package ca.corbett.snotes.io;

import ca.corbett.extras.io.FileSystemUtil;
import ca.corbett.snotes.model.Note;
import ca.corbett.snotes.model.YMDDate;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * A utility class for loading and saving Note objects.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
public class SnotesIO {

    private static final Logger log = Logger.getLogger(SnotesIO.class.getName());

    /**
     * Attempts to load a Note from the given file, and will throw an IOException
     * if something goes wrong.
     *
     * @param file The file to load the Note from. Must be a readable file that exists on disk.
     * @return A Note object representing the content of the given file.
     * @throws IOException If anything at all goes wrong with the load.
     */
    public static Note loadNote(File file) throws IOException {
        if (file == null || !file.exists() || !file.isFile() || !file.canRead()) {
            throw new IOException("File does not exist or is not a readable file.");
        }
        Note note = new Note();
        note.setSourceFile(file);
        List<String> lines = FileSystemUtil.readFileLines(file);
        if (lines.isEmpty()) {
            throw new IOException("File is empty.");
        }

        // The first line is the tag line:
        String tagLine = lines.get(0);
        if (!tagLine.contains("#")) {
            // This used to be considered a fatal error, but eh...
            // let's just log it as a warning. It's fine.
            log.warning("Note " + file.getAbsolutePath() + " has no tags.");
        }

        else {
            // Split the tags and load each one:
            String cleanedTagLine = tagLine.replaceAll("#", "").trim();
            if (!cleanedTagLine.isEmpty()) {
                String[] tags = cleanedTagLine.split("\\s+");
                int tagIndex = 0;
                if (tags.length > 0 && YMDDate.isValidYMD(tags[0])) { // If there's a date tag, it'll be the first one
                    tagIndex = 1;
                    note.setDate(new YMDDate(tags[0]));
                }
                for (int i = tagIndex; i < tags.length; i++) {
                    if (YMDDate.isValidYMD(tags[i])) {
                        // This makes no sense and is certainly an error:
                        throw new IOException("Multiple date tags found in Note " + file.getAbsolutePath());
                    }
                    note.tag(tags[i]);
                }
            }
            else {
                log.warning("Note " + file.getAbsolutePath() + " has no parsable tags.");
            }
        }

        if (lines.size() < 2) {
            // No content, but that's fine - we'll just return an empty Note.
            note.markClean();
            return note;
        }

        int lineIndex = 1;
        if (lines.get(1).trim().isEmpty()) {
            lineIndex++; // skip first blank line.
        }

        for (int i = lineIndex; i < lines.size(); i++) {
            note.append(lines.get(i));
            note.newline();
        }

        note.markClean(); // ignore all those changes we just made to the model object - it's in sync with disk.
        return note;
    }

    /**
     * Attempts to save the given Note to disk, and will throw an IOException if something goes wrong.
     *
     * @param note The Note to save. Must not be null, and must have a non-null source file.
     * @throws IOException If anything at all goes wrong with the save.
     */
    public static void saveNote(Note note) throws IOException {
        if (note == null) {
            throw new IllegalArgumentException("Cannot save a null Note.");
        }
        if (note.getSourceFile() == null) {
            throw new IllegalArgumentException("Cannot save a Note with no source file.");
        }

        // TODO this is wrong!
        //      Dated notes require special handling, as they automatically go into folders
        //      based on the date, like 2021/01/09/note.txt
        //      If it's a first-time save, we have to compute that path.
        //      If it's an existing note, we have to check to see if the location is still good, and update it if not.
        //      Undated notes typically go into a "static" or "undated" folder, but we are not yet
        //      exposing that config property.
        //      All Notes, dated or not, get an automatic filename based on their tag list.
        //      Need to circle back to this in a future ticket! We're not handling any of it right now!
        //      And don't forget the "append if existing" option from the original application!
        //      e.g. create a new dated note with a tag of "tag" and save it when one already exists for that date...
        //           user should be prompted to append to or overwrite the existing note, and the code should be
        //           smart enough to do the right thing.
        //      The code for all of the above exists in the old Mercurial repo. Dig it up and reuse it!

        List<String> lines = new ArrayList<>();
        lines.add(note.getPersistenceTagLine());
        lines.add(""); // blank line between tags and text is conventional.

        // We'll preserve whatever line separators are in the Note's text.
        // This might be none at all if line-wrapping is enabled in the editor, but that's fine.
        lines.add(note.getText());

        // Write it:
        FileSystemUtil.writeLinesToFile(lines, note.getSourceFile());

        // If we make it this far, the Note is clean:
        note.markClean();
    }
}
