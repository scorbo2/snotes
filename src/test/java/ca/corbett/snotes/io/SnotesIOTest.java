package ca.corbett.snotes.io;

import ca.corbett.extras.io.FileSystemUtil;
import ca.corbett.snotes.model.Note;
import ca.corbett.snotes.model.Query;
import ca.corbett.snotes.model.Tag;
import ca.corbett.snotes.model.Template;
import ca.corbett.snotes.model.YMDDate;
import ca.corbett.snotes.model.filter.DateFilter;
import ca.corbett.snotes.model.filter.DayOfMonthFilter;
import ca.corbett.snotes.model.filter.DayOfWeekFilter;
import ca.corbett.snotes.model.filter.MonthFilter;
import ca.corbett.snotes.model.filter.TagFilter;
import ca.corbett.snotes.model.filter.TextFilter;
import ca.corbett.snotes.model.filter.UndatedFilter;
import ca.corbett.snotes.model.filter.YearFilter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class SnotesIOTest {

    static final YMDDate JAN_1_2020 = new YMDDate("2020-01-01");

    @TempDir
    File tempDir;

    @Test
    public void saveQuery_roundTrip_shouldLoad() {
        // GIVEN a Query with some filters and a name:
        Query query = new Query();
        query.setName("My Test Query");
        query.addFilter(new YearFilter(1997, YearFilter.FilterType.ON));
        query.addFilter(new MonthFilter(4, MonthFilter.FilterType.IS));
        query.addFilter(new TextFilter("test"));
        query.addFilter(new DateFilter(JAN_1_2020, DateFilter.FilterType.AFTER_INCLUSIVE));
        query.addFilter(new DayOfWeekFilter(DayOfWeek.MONDAY, DayOfWeekFilter.FilterType.IS));
        query.addFilter(new DayOfMonthFilter(21, DayOfMonthFilter.FilterType.IS));
        query.addFilter(new TagFilter(List.of(new Tag("test-tag")), TagFilter.FilterType.ALL));
        query.addFilter(new UndatedFilter());

        try {
            // WHEN we save it to a file and then load it back:
            File savedFile = File.createTempFile("test", ".query", tempDir);
            SnotesIO.saveQuery(query, savedFile);
            assertNotNull(savedFile);
            assertTrue(savedFile.exists());
            Query loadedQuery = SnotesIO.loadQuery(savedFile);

            // THEN the loaded Query should have the same name and filters as the original:
            assertNotNull(loadedQuery);
            assertEquals(query.getName(), loadedQuery.getName());
            assertEquals(query.size(), loadedQuery.size());

            // AND the filters should be present in the same order, so let's go through them and verify types and values
            assertInstanceOf(YearFilter.class, loadedQuery.getFilters().get(0));
            YearFilter loadedYearFilter = (YearFilter)loadedQuery.getFilters().get(0);
            assertEquals(1997, loadedYearFilter.getTargetYear());
            assertEquals(YearFilter.FilterType.ON, loadedYearFilter.getFilterType());
            assertInstanceOf(MonthFilter.class, loadedQuery.getFilters().get(1));
            MonthFilter loadedMonthFilter = (MonthFilter)loadedQuery.getFilters().get(1);
            assertEquals(4, loadedMonthFilter.getTargetMonth());
            assertEquals(MonthFilter.FilterType.IS, loadedMonthFilter.getFilterType());
            assertInstanceOf(TextFilter.class, loadedQuery.getFilters().get(2));
            TextFilter loadedTextFilter = (TextFilter)loadedQuery.getFilters().get(2);
            assertEquals("test", loadedTextFilter.getContains());
            assertInstanceOf(DateFilter.class, loadedQuery.getFilters().get(3));
            DateFilter loadedDateFilter = (DateFilter)loadedQuery.getFilters().get(3);
            assertEquals(JAN_1_2020, loadedDateFilter.getTargetDate());
            assertEquals(DateFilter.FilterType.AFTER_INCLUSIVE, loadedDateFilter.getFilterType());
            assertInstanceOf(DayOfWeekFilter.class, loadedQuery.getFilters().get(4));
            DayOfWeekFilter loadedDayOfWeekFilter = (DayOfWeekFilter)loadedQuery.getFilters().get(4);
            assertEquals(DayOfWeek.MONDAY, loadedDayOfWeekFilter.getDayOfWeek());
            assertEquals(DayOfWeekFilter.FilterType.IS, loadedDayOfWeekFilter.getFilterType());
            assertInstanceOf(DayOfMonthFilter.class, loadedQuery.getFilters().get(5));
            DayOfMonthFilter loadedDayOfMonthFilter = (DayOfMonthFilter)loadedQuery.getFilters().get(5);
            assertEquals(21, loadedDayOfMonthFilter.getDayOfMonth());
            assertEquals(DayOfMonthFilter.FilterType.IS, loadedDayOfMonthFilter.getFilterType());
            assertInstanceOf(TagFilter.class, loadedQuery.getFilters().get(6));
            TagFilter loadedTagFilter = (TagFilter)loadedQuery.getFilters().get(6);
            assertEquals(1, loadedTagFilter.getTagsToFilter().size());
            assertEquals("test-tag", loadedTagFilter.getTagsToFilter().get(0).getTag());
            assertEquals(TagFilter.FilterType.ALL, loadedTagFilter.getFilterType());
            assertInstanceOf(UndatedFilter.class, loadedQuery.getFilters().get(7));
        }
        catch (IOException ioe) {
            fail("IOException thrown during save/load: " + ioe.getMessage());
        }
    }

    @Test
    public void loadQuery_withMalformedDate_shouldThrowIOException() throws IOException {
        // GIVEN a Query saved with a valid DateFilter:
        Query query = new Query();
        query.setName("Malformed Date Test");
        query.addFilter(new DateFilter(JAN_1_2020, DateFilter.FilterType.ON));
        File savedFile = File.createTempFile("test-malformed", ".query", tempDir);
        SnotesIO.saveQuery(query, savedFile);

        // WHEN we corrupt the saved file by replacing the valid date with a malformed one:
        String content = Files.readString(savedFile.toPath(), StandardCharsets.UTF_8);
        String corrupted = content.replace("2020-01-01", "not-a-real-date");
        Files.writeString(savedFile.toPath(), corrupted, StandardCharsets.UTF_8);

        // THEN Query.load() should throw an IOException rather than silently
        // loading a filter with today's date substituted in:
        assertThrows(IOException.class, () -> SnotesIO.loadQuery(savedFile));
    }

    @Test
    public void saveTemplate_withNullFile_shouldThrowIllegalArgumentException() {
        // GIVEN a Template:
        Template template = new Template();

        // WHEN we try to save to a null file:
        // THEN it should throw an IllegalArgumentException:
        assertThrows(IllegalArgumentException.class, () -> SnotesIO.saveTemplate(template, null));
    }

    @Test
    public void saveTemplate_withDirectory_shouldThrowIOException() {
        // GIVEN a Template:
        Template template = new Template();

        // WHEN we try to save to a directory:
        // THEN it should throw an IOException:
        assertThrows(IOException.class, () -> SnotesIO.saveTemplate(template, tempDir));
    }

    @Test
    public void saveTemplate_withNullTemplate_shouldThrowIllegalArgumentException() {
        // WHEN we try to save a null Template:
        // THEN it should throw an IllegalArgumentException:
        File f = new File(tempDir, "template.template");
        assertThrows(IllegalArgumentException.class, () -> SnotesIO.saveTemplate(null, f));
    }

    // -----------------------------------------------------------------------
    // load tests
    // -----------------------------------------------------------------------

    @Test
    public void loadTemplate_withNullFile_shouldThrowIllegalArgumentException() {
        // WHEN we try to load from a null file:
        // THEN it should throw an IllegalArgumentException:
        assertThrows(IllegalArgumentException.class, () -> SnotesIO.loadTemplate(null));
    }

    @Test
    public void loadTemplate_withNonExistentFile_shouldThrowIOException() {
        // GIVEN a file that does not exist:
        File nonExistent = new File(tempDir, "does-not-exist.template");

        // WHEN we try to load from it:
        // THEN it should throw an IOException:
        assertThrows(IOException.class, () -> SnotesIO.loadTemplate(nonExistent));
    }

    // -----------------------------------------------------------------------
    // load with malformed or edge-case content tests
    // -----------------------------------------------------------------------

    @Test
    public void loadTemplate_withEmptyFile_shouldThrowIOException() throws IOException {
        // GIVEN a file that is completely empty (Jackson readTree returns null for empty input):
        File emptyFile = File.createTempFile("empty", ".template", tempDir);

        // WHEN we try to load from it:
        // THEN it should throw an IOException:
        assertThrows(IOException.class, () -> SnotesIO.loadTemplate(emptyFile));
    }

    @Test
    public void loadTemplate_withMalformedJson_shouldThrowIOException() throws IOException {
        // GIVEN a file containing invalid JSON syntax:
        File malformedFile = File.createTempFile("malformed", ".template", tempDir);
        Files.writeString(malformedFile.toPath(), "{this is not : valid [ json !!!");

        // WHEN we try to load from it:
        // THEN Jackson throws JsonParseException (an IOException subclass):
        assertThrows(IOException.class, () -> SnotesIO.loadTemplate(malformedFile));
    }

    @Test
    public void loadTemplate_withMissingNameField_shouldUseDefaultName() throws IOException {
        // GIVEN a valid JSON file that has no "name" field at all:
        File file = File.createTempFile("no-name", ".template", tempDir);
        Files.writeString(file.toPath(),
                          "{\"dateOption\":\"NONE\",\"context\":\"NONE\",\"tags\":[]}");

        // WHEN we load it:
        Template loaded = SnotesIO.loadTemplate(file);

        // THEN the name should fall back to DEFAULT_NAME:
        assertEquals(Template.DEFAULT_NAME, loaded.getName());
    }

    @Test
    public void loadTemplate_withBlankName_shouldUseDefaultName() throws IOException {
        // GIVEN a valid JSON file where the "name" value is blank whitespace:
        File file = File.createTempFile("blank-name", ".template", tempDir);
        Files.writeString(file.toPath(),
                          "{\"name\":\"   \",\"dateOption\":\"NONE\",\"context\":\"NONE\",\"tags\":[]}");

        // WHEN we load it:
        Template loaded = SnotesIO.loadTemplate(file);

        // THEN the name should fall back to DEFAULT_NAME:
        assertEquals(Template.DEFAULT_NAME, loaded.getName());
    }

    @Test
    public void loadTemplate_withNullNameNode_shouldUseDefaultName() throws IOException {
        // GIVEN a valid JSON file where "name" is explicitly JSON null:
        File file = File.createTempFile("null-name", ".template", tempDir);
        Files.writeString(file.toPath(),
                          "{\"name\":null,\"dateOption\":\"NONE\",\"context\":\"NONE\",\"tags\":[]}");

        // WHEN we load it:
        Template loaded = SnotesIO.loadTemplate(file);

        // THEN the name should fall back to DEFAULT_NAME:
        assertEquals(Template.DEFAULT_NAME, loaded.getName());
    }

    @Test
    public void loadTemplate_withInvalidDateOption_shouldUseNoneDateOption() throws IOException {
        // GIVEN a JSON file with an unrecognized "dateOption" string:
        File file = File.createTempFile("bad-date", ".template", tempDir);
        Files.writeString(file.toPath(),
                          "{\"name\":\"Test\",\"dateOption\":\"INVALID_OPTION\",\"context\":\"NONE\",\"tags\":[]}");

        // WHEN we load it:
        Template loaded = SnotesIO.loadTemplate(file);

        // THEN the dateOption should fall back to NONE:
        assertEquals(Template.DateOption.NONE, loaded.getDateOption());
    }

    @Test
    public void loadTemplate_withMissingDateOptionField_shouldUseNoneDateOption() throws IOException {
        // GIVEN a JSON file with no "dateOption" field:
        File file = File.createTempFile("no-date", ".template", tempDir);
        Files.writeString(file.toPath(),
                          "{\"name\":\"Test\",\"context\":\"NONE\",\"tags\":[]}");

        // WHEN we load it:
        Template loaded = SnotesIO.loadTemplate(file);

        // THEN the dateOption should fall back to NONE:
        assertEquals(Template.DateOption.NONE, loaded.getDateOption());
    }

    @Test
    public void loadTemplate_withNullDateOptionNode_shouldUseNoneDateOption() throws IOException {
        // GIVEN a JSON file where "dateOption" is explicitly JSON null:
        File file = File.createTempFile("null-date", ".template", tempDir);
        Files.writeString(file.toPath(),
                          "{\"name\":\"Test\",\"dateOption\":null,\"context\":\"NONE\",\"tags\":[]}");

        // WHEN we load it:
        Template loaded = SnotesIO.loadTemplate(file);

        // THEN the dateOption should fall back to NONE:
        assertEquals(Template.DateOption.NONE, loaded.getDateOption());
    }

    @Test
    public void loadTemplate_withInvalidContext_shouldUseNoneContext() throws IOException {
        // GIVEN a JSON file with an unrecognized "context" string:
        File file = File.createTempFile("bad-ctx", ".template", tempDir);
        Files.writeString(file.toPath(),
                          "{\"name\":\"Test\",\"dateOption\":\"NONE\",\"context\":\"INVALID_CONTEXT\",\"tags\":[]}");

        // WHEN we load it:
        Template loaded = SnotesIO.loadTemplate(file);

        // THEN the context should fall back to NONE:
        assertEquals(Template.Context.NONE, loaded.getContext());
    }

    @Test
    public void loadTemplate_withMissingContextField_shouldUseNoneContext() throws IOException {
        // GIVEN a JSON file with no "context" field:
        File file = File.createTempFile("no-ctx", ".template", tempDir);
        Files.writeString(file.toPath(),
                          "{\"name\":\"Test\",\"dateOption\":\"NONE\",\"tags\":[]}");

        // WHEN we load it:
        Template loaded = SnotesIO.loadTemplate(file);

        // THEN the context should fall back to NONE:
        assertEquals(Template.Context.NONE, loaded.getContext());
    }

    @Test
    public void loadTemplate_withNullContextNode_shouldUseNoneContext() throws IOException {
        // GIVEN a JSON file where "context" is explicitly JSON null:
        File file = File.createTempFile("null-ctx", ".template", tempDir);
        Files.writeString(file.toPath(),
                          "{\"name\":\"Test\",\"dateOption\":\"NONE\",\"context\":null,\"tags\":[]}");

        // WHEN we load it:
        Template loaded = SnotesIO.loadTemplate(file);

        // THEN the context should fall back to NONE:
        assertEquals(Template.Context.NONE, loaded.getContext());
    }

    @Test
    public void loadTemplate_withNonTextualTagValues_shouldSkipInvalidTags() throws IOException {
        // GIVEN a JSON file where the tags array contains a mix of valid strings and non-string values:
        File file = File.createTempFile("bad-tags", ".template", tempDir);
        Files.writeString(file.toPath(),
                          "{\"name\":\"Test\",\"dateOption\":\"NONE\",\"context\":\"NONE\"," +
                              "\"tags\":[\"valid-tag\",42,null,true]}");

        // WHEN we load it:
        Template loaded = SnotesIO.loadTemplate(file);

        // THEN only the valid textual tag should be present; non-strings are silently skipped:
        assertEquals(1, loaded.getTagList().size());
        assertEquals("valid-tag", loaded.getTagList().get(0).getTag());
    }

    @Test
    public void loadTemplate_withTagsNotArray_shouldHaveNoTags() throws IOException {
        // GIVEN a JSON file where "tags" is a plain string rather than an array:
        File file = File.createTempFile("tags-not-array", ".template", tempDir);
        Files.writeString(file.toPath(),
                          "{\"name\":\"Test\",\"dateOption\":\"NONE\",\"context\":\"NONE\"," +
                              "\"tags\":\"not-an-array\"}");

        // WHEN we load it:
        Template loaded = SnotesIO.loadTemplate(file);

        // THEN the tag list should be empty (non-array tags field is silently ignored):
        assertTrue(loaded.getTagList().isEmpty());
    }

    @Test
    public void loadTemplate_withMissingTagsField_shouldHaveNoTags() throws IOException {
        // GIVEN a JSON file with no "tags" field at all:
        File file = File.createTempFile("no-tags", ".template", tempDir);
        Files.writeString(file.toPath(),
                          "{\"name\":\"Test\",\"dateOption\":\"NONE\",\"context\":\"NONE\"}");

        // WHEN we load it:
        Template loaded = SnotesIO.loadTemplate(file);

        // THEN the tag list should be empty:
        assertTrue(loaded.getTagList().isEmpty());
    }

    // -----------------------------------------------------------------------
    // Round-trip test
    // -----------------------------------------------------------------------

    @Test
    public void saveTemplate_roundTrip_shouldLoad() {
        // GIVEN a fully populated Template:
        Template template = new Template("My Test Template");
        template.setDateOption(Template.DateOption.TODAY);
        template.setContext(Template.Context.MOST_RECENT3);
        template.addTag("work");
        template.addTag("project-alpha");

        try {
            // WHEN we save it to a file and then load it back:
            File savedFile = File.createTempFile("test", ".template", tempDir);
            SnotesIO.saveTemplate(template, savedFile);
            assertNotNull(savedFile);
            assertTrue(savedFile.exists());
            Template loaded = SnotesIO.loadTemplate(savedFile);

            // THEN the loaded Template should match the original in all fields:
            assertNotNull(loaded);
            assertEquals(template.getName(), loaded.getName());
            assertEquals(template.getDateOption(), loaded.getDateOption());
            assertEquals(template.getContext(), loaded.getContext());

            // AND the tags should match:
            List<Tag> originalTags = template.getTagList();
            List<Tag> loadedTags = loaded.getTagList();
            assertEquals(originalTags.size(), loadedTags.size());
            for (int i = 0; i < originalTags.size(); i++) {
                assertEquals(originalTags.get(i).getTag(), loadedTags.get(i).getTag());
            }
        }
        catch (IOException ioe) {
            fail("IOException thrown during save/load: " + ioe.getMessage());
        }
    }

    @Test
    public void saveTemplate_roundTrip_withNoTagsAndDefaultValues_shouldLoad() {
        // GIVEN a minimal Template with default values and no tags:
        Template template = new Template();

        try {
            // WHEN we save it to a file and then load it back:
            File savedFile = File.createTempFile("test-minimal", ".template", tempDir);
            SnotesIO.saveTemplate(template, savedFile);
            Template loaded = SnotesIO.loadTemplate(savedFile);

            // THEN all default values should be preserved:
            assertNotNull(loaded);
            assertEquals(Template.DEFAULT_NAME, loaded.getName());
            assertEquals(Template.DateOption.NONE, loaded.getDateOption());
            assertEquals(Template.Context.NONE, loaded.getContext());
            assertTrue(loaded.getTagList().isEmpty());
        }
        catch (IOException ioe) {
            fail("IOException thrown during save/load: " + ioe.getMessage());
        }
    }

    @Test
    public void saveTemplate_roundTrip_withAllDateOptions_shouldLoad() {
        // GIVEN a Template saved with each possible DateOption:
        for (Template.DateOption option : Template.DateOption.values()) {
            Template template = new Template("DateOption Test");
            template.setDateOption(option);

            try {
                File savedFile = File.createTempFile("test-date-" + option.name(), ".template", tempDir);
                SnotesIO.saveTemplate(template, savedFile);
                Template loaded = SnotesIO.loadTemplate(savedFile);

                // THEN the loaded Template should have the same DateOption:
                assertEquals(option, loaded.getDateOption(),
                             "DateOption mismatch for: " + option);
            }
            catch (IOException ioe) {
                fail("IOException thrown for DateOption " + option + ": " + ioe.getMessage());
            }
        }
    }

    @Test
    public void saveTemplate_roundTrip_withAllContextOptions_shouldLoad() {
        // GIVEN a Template saved with each possible Context option:
        for (Template.Context ctx : Template.Context.values()) {
            Template template = new Template("Context Test");
            template.setContext(ctx);

            try {
                File savedFile = File.createTempFile("test-ctx-" + ctx.name(), ".template", tempDir);
                SnotesIO.saveTemplate(template, savedFile);
                Template loaded = SnotesIO.loadTemplate(savedFile);

                // THEN the loaded Template should have the same Context:
                assertEquals(ctx, loaded.getContext(),
                             "Context mismatch for: " + ctx);
            }
            catch (IOException ioe) {
                fail("IOException thrown for Context " + ctx + ": " + ioe.getMessage());
            }
        }
    }

    @Test
    public void loadNote_withDateTagsAndText_shouldPopulateModelAndMarkClean(@TempDir Path tempDir) throws IOException {
        Path noteFile = tempDir.resolve("dated-note.snote");
        Files.write(noteFile, List.of("#1997-04-21 #work #project", "", "Line one", "Line two"));

        Note loaded = SnotesIO.loadNote(noteFile.toFile());

        assertNotNull(loaded);
        assertEquals(noteFile.toFile(), loaded.getSourceFile());
        assertTrue(loaded.hasDate());
        assertNotNull(loaded.getDate());
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

        SnotesIO.saveNote(note, note.getSourceFile());

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
        assertThrows(IllegalArgumentException.class, () -> SnotesIO.saveNote(null, new File("a")));
    }

    @Test
    public void saveNote_withNoTargetFile_shouldThrowIllegalArgumentException() {
        Note note = new Note();
        note.setText("hello");

        assertThrows(IllegalArgumentException.class, () -> SnotesIO.saveNote(note, null));
    }

    @Test
    public void saveNote_withDirectoryAsTarget_shouldThrowIOException(@TempDir Path tempDir) {
        Note note = new Note();
        File directory = tempDir.toFile();
        note.setSourceFile(directory);
        note.setText("hello");

        assertThrows(IOException.class, () -> SnotesIO.saveNote(note, directory));
    }

    @Test
    public void computeFile_withNoteAndNullDataDir_shouldThrow() {
        assertThrows(IllegalArgumentException.class, () -> SnotesIO.computeFile(null, new Note()));
    }

    @Test
    public void computeFile_withQueryAndNullDataDir_shouldThrow() {
        assertThrows(IllegalArgumentException.class, () -> SnotesIO.computeFile(null, new Query()));
    }

    @Test
    public void computeFile_withTemplateAndNullDataDir_shouldThrow() {
        assertThrows(IllegalArgumentException.class, () -> SnotesIO.computeFile(null, new Template()));
    }

    @Test
    public void computeFile_withNullNote_shouldThrow() {
        assertThrows(IllegalArgumentException.class, () -> SnotesIO.computeFile(tempDir, (Note)null));
    }

    @Test
    public void computeFile_withNullQuery_shouldThrow() {
        assertThrows(IllegalArgumentException.class, () -> SnotesIO.computeFile(tempDir, (Query)null));
    }

    @Test
    public void computeFile_withNullTemplate_shouldThrow() {
        assertThrows(IllegalArgumentException.class, () -> SnotesIO.computeFile(tempDir, (Template)null));
    }

    @Test
    public void computeFile_withUntaggedUndatedNote_shouldReturnDefaultStaticFile() {
        // GIVEN an untagged, undated Note:
        Note note = new Note();

        // WHEN we compute a filename for it:
        File computed = SnotesIO.computeFile(tempDir, note);

        // THEN it should have the default filename:
        assertEquals("untagged_note.txt", computed.getName());

        // AND it should be in the "static" subdirectory of the data directory:
        File expectedDir = new File(tempDir, DataManager.STATIC_DIR);
        assertEquals(expectedDir.getAbsolutePath(), computed.getParentFile().getAbsolutePath());
    }

    @Test
    public void computeFile_withTaggedUndatedNote_shouldReturnFileWithTags() {
        // GIVEN a Note with some tags but no date:
        Note note = new Note();
        note.tag("work");
        note.tag("project");

        // WHEN we compute a filename for it:
        File computed = SnotesIO.computeFile(tempDir, note);

        // THEN the filename should include both tags:
        assertTrue(computed.getName().contains("work"));
        assertTrue(computed.getName().contains("project"));

        // AND the tags should have been sorted, with an underscore separator:
        assertEquals("project_work.txt", computed.getName());

        // AND it should be in the "static" subdirectory of the data directory:
        File expectedDir = new File(tempDir, DataManager.STATIC_DIR);
        assertEquals(expectedDir.getAbsolutePath(), computed.getParentFile().getAbsolutePath());
    }

    @Test
    public void computeFile_withUntaggedDatedNote_shouldReturnDefaultNameInDatedDir() {
        // GIVEN a Note with a date but no tags:
        Note note = new Note();
        note.setDate(new YMDDate("2020-01-01"));
        assertTrue(note.hasDate());

        // WHEN we compute a filename for it:
        File computed = SnotesIO.computeFile(tempDir, note);

        // THEN it should have the default filename:
        assertEquals("untagged_note.txt", computed.getName());

        // AND it should be in a dated subdirectory structure in the data directory:
        String expectedPath = tempDir.getAbsolutePath() + File.separator
            + note.getDate().getYearStr() + File.separator
            + note.getDate().getMonthStr() + File.separator
            + note.getDate().getDayStr() + File.separator + "untagged_note.txt";
        assertEquals(expectedPath, computed.getAbsolutePath());
    }

    @Test
    public void computeFile_withTaggedDatedNote_shouldReturnFileWithTagsInDatedDir() {
        // GIVEN a Note with a date and some tags:
        Note note = new Note();
        note.setDate(new YMDDate("2020-01-01"));
        note.tag("work");
        note.tag("project");

        // WHEN we compute a filename for it:
        File computed = SnotesIO.computeFile(tempDir, note);

        // THEN the filename should include both tags:
        assertTrue(computed.getName().contains("work"));
        assertTrue(computed.getName().contains("project"));

        // AND the tags should have been sorted, with an underscore separator:
        assertEquals("project_work.txt", computed.getName());

        // AND it should be in a dated subdirectory structure in the data directory:
        String expectedPath = tempDir.getAbsolutePath() + File.separator
            + note.getDate().getYearStr() + File.separator
            + note.getDate().getMonthStr() + File.separator
            + note.getDate().getDayStr() + File.separator + "project_work.txt";
        assertEquals(expectedPath, computed.getAbsolutePath());
    }

    @Test
    public void computeFile_withNamelessQuery_shouldUseDefaultName() {
        // GIVEN a Query with no name:
        Query query = new Query();
        query.setName(" "); // Query won't let us do this, and will silently set it to DEFAULT_NAME

        // WHEN we compute a file for it:
        File computed = SnotesIO.computeFile(tempDir, query);

        // THEN the filename should be based on the default query name:
        String expectedName = FileSystemUtil.sanitizeFilename(Query.DEFAULT_NAME + ".query");
        assertEquals(expectedName, computed.getName());
    }

    @Test
    public void computeFile_withNamedQuery_shouldNameProperly() {
        // GIVEN a Query with a name:
        Query query = new Query();
        query.setName("My Test Query");

        // WHEN we compute a file for it:
        File computed = SnotesIO.computeFile(tempDir, query);

        // THEN the filename should be based on the query name:
        String expectedName = FileSystemUtil.sanitizeFilename("My Test Query.query");
        assertEquals(expectedName, computed.getName());
    }

    @Test
    public void computeFile_withNamelessTemplate_shouldUseDefaultName() {
        // GIVEN a Template with no name:
        Template template = new Template();
        template.setName(" "); // Template won't let us do this, and will silently set it to DEFAULT_NAME

        // WHEN we compute a file for it:
        File computed = SnotesIO.computeFile(tempDir, template);

        // THEN the filename should be based on the default template name:
        String expectedName = FileSystemUtil.sanitizeFilename(Template.DEFAULT_NAME + ".template");
        assertEquals(expectedName, computed.getName());
    }

    @Test
    public void computeFile_withNamedTemplate_shouldNameProperly() {
        // GIVEN a Template with a name:
        Template template = new Template();
        template.setName("My Test Template");

        // WHEN we compute a file for it:
        File computed = SnotesIO.computeFile(tempDir, template);

        // THEN the filename should be based on the template name:
        String expectedName = FileSystemUtil.sanitizeFilename("My Test Template.template");
        assertEquals(expectedName, computed.getName());
    }
}