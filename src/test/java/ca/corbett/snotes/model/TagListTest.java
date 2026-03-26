package ca.corbett.snotes.model;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TagListTest {

    private static LocalDate today;

    @BeforeAll
    public static void setup() {
        // We need to capture today's date at the start of the test suite, so that we can
        // compare it against the YMDDate values that are created during the tests.
        // This might cause problems if the test starts immediately before midnight...
        today = LocalDate.now();
    }

    @Test
    public void noArgConstructor_shouldCreateUndatedList() {
        TagList tagList = new TagList();
        assertFalse(tagList.hasDate());
    }

    @Test
    public void constructor_withNull_shouldCreateDatedList() {
        // WHEN we pass null to the constructor:
        TagList tagList = new TagList(null);

        // THEN we should get a TagList that has a date tag:
        assertTrue(tagList.hasDate());

        // AND the date should match today's date:
        assertEquals(today.toString(), tagList.getDateTag().getDate().toString());
    }

    @Test
    public void setDate_withNull_shouldRemoveDate() {
        // GIVEN a TagList with a date:
        TagList tagList = new TagList(new YMDDate("1997-04-21"));
        assertTrue(tagList.hasDate());

        // WHEN we set the date to null:
        tagList.setDate((String)null);

        // THEN the TagList should no longer have a date:
        assertFalse(tagList.hasDate());
    }

    @Test
    public void addTag_withDateTag_shouldSetDate() {
        // GIVEN a TagList with no date:
        TagList tagList = new TagList();
        assertFalse(tagList.hasDate());

        // WHEN we add a tag that looks like a date:
        tagList.addTag("1997-04-21");

        // THEN the TagList should now have a date:
        assertTrue(tagList.hasDate());
    }

    @Test
    public void size_withVariousContents_shouldReturnCorrectSize() {
        // GIVEN an empty TagList:
        TagList tagList = new TagList();
        assertEquals(0, tagList.size());

        // WHEN we add a normal tag, THEN the size should be 1:
        tagList.addTag("some-tag");
        assertEquals(1, tagList.size());

        // WHEN we add a date tag, THEN the size should be 2 (combined size):
        tagList.addTag("1997-04-21");
        assertEquals(2, tagList.size());

        // WHEN we add another normal tag, THEN the size should be 3:
        tagList.addTag("another-tag");
        assertEquals(3, tagList.size());

        // WHEN we set date to null, THEN the size should decrease by 1:
        tagList.setDate((String)null);
        assertEquals(2, tagList.size());
    }

    @Test
    public void removeTag_withNonExistentTag_shouldHaveNoEffect() {
        // GIVEN a TagList with some tags:
        TagList tagList = new TagList();
        tagList.addTag("tag1");
        tagList.addTag("tag2");
        assertEquals(2, tagList.size());

        // WHEN we try to remove a tag that isn't in the list:
        tagList.removeTag("non-existent-tag");

        // THEN the size should remain unchanged:
        assertEquals(2, tagList.size());
    }

    @Test
    public void removeTag_withDateString_shouldRemoveDateTag() {
        // GIVEN a TagList with a date tag:
        TagList tagList = new TagList();
        tagList.addTag("1997-04-21");
        assertTrue(tagList.hasDate());

        // WHEN we remove the date tag using its string representation:
        tagList.removeTag("1997-04-21");

        // THEN the TagList should no longer have a date:
        assertFalse(tagList.hasDate());
    }

    @Test
    public void removeTag_withNormalTag_shouldRemoveThatTag() {
        // GIVEN a TagList with some normal tags:
        TagList tagList = new TagList();
        tagList.addTag("tag1");
        tagList.addTag("tag2");
        assertEquals(2, tagList.size());

        // WHEN we remove one of the normal tags:
        tagList.removeTag("tag1");

        // THEN the size should decrease by 1:
        assertEquals(1, tagList.size());
    }

    @Test
    public void hasTag_withValidValues_shouldReturnTrue() {
        // GIVEN a TagList with some tags:
        TagList tagList = new TagList();
        tagList.addTag("tag1");
        tagList.addTag("tag2");
        tagList.addTag("1997-04-21"); // This should be treated as a date tag, not a normal tag

        // THEN hasTag should return true for those tags:
        assertTrue(tagList.hasTag("tag1"));
        assertTrue(tagList.hasTag("tag2"));
        assertTrue(tagList.hasTag("1997-04-21")); // This should return true because it's a date tag
        assertTrue(tagList.hasTag(new DateTag("1997-04-21"))); // This should also return true
    }

    @Test
    public void hasTag_withNonExistentTag_shouldReturnFalse() {
        // GIVEN a TagList with some tags:
        TagList tagList = new TagList();
        tagList.addTag("tag1");
        tagList.addTag("tag2");

        // THEN hasTag should return false for a tag that isn't in the list:
        assertFalse(tagList.hasTag("non-existent-tag"));
    }

    @Test
    public void getNonDateTags_shouldExcludeDateTag() {
        // GIVEN a TagList with some normal tags and a date tag:
        TagList tagList = new TagList();
        tagList.addTag("tag1");
        tagList.addTag("tag2");
        tagList.addTag("1997-04-21"); // This should be treated as a date tag

        // THEN getNonDateTags should return only the normal tags, not the date tag:
        assertTrue(tagList.getNonDateTags().contains(new Tag("tag1")));
        assertTrue(tagList.getNonDateTags().contains(new Tag("tag2")));
        assertFalse(tagList.getNonDateTags().contains(new DateTag("1997-04-21")));
    }

    @Test
    public void getTags_withValidList_shouldReturnAllTagsWithDateFirst() {
        // GIVEN a TagList with some normal tags and a date tag:
        TagList tagList = new TagList();
        tagList.addTag("tag1");
        tagList.addTag("tag2");
        tagList.addTag("1997-04-21"); // This should be treated as a date tag

        // THEN getTags should return all tags, with the date tag first:
        List<Tag> actualTags = tagList.getTags();
        assertEquals(3, actualTags.size());
        assertInstanceOf(DateTag.class, actualTags.get(0)); // date should be first!
        assertEquals("1997-04-21", tagList.getTags().get(0).getTag());
        assertEquals(1, actualTags.indexOf(new Tag("tag1")));
        assertEquals(2, actualTags.indexOf(new Tag("tag2")));
    }

    @Test
    public void fromRawString_withNullOrBlank_shouldCreateEmptyTagList() {
        // WHEN we pass null to fromRawString:
        TagList tagList1 = TagList.fromRawString(null);

        // THEN we should get an empty TagList:
        assertTrue(tagList1.getTags().isEmpty());

        // WHEN we pass a blank string to fromRawString:
        TagList tagList2 = TagList.fromRawString("   ");

        // THEN we should also get an empty TagList:
        assertTrue(tagList2.getTags().isEmpty());
    }

    @Test
    public void fromRawString_withSingleTag_shouldCreateTagListWithThatTag() {
        // WHEN we pass a single tag string to fromRawString:
        TagList tagList = TagList.fromRawString("some-tag");

        // THEN we should get a TagList with that tag:
        assertEquals(1, tagList.size());
        assertTrue(tagList.hasTag("some-tag"));
    }

    @Test
    public void fromRawString_withMultipleTags_shouldCreateTagListWithThoseTags() {
        // WHEN we pass a comma-separated string of tags to fromRawString:
        TagList tagList = TagList.fromRawString("tag1, tag2, tag3");

        // THEN we should get a TagList with those tags:
        assertEquals(3, tagList.size());
        assertTrue(tagList.hasTag("tag1"));
        assertTrue(tagList.hasTag("tag2"));
        assertTrue(tagList.hasTag("tag3"));
    }

    @Test
    public void fromRawString_withMultipleTagsAndMixedSeparators_shouldCreateTagListWithThoseTags() {
        // WHEN we pass a string of tags with mixed separators to fromRawString:
        TagList tagList = TagList.fromRawString("tag1, tag2, tag3  tag4");

        // THEN we should get a TagList with those tags:
        assertEquals(4, tagList.size());
        assertTrue(tagList.hasTag("tag1"));
        assertTrue(tagList.hasTag("tag2"));
        assertTrue(tagList.hasTag("tag3"));
        assertTrue(tagList.hasTag("tag4"));
    }
}