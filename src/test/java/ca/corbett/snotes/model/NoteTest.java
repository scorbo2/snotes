package ca.corbett.snotes.model;

import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

class NoteTest {

    @Test
    public void constructor_shouldCreateUntaggedNote() {
        // WHEN we construct a Note and give it no tags at all:
        Note note = new Note();

        // THEN the Note should have no tags, no date, no text, no source file, and should be dirty:
        assertFalse(note.hasDate());
        assertEquals(0, note.getNonDateTags().size());
        assertNull(note.getSourceFile());
        assertTrue(note.isDirty());
    }

    @Test
    public void setDate_withValidDate_shouldSetDateTagAndDirty() {
        // GIVEN a Note with no date tag:
        Note note = new Note();

        // WHEN we set a valid date on the Note:
        note.setDate(new YMDDate("1997-04-21"));

        // THEN the Note should have a date tag with the correct value, and should be dirty:
        assertTrue(note.hasDate());
        assertNotNull(note.getDate());
        assertEquals("1997-04-21", note.getDate().toString());
        assertTrue(note.isDirty());
    }

    @Test
    public void setText_withText_shouldSetTextAndDirty() {
        // GIVEN a Note with no text:
        Note note = new Note();

        // WHEN we set some text on the Note:
        note.setText("This is some sample text.");

        // THEN the Note should have the correct text, and should be dirty:
        assertTrue(note.hasText());
        assertNotNull(note.getText());
        assertEquals("This is some sample text.", note.getText());
        assertTrue(note.isDirty());
    }

    @Test
    public void getHumanTagLine_withDateAndTags_shouldReturnCorrectFormat() {
        Locale originalLocale = Locale.getDefault();
        try {
            Locale.setDefault(Locale.ENGLISH);
            // GIVEN a Note with a date tag and some non-date tags:
            Note note = new Note();
            note.setDate(new YMDDate("1997-04-21"));
            note.tag("tag1");
            note.tag("tag2");

            // WHEN we get the human-readable tag line:
            String tagLine = note.getHumanTagLine();

            // THEN it should be in the correct format:
            // (this test will only work if the locale is English...)
            assertEquals("#1997-04-21 (Monday) #tag1 #tag2" + System.lineSeparator(), tagLine);
        }
        finally {
            Locale.setDefault(originalLocale);
        }
    }

    @Test
    public void getPersistenceTagLine_withDateAndTags_shouldReturnCorrectFormat() {
        // GIVEN a Note with a date tag and some non-date tags:
        Note note = new Note();
        note.setDate(new YMDDate("1997-04-21"));
        note.tag("tag1");
        note.tag("tag2");

        // WHEN we get the persistence tag line:
        String tagLine = note.getPersistenceTagLine();

        // THEN it should be in the correct format:
        assertEquals("#1997-04-21 #tag1 #tag2", tagLine);
    }
}