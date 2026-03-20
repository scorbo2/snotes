package ca.corbett.snotes.model;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DateTagTest {

    private static LocalDate today;

    @BeforeAll
    public static void setup() {
        // We need to capture today's date at the start of the test suite, so that we can
        // compare it against the YMDDate values that are created during the tests.
        // This might cause problems if the test starts immediately before midnight...
        today = LocalDate.now();
    }

    @Test
    public void constructor_withValidDateString_shouldSetDate() {
        DateTag dateTag = new DateTag("1997-04-21");
        assertEquals("1997-04-21", dateTag.getTag());
        assertEquals("#1997-04-21", dateTag.toString());
    }

    @Test
    public void constructor_withInvalidDateString_shouldDefaultToToday() {
        DateTag dateTag = new DateTag("invalid-date");
        assertEquals(today.toString(), dateTag.getTag());
        assertEquals("#" + today.toString(), dateTag.toString());
    }

    @Test
    public void compareTo_otherDateTag_shouldCompareByDate() {
        DateTag dateTag1 = new DateTag("1997-04-21");
        DateTag dateTag2 = new DateTag("2000-01-01");
        assertTrue(dateTag1.compareTo(dateTag2) < 0);
        assertTrue(dateTag2.compareTo(dateTag1) > 0);
    }

    @Test
    public void compareTo_nonDateTag_shouldFallbackToDefaultComparison() {
        DateTag dateTag = new DateTag("1997-04-21");
        Tag otherTag = new Tag("some-tag");
        assertTrue(dateTag.compareTo(otherTag) < 0);
        assertTrue(otherTag.compareTo(dateTag) > 0);
    }

    @Test
    public void copyConstructor_withNull_shouldDefaultToToday() {
        DateTag dateTag = new DateTag((DateTag)null);
        assertEquals(today.toString(), dateTag.getTag());
        assertEquals("#" + today.toString(), dateTag.toString());
    }
}