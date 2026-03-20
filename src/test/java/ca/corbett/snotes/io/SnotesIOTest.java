package ca.corbett.snotes.io;

import ca.corbett.snotes.model.Note;
import ca.corbett.snotes.model.Tag;
import ca.corbett.snotes.model.YMDDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SnotesIOTest {

    @Test
    public void loadNote_withDateTagsAndText_shouldPopulateModelAndMarkClean(@TempDir Path tempDir) throws IOException {
        Path noteFile = tempDir.resolve("dated-note.snote");
        Files.write(noteFile, List.of("#1997-04-21 #work #project", "", "Line one", "Line two"));

        Note loaded = SnotesIO.loadNote(noteFile.toFile());

        assertNotNull(loaded);
        assertEquals(noteFile.toFile(), loaded.getSourceFile());
        assertTrue(loaded.hasDate());
        assertEquals("1997-04-21", loaded.getDate().toString());
        assertTrue(loaded.hasTag(new Tag("work")));
        assertTrue(loaded.hasTag(new Tag("project")));
        assertEquals("Line one" + System.lineSeparator() + "Line two" + System.lineSeparator(), loaded.getText());
        assertFalse(loaded.isDirty());
    }

    @Test
    public void loadNote_withNoTagLineMarkers_shouldLoadAsUntagged(@TempDir Path tempDir) throws IOException {
        Path noteFile = tempDir.resolve("untagged-note.snote");
        Files.write(noteFile, List.of("this is not a tag line", "", "Body text"));

        Note loaded = SnotesIO.loadNote(noteFile.toFile());

        assertFalse(loaded.hasDate());
        assertTrue(loaded.getTags().isEmpty());
        assertEquals("Body text" + System.lineSeparator(), loaded.getText());
        assertFalse(loaded.isDirty());
    }

    @Test
    public void loadNote_withNullOrMissingFile_shouldThrowIOException(@TempDir Path tempDir) {
        assertThrows(IOException.class, () -> SnotesIO.loadNote(null));
        assertThrows(IOException.class, () -> SnotesIO.loadNote(tempDir.resolve("missing.snote").toFile()));
    }

    @Test
    public void loadNote_withEmptyFile_shouldThrowIOException(@TempDir Path tempDir) throws IOException {
        Path noteFile = tempDir.resolve("empty.snote");
        Files.createFile(noteFile);

        assertThrows(IOException.class, () -> SnotesIO.loadNote(noteFile.toFile()));
    }

    @Test
    public void loadNote_withMultipleDateTags_shouldThrowIOException(@TempDir Path tempDir) throws IOException {
        Path noteFile = tempDir.resolve("bad-dates.snote");
        Files.write(noteFile, List.of("#1997-04-21 #2025-01-01 #work", "", "text"));

        assertThrows(IOException.class, () -> SnotesIO.loadNote(noteFile.toFile()));
    }

    @Test
    public void saveNote_withValidNote_shouldWriteFileAndMarkClean(@TempDir Path tempDir) throws IOException {
        Path noteFile = tempDir.resolve("save-ok.snote");
        Note note = new Note();
        note.setSourceFile(noteFile.toFile());
        note.setDate(new YMDDate("1997-04-21"));
        note.tag("work");
        note.tag("project");
        note.setText("Line one" + System.lineSeparator() + "Line two");

        assertTrue(note.isDirty());

        SnotesIO.saveNote(note);

        assertTrue(Files.exists(noteFile));
        assertFalse(note.isDirty());

        String fileContents = Files.readString(noteFile);
        assertTrue(
            fileContents.startsWith(note.getPersistenceTagLine() + System.lineSeparator() + System.lineSeparator()));
        assertTrue(fileContents.contains("Line one"));
        assertTrue(fileContents.contains("Line two"));
    }

    @Test
    public void saveNote_withNullNote_shouldThrowIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> SnotesIO.saveNote(null));
    }

    @Test
    public void saveNote_withNoSourceFile_shouldThrowIllegalArgumentException() {
        Note note = new Note();
        note.setText("hello");

        assertThrows(IllegalArgumentException.class, () -> SnotesIO.saveNote(note));
    }

    @Test
    public void saveNote_withDirectoryAsTarget_shouldThrowIOException(@TempDir Path tempDir) {
        Note note = new Note();
        File directory = tempDir.toFile();
        note.setSourceFile(directory);
        note.setText("hello");

        assertThrows(IOException.class, () -> SnotesIO.saveNote(note));
    }

}