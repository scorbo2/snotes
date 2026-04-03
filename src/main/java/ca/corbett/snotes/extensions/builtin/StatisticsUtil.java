package ca.corbett.snotes.extensions.builtin;

import ca.corbett.snotes.model.Note;
import ca.corbett.snotes.model.Query;

import java.util.List;

public class StatisticsUtil {

    private StatisticsUtil() {
    }

    /**
     * For the given Note, examines the text and returns the count of words within it.
     */
    public static int countWords(Note note) {
        if (note == null || note.getText() == null || note.getText().isBlank()) {
            return 0; // easy check
        }

        // Replace everything that isn't a letter, number, or apostrophe with a space, then split on whitespace:
        // (we include apostrophes so that words like "isn't" don't get split into "isn t").
        String[] words = note.getText()
                             .replaceAll("[^\\w']", " ") // keep letters, numbers, apostrophes; turn rest into spaces
                             .trim() // remove leading/trailing spaces
                             .split("\\s+"); // split on one or more whitespace characters

        return words.length;
    }

    public static int countWords(List<Note> notes) {
        return countWords(notes, null);
    }

    public static int countWords(List<Note> notes, Query query) {
        if (notes == null || notes.isEmpty()) {
            return 0;
        }

        // If a query is provided, filter the notes first:
        if (query != null) {
            notes = query.execute(notes);
        }

        // Sum the word counts for all notes:
        return notes.stream()
                    .mapToInt(StatisticsUtil::countWords)
                    .sum();
    }
}
