package ca.corbett.snotes.io;

import ca.corbett.snotes.model.Note;
import ca.corbett.snotes.model.Query;
import ca.corbett.snotes.model.Template;
import ca.corbett.snotes.model.YMDDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class DataManagerTest {

    DataManager dataManager;

    @TempDir
    File tempDir;

    @BeforeEach
    void setup() {
        // Create a fresh DataManager backed by our isolated tempDir for each test:
        dataManager = new DataManager(tempDir);
    }

    // -----------------------------------------------------------------------
    // newNote tests
    // -----------------------------------------------------------------------

    @Test
    void newNote_shouldReturnDirtyNoteWithScratchFile() {
        // WHEN we create a new Note:
        Note note = dataManager.newNote();

        // THEN it should be dirty (not yet persisted):
        assertTrue(note.isDirty());

        // AND it should have a scratch file assigned inside the scratch directory:
        assertNotNull(note.getSourceFile());
        assertTrue(note.getSourceFile().exists());
        File expectedScratchDir = new File(tempDir, DataManager.SCRATCH_DIR);
        assertEquals(expectedScratchDir.getAbsolutePath(),
                note.getSourceFile().getParentFile().getAbsolutePath());

        // AND it should NOT appear in the regular notes list yet:
        assertFalse(dataManager.getNotes().contains(note));
    }

    // -----------------------------------------------------------------------
    // save tests
    // -----------------------------------------------------------------------

    @Test
    void save_scratchNote_shouldPromoteToNotesAndRemoveFromScratch() throws IOException {
        // GIVEN a new scratch note with text and a tag:
        Note note = dataManager.newNote();
        note.setText("Promoted note content");
        note.tag("promote-test");

        // WHEN we save it:
        dataManager.save(note);

        // THEN the note should appear in the regular notes list:
        assertTrue(dataManager.getNotes().contains(note));

        // AND the note should be clean:
        assertFalse(note.isDirty());

        // AND the source file should be inside the data directory, not the scratch directory:
        assertNotNull(note.getSourceFile());
        assertTrue(note.getSourceFile().exists());
        assertFalse(note.getSourceFile().getAbsolutePath().contains(DataManager.SCRATCH_DIR));
    }

    @Test
    void isScratchNote_withScratchNote_shouldReturnTrue() {
        // GIVEN a new scratch note:
        Note note = dataManager.newNote();
        File scratchFile = note.getSourceFile();
        assertNotNull(scratchFile);

        // WHEN we check if its source file is a scratch file:
        boolean isScratch = dataManager.isScratchNote(note);

        // THEN it should return true:
        assertTrue(isScratch);
    }

    @Test
    void isScratchNote_withNonScratchNotes_shouldReturnFalse() throws IOException {
        // GIVEN a couple of notes that have been promoted to real notes:
        Note note1 = dataManager.newNote();
        note1.tag("real-note-1");
        note1.setText("Real note 1 content");
        dataManager.save(note1);

        Note note2 = dataManager.newNote();
        note2.tag("real-note-2");
        note2.setText("Real note 2 content");
        dataManager.save(note2);

        // WHEN we check if their source files are scratch files:
        boolean isScratch1 = dataManager.isScratchNote(note1);
        boolean isScratch2 = dataManager.isScratchNote(note2);

        // THEN it should return false for both:
        assertFalse(isScratch1, "Expected note1 to not be a scratch note");
        assertFalse(isScratch2, "Expected note2 to not be a scratch note");

        // Also, null notes and notes with no source file should return false:
        assertFalse(dataManager.isScratchNote(null), "Expected null note to not be a scratch note");
        Note emptyNote = new Note();
        assertFalse(dataManager.isScratchNote(emptyNote), "Expected note with no source file to not be a scratch note");
    }

    @Test
    void hasCollision_withNoCollision_shouldReturnFalse() throws IOException {
        // GIVEN a note that has already been saved:
        Note note = dataManager.newNote();
        note.tag("collision-test");
        note.setText("I am the only one with this tag");
        dataManager.save(note);

        // WHEN we check for a collision with the same tag:
        boolean hasCollision = dataManager.hasCollision(note);

        // THEN it should return false because there is no other file with the same computed save path:
        assertFalse(hasCollision);
    }

    @Test
    void hasCollision_withCollision_shouldReturnTrue() throws IOException {
        // GIVEN a note that has already been saved:
        Note firstNote = dataManager.newNote();
        firstNote.tag("collision-test");
        firstNote.setText("I am the first one with this tag");
        dataManager.save(firstNote);

        // GIVEN a second scratch note with the same tag (same computed save path):
        Note secondNote = dataManager.newNote();
        secondNote.tag("collision-test");
        secondNote.setText("I will collide with the first one");

        // WHEN we check for a collision with the same tag:
        boolean hasCollision = dataManager.hasCollision(secondNote);

        // THEN it should return true because there is already a file with the same computed save path:
        assertTrue(hasCollision);
    }

    @Test
    void saveNote_withCollisionAndABORT_shouldThrow() throws IOException {
        // GIVEN a note that has already been saved:
        Note firstNote = dataManager.newNote();
        firstNote.tag("collision-abort-test");
        firstNote.setText("I am the first one with this tag");
        dataManager.save(firstNote);

        // GIVEN a second scratch note with the same tag (same computed save path):
        Note secondNote = dataManager.newNote();
        secondNote.tag("collision-abort-test");
        secondNote.setText("I will collide with the first one");

        // WHEN we try to save the second note to the same location:
        // THEN it should throw an IOException because of the collision, and should not overwrite the existing file:
        assertThrows(IOException.class, () -> dataManager.save(secondNote));

        // AND the original file should still contain the original content:
        String content = Files.readString(firstNote.getSourceFile().toPath());
        assertTrue(content.contains("I am the first one with this tag"));
    }

    @Test
    void saveNote_withCollisionAndOVERWRITE_shouldOverwrite() throws IOException {
        // GIVEN a note that has already been saved:
        Note firstNote = dataManager.newNote();
        firstNote.tag("collision-overwrite-test");
        firstNote.setText("I am the first one with this tag");
        dataManager.save(firstNote);

        // GIVEN a second scratch note with the same tag (same computed save path):
        Note secondNote = dataManager.newNote();
        secondNote.tag("collision-overwrite-test");
        secondNote.setText("I will overwrite the first one");
        assertTrue(dataManager.hasCollision(secondNote));

        // WHEN we try to save the second note to the same location with an overwrite option:
        // THEN it should overwrite the existing file without throwing an exception:
        assertDoesNotThrow(() -> dataManager.save(secondNote, DataManager.CollisionStrategy.OVERWRITE));

        // AND the original file should now contain the content of the second note:
        String content = Files.readString(firstNote.getSourceFile().toPath());
        assertTrue(content.contains("I will overwrite the first one"));

        // AND now trying to save the original note should throw, because it has been removed from cache:
        assertThrows(IOException.class, () -> dataManager.save(firstNote));
    }

    @Test
    void saveNote_withCollisionAndAPPEND_shouldAppendTo() throws IOException {
        // GIVEN a note that has already been saved:
        Note firstNote = dataManager.newNote();
        firstNote.tag("collision-overwrite-test");
        firstNote.setText("I am the first one with this tag");
        dataManager.save(firstNote);

        // GIVEN a second scratch note with the same tag (same computed save path):
        Note secondNote = dataManager.newNote();
        secondNote.tag("collision-overwrite-test");
        secondNote.setText("I will append to the first one");
        assertTrue(dataManager.hasCollision(secondNote));

        // WHEN we try to save the second note to the same location with an append option:
        // THEN it should append to the existing file without throwing an exception:
        assertDoesNotThrow(() -> dataManager.save(secondNote, DataManager.CollisionStrategy.APPEND));

        // AND the original file should now contain the content of both notes:
        String content = Files.readString(firstNote.getSourceFile().toPath());
        assertTrue(content.contains("I am the first one with this tag"));
        assertTrue(content.contains("I will append to the first one"));

        // AND now trying to save the original note should throw, because it has been removed from cache:
        assertThrows(IOException.class, () -> dataManager.save(firstNote));
    }

    @Test
    void save_modifiedNote_shouldOverwriteFileInPlace() throws IOException {
        // GIVEN a note that has already been saved (promoted):
        Note note = dataManager.newNote();
        note.tag("in-place-test");
        note.setText("Original content");
        dataManager.save(note);
        File originalFile = note.getSourceFile();
        assertNotNull(originalFile);
        assertTrue(originalFile.exists());

        // WHEN we modify the note and save again:
        note.setText("Updated content");
        dataManager.save(note);

        // THEN the note should still be saved to the same file:
        assertTrue(Files.isSameFile(originalFile.toPath(), note.getSourceFile().toPath()));

        // AND the file should contain the updated content:
        String savedContent = Files.readString(note.getSourceFile().toPath());
        assertTrue(savedContent.contains("Updated content"));
    }

    @Test
    void save_whenTargetFileAlreadyExists_shouldThrowIOException() throws IOException {
        // GIVEN a note that has already been saved:
        Note firstNote = dataManager.newNote();
        firstNote.tag("collision-tag");
        firstNote.setText("I was here first");
        dataManager.save(firstNote);
        assertTrue(firstNote.getSourceFile().exists());

        // GIVEN a second scratch note with the same tag (same computed save path):
        Note secondNote = dataManager.newNote();
        secondNote.tag("collision-tag");
        secondNote.setText("I'll cause a collision");

        // WHEN we try to save the second note to the same location:
        // THEN it should throw an IOException rather than silently overwriting the first:
        assertThrows(IOException.class, () -> dataManager.save(secondNote));
    }

    @Test
    void save_datedNote_shouldCreateDateSubdirectoryStructure() throws IOException {
        // GIVEN a dated scratch note:
        Note note = dataManager.newNote();
        note.setDate(new YMDDate("2024-06-15"));
        note.tag("dated-save-test");
        note.setText("A dated note");

        // WHEN we save it:
        dataManager.save(note);

        // THEN the file should exist inside the year/month/day directory structure:
        assertNotNull(note.getSourceFile());
        assertTrue(note.getSourceFile().exists());
        Path relativePath = tempDir.toPath().relativize(note.getSourceFile().toPath());
        assertEquals("2024", relativePath.getName(0).toString(), "Expected year directory");
        assertEquals("06", relativePath.getName(1).toString(), "Expected month directory");
        assertEquals("15", relativePath.getName(2).toString(), "Expected day directory");
    }

    // -----------------------------------------------------------------------
    // saveScratch tests
    // -----------------------------------------------------------------------

    @Test
    void saveScratch_withNullSourceFile_shouldThrowIOException() {
        // GIVEN a scratch note whose source file has been set to null
        // (simulating a scenario where scratch file creation fails):
        Note note = dataManager.newNote();
        note.setSourceFile(null);

        // WHEN we try to save it in-place:
        // THEN it should throw an IOException because there is nowhere to persist it:
        assertThrows(IOException.class, () -> dataManager.saveScratch(note));
    }

    @Test
    void saveScratch_scratchNote_shouldPersistInScratchDirWithoutPromotion() throws IOException {
        // GIVEN a scratch note with content:
        Note note = dataManager.newNote();
        note.setText("Scratch auto-save content");
        note.tag("scratch-persist-test");
        File scratchFile = note.getSourceFile();
        assertNotNull(scratchFile);

        // WHEN we save it in-place as a scratch note:
        dataManager.saveScratch(note);

        // THEN the note should still NOT be in the regular notes list (no promotion):
        assertFalse(dataManager.getNotes().contains(note));

        // AND the scratch file should exist and contain the saved content:
        assertTrue(scratchFile.exists());
        String content = Files.readString(scratchFile.toPath());
        assertTrue(content.contains("Scratch auto-save content"));

        // AND the note should be clean:
        assertFalse(note.isDirty());
    }

    @Test
    void saveScratch_nonScratchNote_shouldReturnWithoutModifyingFile() throws IOException {
        // GIVEN a note that has already been promoted to a real note:
        Note note = dataManager.newNote();
        note.tag("non-scratch-test");
        note.setText("Original real content");
        dataManager.save(note);
        assertTrue(dataManager.getNotes().contains(note));
        File realFile = note.getSourceFile();

        // WHEN we modify the note and call saveScratch on it (it is no longer a scratch note):
        note.setText("Modified content - should not be scratch-saved");
        dataManager.saveScratch(note);

        // THEN the file should still contain the original content (saveScratch did nothing):
        String content = Files.readString(realFile.toPath());
        assertFalse(content.contains("Modified content - should not be scratch-saved"),
                    "saveScratch should not save a note that is no longer in the scratch list");

        // AND the note should still be dirty (saveScratch did not mark it clean):
        assertTrue(note.isDirty());
    }

    // -----------------------------------------------------------------------
    // saveAll tests
    // -----------------------------------------------------------------------

    @Test
    void saveAll_withDirtyScratchNote_shouldPersistItInPlace() throws IOException {
        // GIVEN a dirty scratch note (new notes are dirty by default):
        Note scratchNote = dataManager.newNote();
        scratchNote.tag("saveall-scratch-test");
        scratchNote.setText("Auto-saved scratch content");
        assertTrue(scratchNote.isDirty());
        File scratchFile = scratchNote.getSourceFile();
        assertNotNull(scratchFile);

        // WHEN we call saveAll:
        dataManager.saveAll();

        // THEN the scratch note should be clean:
        assertFalse(scratchNote.isDirty());

        // AND the scratch file should contain the content:
        String content = Files.readString(scratchFile.toPath());
        assertTrue(content.contains("Auto-saved scratch content"));

        // AND the note should NOT have been promoted to the regular notes list:
        assertFalse(dataManager.getNotes().contains(scratchNote));
    }

    @Test
    void saveAll_withDirtyPromotedNote_shouldPersistAndMarkClean() throws IOException {
        // GIVEN a note that was promoted from scratch and then modified:
        Note note = dataManager.newNote();
        note.tag("saveall-promoted-test");
        note.setText("Original content");
        dataManager.save(note);
        assertFalse(note.isDirty());

        // WHEN we modify the note (making it dirty again) and call saveAll:
        note.setText("Updated by saveAll");
        assertTrue(note.isDirty());
        dataManager.saveAll();

        // THEN the note should be clean:
        assertFalse(note.isDirty());

        // AND the persisted file should contain the updated content:
        String content = Files.readString(note.getSourceFile().toPath());
        assertTrue(content.contains("Updated by saveAll"));
    }

    @Test
    void saveAll_withNoNotes_shouldCompleteWithoutError() {
        // GIVEN a fresh DataManager with no notes:
        // (dataManager is already empty from @BeforeEach setup)

        // WHEN we call saveAll:
        // THEN it should complete without throwing any exception:
        assertDoesNotThrow(() -> dataManager.saveAll());
    }

    // -----------------------------------------------------------------------
    // loadAll tests
    // -----------------------------------------------------------------------

    @Test
    void loadAll_withNullDir_shouldThrowIOException() {
        // WHEN we try to load from a null directory:
        // THEN it should throw an IOException immediately:
        DataManager manager = new DataManager(null);
        assertThrows(IOException.class, () -> manager.loadAll(null));
    }

    @Test
    void loadAll_withFileInsteadOfDirectory_shouldThrowIOException() throws IOException {
        // GIVEN a regular file (not a directory):
        File notADirectory = File.createTempFile("not-a-dir", ".txt", tempDir);
        assertTrue(notADirectory.isFile());
        DataManager manager = new DataManager(notADirectory);

        // WHEN we try to load from it:
        // THEN it should throw an IOException:
        assertThrows(IOException.class, () -> manager.loadAll());
    }

    @Test
    void loadAll_whenMetadataDirIsFile_shouldThrowIOException() throws IOException {
        // GIVEN a data directory where the expected metadata subdirectory already exists as a plain file:
        File dataDir = new File(tempDir, "data-meta-file");
        assertTrue(dataDir.mkdirs());
        File metadataAsFile = new File(dataDir, DataManager.METADATA_DIR);
        assertTrue(metadataAsFile.createNewFile());
        DataManager manager = new DataManager(dataDir);

        // WHEN we try to load from that directory:
        // THEN it should throw an IOException because the metadata dir is not a directory:
        assertThrows(IOException.class, () -> manager.loadAll());
    }

    @Test
    void loadAll_whenStaticDirIsFile_shouldThrowIOException() throws IOException {
        // GIVEN a data directory where the expected static subdirectory already exists as a plain file:
        File dataDir = new File(tempDir, "data-static-file");
        assertTrue(dataDir.mkdirs());
        File staticAsFile = new File(dataDir, DataManager.STATIC_DIR);
        assertTrue(staticAsFile.createNewFile());
        DataManager manager = new DataManager(dataDir);

        // WHEN we try to load from that directory:
        // THEN it should throw an IOException because the static dir is not a directory:
        assertThrows(IOException.class, () -> manager.loadAll());
    }

    @Test
    void loadAll_whenScratchDirIsFile_shouldThrowIOException() throws IOException {
        // GIVEN a data directory where the expected scratch subdirectory already exists as a plain file:
        File dataDir = new File(tempDir, "data-scratch-file");
        assertTrue(dataDir.mkdirs());
        DataManager manager = new DataManager(dataDir);
        // The metadata and static dirs must exist as real directories first so we reach the scratch check:
        assertTrue(new File(dataDir, DataManager.METADATA_DIR).mkdirs());
        assertTrue(new File(dataDir, DataManager.STATIC_DIR).mkdirs());
        File scratchAsFile = new File(dataDir, DataManager.SCRATCH_DIR);
        assertTrue(scratchAsFile.createNewFile());

        // WHEN we try to load from that directory:
        // THEN it should throw an IOException because the scratch dir is not a directory:
        assertThrows(IOException.class, () -> manager.loadAll());
    }

    @Test
    void isQueryNameAvailable_withUniqueName_shouldReturnTrue() {
        try {
            // GIVEN a DataManager with a couple of existing queries:
            Query query1 = new Query();
            query1.setName("Existing Query 1");
            dataManager.saveQuery(query1);
            Query query2 = new Query();
            query2.setName("Existing Query 2");
            dataManager.saveQuery(query2);

            // WHEN we check for the availability of a unique query name:
            String newName = "Unique Query Name";
            boolean isAvailable = dataManager.isQueryNameAvailable(newName);

            // THEN it should return true:
            assertTrue(isAvailable);
        }
        catch (IOException ioe) {
            fail("Unexpected IOException during test setup: " + ioe.getMessage());
        }
    }

    @Test
    void isQueryNameAvailable_withDuplicateName_shouldReturnFalse() {
        try {
            // GIVEN a DataManager with an existing query:
            Query existingQuery = new Query();
            existingQuery.setName("Duplicate Query Name");
            dataManager.saveQuery(existingQuery);

            // WHEN we check for the availability of the same name:
            boolean isAvailable = dataManager.isQueryNameAvailable("Duplicate Query Name");

            // THEN it should return false:
            assertFalse(isAvailable);
        }
        catch (IOException ioe) {
            fail("Unexpected IOException during test setup: " + ioe.getMessage());
        }
    }

    @Test
    void isQueryNameAvailable_withNullName_shouldThrow() {
        // WHEN we check for the availability of a null name:
        // THEN it should immediately throw IllegalArgumentException:
        assertThrows(IllegalArgumentException.class, () -> dataManager.isQueryNameAvailable(null));
    }

    @Test
    void isQueryNameAvailable_withExcludedNameMatches_shouldReturnTrue() {
        try {
            // GIVEN a DataManager with an existing query:
            final String queryName = "Existing Query";
            Query existingQuery = new Query();
            existingQuery.setName(queryName);
            dataManager.saveQuery(existingQuery);

            // WHEN we check for the availability of the same name but exclude that very name:
            boolean isAvailable = dataManager.isQueryNameAvailable(queryName, queryName);

            // THEN it should return true because we are excluding the existing query from the check:
            assertTrue(isAvailable);
        }
        catch (IOException ioe) {
            fail("Unexpected IOException during test setup: " + ioe.getMessage());
        }
    }

    @Test
    void isQueryNameAvailable_withExcludedNameDoesNotMatch_shouldReturnFalse() {
        try {
            // GIVEN a DataManager with an existing query:
            final String existingName = "Existing Query";
            Query existingQuery = new Query();
            existingQuery.setName(existingName);
            dataManager.saveQuery(existingQuery);

            // WHEN we check for the availability of the same name but exclude a different name:
            boolean isAvailable = dataManager.isQueryNameAvailable(existingName, "Some Other Name");

            // THEN it should return false because the existing query is not being excluded:
            assertFalse(isAvailable);
        }
        catch (IOException ioe) {
            fail("Unexpected IOException during test setup: " + ioe.getMessage());
        }
    }

    // -----------------------------------------------------------------------
    // isTemplateNameAvailable tests
    // -----------------------------------------------------------------------

    @Test
    void isTemplateNameAvailable_withUniqueName_shouldReturnTrue() {
        try {
            // GIVEN a DataManager with a couple of existing templates:
            Template template1 = new Template();
            template1.setName("Existing Template 1");
            dataManager.saveTemplate(template1);
            Template template2 = new Template();
            template2.setName("Existing Template 2");
            dataManager.saveTemplate(template2);

            // WHEN we check for the availability of a unique template name:
            String newName = "Unique Template Name";
            boolean isAvailable = dataManager.isTemplateNameAvailable(newName);

            // THEN it should return true:
            assertTrue(isAvailable);
        }
        catch (IOException ioe) {
            fail("Unexpected IOException during test setup: " + ioe.getMessage());
        }
    }

    @Test
    void isTemplateNameAvailable_withDuplicateName_shouldReturnFalse() {
        try {
            // GIVEN a DataManager with an existing template:
            Template existingTemplate = new Template();
            existingTemplate.setName("Duplicate Template Name");
            dataManager.saveTemplate(existingTemplate);

            // WHEN we check for the availability of the same name:
            boolean isAvailable = dataManager.isTemplateNameAvailable("Duplicate Template Name");

            // THEN it should return false:
            assertFalse(isAvailable);
        }
        catch (IOException ioe) {
            fail("Unexpected IOException during test setup: " + ioe.getMessage());
        }
    }

    @Test
    void isTemplateNameAvailable_withNullName_shouldThrow() {
        // WHEN we check for the availability of a null name:
        // THEN it should immediately throw IllegalArgumentException:
        assertThrows(IllegalArgumentException.class, () -> dataManager.isTemplateNameAvailable(null));
    }

    @Test
    void isTemplateNameAvailable_withExcludedNameMatches_shouldReturnTrue() {
        try {
            // GIVEN a DataManager with an existing template:
            final String templateName = "Existing Template";
            Template existingTemplate = new Template();
            existingTemplate.setName(templateName);
            dataManager.saveTemplate(existingTemplate);

            // WHEN we check for the availability of the same name but exclude that very name:
            boolean isAvailable = dataManager.isTemplateNameAvailable(templateName, templateName);

            // THEN it should return true because we are excluding the existing template from the check:
            assertTrue(isAvailable);
        }
        catch (IOException ioe) {
            fail("Unexpected IOException during test setup: " + ioe.getMessage());
        }
    }

    @Test
    void isTemplateNameAvailable_withExcludedNameDoesNotMatch_shouldReturnFalse() {
        try {
            // GIVEN a DataManager with an existing template:
            final String existingName = "Existing Template";
            Template existingTemplate = new Template();
            existingTemplate.setName(existingName);
            dataManager.saveTemplate(existingTemplate);

            // WHEN we check for the availability of the same name but exclude a different name:
            boolean isAvailable = dataManager.isTemplateNameAvailable(existingName, "Some Other Name");

            // THEN it should return false because the existing template is not being excluded:
            assertFalse(isAvailable);
        }
        catch (IOException ioe) {
            fail("Unexpected IOException during test setup: " + ioe.getMessage());
        }
    }

    @Test
    void deleteQuery_withNull_shouldThrow() {
        // WHEN we try to delete a null query:
        // THEN it should throw IllegalArgumentException:
        assertThrows(IllegalArgumentException.class, () -> dataManager.delete((Query)null));
    }

    @Test
    void deleteQuery_withQueryNotInCache_shouldDoNothing() {
        try {
            // GIVEN a DataManager with a couple of queries:
            Query query1 = new Query();
            query1.setName("Query 1");
            dataManager.saveQuery(query1);
            Query query2 = new Query();
            query2.setName("Query 2");
            dataManager.saveQuery(query2);

            // WHEN we delete a Query instance that is not contained by our dataManager:
            Query notInCache = new Query();
            notInCache.setName("Not In Cache");
            assertFalse(dataManager.getQueries().contains(notInCache));
            assertDoesNotThrow(() -> dataManager.delete(notInCache));

            // THEN there should be no change to the existing queries:
            assertEquals(2, dataManager.getQueries().size());
            assertTrue(dataManager.getQueries().contains(query1));
            assertTrue(dataManager.getQueries().contains(query2));
        }
        catch (IOException ioe) {
            fail("Unexpected IOException during test setup: " + ioe.getMessage());
        }
    }

    @Test
    void deleteQuery_withQueryInCache_shouldRemove() {
        try {
            // GIVEN a DataManager with a couple of queries:
            Query query1 = new Query();
            query1.setName("Query 1");
            dataManager.saveQuery(query1);
            Query query2 = new Query();
            query2.setName("Query 2");
            dataManager.saveQuery(query2);

            // WHEN we delete one of the existing queries:
            assertTrue(dataManager.getQueries().contains(query1));
            dataManager.delete(query1);

            // THEN it should be removed from the cache:
            assertFalse(dataManager.getQueries().contains(query1));

            // AND the other query should still exist:
            assertTrue(dataManager.getQueries().contains(query2));
        }
        catch (IOException ioe) {
            fail("Unexpected IOException during test setup: " + ioe.getMessage());
        }
    }
}