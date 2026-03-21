package ca.corbett.snotes.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class TemplateTest {

    @TempDir
    File tempDir;

    // -----------------------------------------------------------------------
    // Constructor tests
    // -----------------------------------------------------------------------

    @Test
    public void constructor_withNoArgs_shouldCreateDefaultTemplate() {
        // WHEN we construct a Template with no arguments:
        Template template = new Template();

        // THEN it should have the default name, NONE date option, NONE context, and no tags:
        assertNotNull(template.getName());
        assertEquals(Template.DEFAULT_NAME, template.getName());
        assertEquals(Template.DateOption.NONE, template.getDateOption());
        assertEquals(Template.Context.NONE, template.getContext());
        assertNotNull(template.getTagList());
        assertTrue(template.getTagList().isEmpty());
    }

    @Test
    public void constructor_withNullName_shouldThrowIllegalArgumentException() {
        // WHEN we construct a Template with a null name:
        // THEN it should throw an IllegalArgumentException:
        assertThrows(IllegalArgumentException.class, () -> new Template(null));
    }

    @Test
    public void constructor_withBlankName_shouldSetDefaultName() {
        // WHEN we construct a Template with a blank name:
        Template template = new Template("   ");

        // THEN the name should be set to DEFAULT_NAME:
        assertEquals(Template.DEFAULT_NAME, template.getName());
    }

    @Test
    public void constructor_withNameTooLong_shouldTruncateName() {
        // GIVEN a name that exceeds the limit:
        String longName = "This is a very long template name that exceeds the limit";

        // WHEN we construct a Template with it:
        Template template = new Template(longName);

        // THEN the name should be truncated to NAME_LENGTH_LIMIT:
        assertEquals(Template.NAME_LENGTH_LIMIT, template.getName().length());
        assertEquals(longName.substring(0, Template.NAME_LENGTH_LIMIT), template.getName());
    }

    @Test
    public void constructor_withValidName_shouldSetName() {
        // WHEN we construct a Template with a valid name:
        Template template = new Template("My Template");

        // THEN the name should be set correctly:
        assertEquals("My Template", template.getName());
    }

    // -----------------------------------------------------------------------
    // setName tests
    // -----------------------------------------------------------------------

    @Test
    public void setName_withNull_shouldThrowIllegalArgumentException() {
        // GIVEN a Template:
        Template template = new Template();

        // WHEN we try to set the name to null:
        // THEN it should throw an IllegalArgumentException:
        assertThrows(IllegalArgumentException.class, () -> template.setName(null));
    }

    @Test
    public void setName_withBlank_shouldSetDefaultName() {
        // GIVEN a Template with a custom name:
        Template template = new Template("My Template");

        // WHEN we set the name to a blank string:
        template.setName("   ");

        // THEN the name should be reset to DEFAULT_NAME:
        assertEquals(Template.DEFAULT_NAME, template.getName());
    }

    @Test
    public void setName_withNameTooLong_shouldTruncateName() {
        // GIVEN a Template:
        Template template = new Template();

        // WHEN we set the name to a string longer than the limit:
        String longName = "This is a very long template name that exceeds the limit";
        template.setName(longName);

        // THEN the name should be truncated to NAME_LENGTH_LIMIT:
        assertEquals(Template.NAME_LENGTH_LIMIT, template.getName().length());
        assertEquals(longName.substring(0, Template.NAME_LENGTH_LIMIT), template.getName());
    }

    @Test
    public void setName_withValidName_shouldSet() {
        // GIVEN a Template:
        Template template = new Template();

        // WHEN we set the name to a valid string within the limit:
        String validName = "My Valid Template";
        template.setName(validName);

        // THEN the name should be set correctly:
        assertEquals(validName, template.getName());
    }

    // -----------------------------------------------------------------------
    // setDateOption tests
    // -----------------------------------------------------------------------

    @Test
    public void setDateOption_withNull_shouldThrowIllegalArgumentException() {
        // GIVEN a Template:
        Template template = new Template();

        // WHEN we try to set the date option to null:
        // THEN it should throw an IllegalArgumentException:
        assertThrows(IllegalArgumentException.class, () -> template.setDateOption(null));
    }

    @Test
    public void setDateOption_withValidOption_shouldSet() {
        // GIVEN a Template:
        Template template = new Template();

        // WHEN we set the date option to each valid value:
        for (Template.DateOption option : Template.DateOption.values()) {
            template.setDateOption(option);

            // THEN the date option should be set correctly:
            assertEquals(option, template.getDateOption());
        }
    }

    // -----------------------------------------------------------------------
    // setContext tests
    // -----------------------------------------------------------------------

    @Test
    public void setContext_withNull_shouldThrowIllegalArgumentException() {
        // GIVEN a Template:
        Template template = new Template();

        // WHEN we try to set the context to null:
        // THEN it should throw an IllegalArgumentException:
        assertThrows(IllegalArgumentException.class, () -> template.setContext(null));
    }

    @Test
    public void setContext_withValidContext_shouldSet() {
        // GIVEN a Template:
        Template template = new Template();

        // WHEN we set the context to each valid value:
        for (Template.Context ctx : Template.Context.values()) {
            template.setContext(ctx);

            // THEN the context should be set correctly:
            assertEquals(ctx, template.getContext());
        }
    }

    // -----------------------------------------------------------------------
    // addTag tests
    // -----------------------------------------------------------------------

    @Test
    public void addTag_withNull_shouldThrowIllegalArgumentException() {
        // GIVEN a Template:
        Template template = new Template();

        // WHEN we try to add a null tag:
        // THEN it should throw an IllegalArgumentException:
        assertThrows(IllegalArgumentException.class, () -> template.addTag(null));
    }

    @Test
    public void addTag_withBlank_shouldThrowIllegalArgumentException() {
        // GIVEN a Template:
        Template template = new Template();

        // WHEN we try to add a blank tag:
        // THEN it should throw an IllegalArgumentException:
        assertThrows(IllegalArgumentException.class, () -> template.addTag("   "));
    }

    @Test
    public void addTag_withValidTag_shouldAdd() {
        // GIVEN a Template:
        Template template = new Template();
        assertTrue(template.getTagList().isEmpty());

        // WHEN we add a valid tag:
        template.addTag("work");

        // THEN the tag list should contain it:
        assertEquals(1, template.getTagList().size());
        assertEquals("work", template.getTagList().get(0).getTag());
    }

    @Test
    public void addTag_tagShouldBeNormalized() {
        // GIVEN a Template:
        Template template = new Template();

        // WHEN we add a tag with mixed case, spaces, and slashes:
        template.addTag("My/Tag Value");

        // THEN the tag should be normalized (lowercased, spaces/slashes replaced):
        assertEquals(1, template.getTagList().size());
        assertEquals("my_tag_value", template.getTagList().get(0).getTag());
    }

    // -----------------------------------------------------------------------
    // clearTags tests
    // -----------------------------------------------------------------------

    @Test
    public void clearTags_shouldRemoveAllTags() {
        // GIVEN a Template with multiple tags:
        Template template = new Template();
        template.addTag("tag1");
        template.addTag("tag2");
        template.addTag("tag3");
        assertEquals(3, template.getTagList().size());

        // WHEN we call clearTags():
        template.clearTags();

        // THEN the tag list should be empty:
        assertTrue(template.getTagList().isEmpty());
    }

    // -----------------------------------------------------------------------
    // getTagList tests
    // -----------------------------------------------------------------------

    @Test
    public void getTagList_shouldReturnCopy() {
        // GIVEN a Template with a tag:
        Template template = new Template();
        template.addTag("original-tag");

        // WHEN we get the tag list and modify the returned list:
        List<Tag> returnedList = template.getTagList();
        returnedList.clear();

        // THEN the template's internal tag list should be unaffected:
        assertEquals(1, template.getTagList().size());
        assertEquals("original-tag", template.getTagList().get(0).getTag());
    }

    // -----------------------------------------------------------------------
    // save tests
    // -----------------------------------------------------------------------

    @Test
    public void save_withNullFile_shouldThrowIllegalArgumentException() {
        // GIVEN a Template:
        Template template = new Template();

        // WHEN we try to save to a null file:
        // THEN it should throw an IllegalArgumentException:
        assertThrows(IllegalArgumentException.class, () -> template.save(null));
    }

    @Test
    public void save_withDirectory_shouldThrowIOException() {
        // GIVEN a Template:
        Template template = new Template();

        // WHEN we try to save to a directory:
        // THEN it should throw an IOException:
        assertThrows(IOException.class, () -> template.save(tempDir));
    }

    // -----------------------------------------------------------------------
    // load tests
    // -----------------------------------------------------------------------

    @Test
    public void load_withNullFile_shouldThrowIllegalArgumentException() {
        // WHEN we try to load from a null file:
        // THEN it should throw an IllegalArgumentException:
        assertThrows(IllegalArgumentException.class, () -> Template.load(null));
    }

    @Test
    public void load_withNonExistentFile_shouldThrowIOException() {
        // GIVEN a file that does not exist:
        File nonExistent = new File(tempDir, "does-not-exist.template");

        // WHEN we try to load from it:
        // THEN it should throw an IOException:
        assertThrows(IOException.class, () -> Template.load(nonExistent));
    }

    // -----------------------------------------------------------------------
    // load with malformed or edge-case content tests
    // -----------------------------------------------------------------------

    @Test
    public void load_withEmptyFile_shouldThrowIOException() throws IOException {
        // GIVEN a file that is completely empty (Jackson readTree returns null for empty input):
        File emptyFile = File.createTempFile("empty", ".template", tempDir);

        // WHEN we try to load from it:
        // THEN it should throw an IOException:
        assertThrows(IOException.class, () -> Template.load(emptyFile));
    }

    @Test
    public void load_withMalformedJson_shouldThrowIOException() throws IOException {
        // GIVEN a file containing invalid JSON syntax:
        File malformedFile = File.createTempFile("malformed", ".template", tempDir);
        Files.writeString(malformedFile.toPath(), "{this is not : valid [ json !!!");

        // WHEN we try to load from it:
        // THEN Jackson throws JsonParseException (an IOException subclass):
        assertThrows(IOException.class, () -> Template.load(malformedFile));
    }

    @Test
    public void load_withMissingNameField_shouldUseDefaultName() throws IOException {
        // GIVEN a valid JSON file that has no "name" field at all:
        File file = File.createTempFile("no-name", ".template", tempDir);
        Files.writeString(file.toPath(),
                          "{\"dateOption\":\"NONE\",\"context\":\"NONE\",\"tags\":[]}");

        // WHEN we load it:
        Template loaded = Template.load(file);

        // THEN the name should fall back to DEFAULT_NAME:
        assertEquals(Template.DEFAULT_NAME, loaded.getName());
    }

    @Test
    public void load_withBlankName_shouldUseDefaultName() throws IOException {
        // GIVEN a valid JSON file where the "name" value is blank whitespace:
        File file = File.createTempFile("blank-name", ".template", tempDir);
        Files.writeString(file.toPath(),
                          "{\"name\":\"   \",\"dateOption\":\"NONE\",\"context\":\"NONE\",\"tags\":[]}");

        // WHEN we load it:
        Template loaded = Template.load(file);

        // THEN the name should fall back to DEFAULT_NAME:
        assertEquals(Template.DEFAULT_NAME, loaded.getName());
    }

    @Test
    public void load_withNullNameNode_shouldUseDefaultName() throws IOException {
        // GIVEN a valid JSON file where "name" is explicitly JSON null:
        File file = File.createTempFile("null-name", ".template", tempDir);
        Files.writeString(file.toPath(),
                          "{\"name\":null,\"dateOption\":\"NONE\",\"context\":\"NONE\",\"tags\":[]}");

        // WHEN we load it:
        Template loaded = Template.load(file);

        // THEN the name should fall back to DEFAULT_NAME:
        assertEquals(Template.DEFAULT_NAME, loaded.getName());
    }

    @Test
    public void load_withInvalidDateOption_shouldUseNoneDateOption() throws IOException {
        // GIVEN a JSON file with an unrecognized "dateOption" string:
        File file = File.createTempFile("bad-date", ".template", tempDir);
        Files.writeString(file.toPath(),
                          "{\"name\":\"Test\",\"dateOption\":\"INVALID_OPTION\",\"context\":\"NONE\",\"tags\":[]}");

        // WHEN we load it:
        Template loaded = Template.load(file);

        // THEN the dateOption should fall back to NONE:
        assertEquals(Template.DateOption.NONE, loaded.getDateOption());
    }

    @Test
    public void load_withMissingDateOptionField_shouldUseNoneDateOption() throws IOException {
        // GIVEN a JSON file with no "dateOption" field:
        File file = File.createTempFile("no-date", ".template", tempDir);
        Files.writeString(file.toPath(),
                          "{\"name\":\"Test\",\"context\":\"NONE\",\"tags\":[]}");

        // WHEN we load it:
        Template loaded = Template.load(file);

        // THEN the dateOption should fall back to NONE:
        assertEquals(Template.DateOption.NONE, loaded.getDateOption());
    }

    @Test
    public void load_withNullDateOptionNode_shouldUseNoneDateOption() throws IOException {
        // GIVEN a JSON file where "dateOption" is explicitly JSON null:
        File file = File.createTempFile("null-date", ".template", tempDir);
        Files.writeString(file.toPath(),
                          "{\"name\":\"Test\",\"dateOption\":null,\"context\":\"NONE\",\"tags\":[]}");

        // WHEN we load it:
        Template loaded = Template.load(file);

        // THEN the dateOption should fall back to NONE:
        assertEquals(Template.DateOption.NONE, loaded.getDateOption());
    }

    @Test
    public void load_withInvalidContext_shouldUseNoneContext() throws IOException {
        // GIVEN a JSON file with an unrecognized "context" string:
        File file = File.createTempFile("bad-ctx", ".template", tempDir);
        Files.writeString(file.toPath(),
                          "{\"name\":\"Test\",\"dateOption\":\"NONE\",\"context\":\"INVALID_CONTEXT\",\"tags\":[]}");

        // WHEN we load it:
        Template loaded = Template.load(file);

        // THEN the context should fall back to NONE:
        assertEquals(Template.Context.NONE, loaded.getContext());
    }

    @Test
    public void load_withMissingContextField_shouldUseNoneContext() throws IOException {
        // GIVEN a JSON file with no "context" field:
        File file = File.createTempFile("no-ctx", ".template", tempDir);
        Files.writeString(file.toPath(),
                          "{\"name\":\"Test\",\"dateOption\":\"NONE\",\"tags\":[]}");

        // WHEN we load it:
        Template loaded = Template.load(file);

        // THEN the context should fall back to NONE:
        assertEquals(Template.Context.NONE, loaded.getContext());
    }

    @Test
    public void load_withNullContextNode_shouldUseNoneContext() throws IOException {
        // GIVEN a JSON file where "context" is explicitly JSON null:
        File file = File.createTempFile("null-ctx", ".template", tempDir);
        Files.writeString(file.toPath(),
                          "{\"name\":\"Test\",\"dateOption\":\"NONE\",\"context\":null,\"tags\":[]}");

        // WHEN we load it:
        Template loaded = Template.load(file);

        // THEN the context should fall back to NONE:
        assertEquals(Template.Context.NONE, loaded.getContext());
    }

    @Test
    public void load_withNonTextualTagValues_shouldSkipInvalidTags() throws IOException {
        // GIVEN a JSON file where the tags array contains a mix of valid strings and non-string values:
        File file = File.createTempFile("bad-tags", ".template", tempDir);
        Files.writeString(file.toPath(),
                          "{\"name\":\"Test\",\"dateOption\":\"NONE\",\"context\":\"NONE\"," +
                          "\"tags\":[\"valid-tag\",42,null,true]}");

        // WHEN we load it:
        Template loaded = Template.load(file);

        // THEN only the valid textual tag should be present; non-strings are silently skipped:
        assertEquals(1, loaded.getTagList().size());
        assertEquals("valid-tag", loaded.getTagList().get(0).getTag());
    }

    @Test
    public void load_withTagsNotArray_shouldHaveNoTags() throws IOException {
        // GIVEN a JSON file where "tags" is a plain string rather than an array:
        File file = File.createTempFile("tags-not-array", ".template", tempDir);
        Files.writeString(file.toPath(),
                          "{\"name\":\"Test\",\"dateOption\":\"NONE\",\"context\":\"NONE\"," +
                          "\"tags\":\"not-an-array\"}");

        // WHEN we load it:
        Template loaded = Template.load(file);

        // THEN the tag list should be empty (non-array tags field is silently ignored):
        assertTrue(loaded.getTagList().isEmpty());
    }

    @Test
    public void load_withMissingTagsField_shouldHaveNoTags() throws IOException {
        // GIVEN a JSON file with no "tags" field at all:
        File file = File.createTempFile("no-tags", ".template", tempDir);
        Files.writeString(file.toPath(),
                          "{\"name\":\"Test\",\"dateOption\":\"NONE\",\"context\":\"NONE\"}");

        // WHEN we load it:
        Template loaded = Template.load(file);

        // THEN the tag list should be empty:
        assertTrue(loaded.getTagList().isEmpty());
    }

    // -----------------------------------------------------------------------
    // Round-trip test
    // -----------------------------------------------------------------------

    @Test
    public void save_roundTrip_shouldLoad() {
        // GIVEN a fully populated Template:
        Template template = new Template("My Test Template");
        template.setDateOption(Template.DateOption.TODAY);
        template.setContext(Template.Context.MOST_RECENT3);
        template.addTag("work");
        template.addTag("project-alpha");

        try {
            // WHEN we save it to a file and then load it back:
            File savedFile = File.createTempFile("test", ".template", tempDir);
            template.save(savedFile);
            assertNotNull(savedFile);
            assertTrue(savedFile.exists());
            Template loaded = Template.load(savedFile);

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
    public void save_roundTrip_withNoTagsAndDefaultValues_shouldLoad() {
        // GIVEN a minimal Template with default values and no tags:
        Template template = new Template();

        try {
            // WHEN we save it to a file and then load it back:
            File savedFile = File.createTempFile("test-minimal", ".template", tempDir);
            template.save(savedFile);
            Template loaded = Template.load(savedFile);

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
    public void save_roundTrip_withAllDateOptions_shouldLoad() {
        // GIVEN a Template saved with each possible DateOption:
        for (Template.DateOption option : Template.DateOption.values()) {
            Template template = new Template("DateOption Test");
            template.setDateOption(option);

            try {
                File savedFile = File.createTempFile("test-date-" + option.name(), ".template", tempDir);
                template.save(savedFile);
                Template loaded = Template.load(savedFile);

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
    public void save_roundTrip_withAllContextOptions_shouldLoad() {
        // GIVEN a Template saved with each possible Context option:
        for (Template.Context ctx : Template.Context.values()) {
            Template template = new Template("Context Test");
            template.setContext(ctx);

            try {
                File savedFile = File.createTempFile("test-ctx-" + ctx.name(), ".template", tempDir);
                template.save(savedFile);
                Template loaded = Template.load(savedFile);

                // THEN the loaded Template should have the same Context:
                assertEquals(ctx, loaded.getContext(),
                             "Context mismatch for: " + ctx);
            }
            catch (IOException ioe) {
                fail("IOException thrown for Context " + ctx + ": " + ioe.getMessage());
            }
        }
    }
}

