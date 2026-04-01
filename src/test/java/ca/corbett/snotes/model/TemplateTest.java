package ca.corbett.snotes.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    public void constructor_shouldDefaultOrderToZero() {
        // WHEN we construct a Template:
        Template template = new Template();

        // THEN the order should default to zero:
        assertEquals(0, template.getOrder());
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
}

