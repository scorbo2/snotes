package ca.corbett.snotes.model;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YMDDateTest {

    private static LocalDate testDate;
    private static LocalDate today;

    @BeforeAll
    public static void setup() {
        // We need to capture today's date at the start of the test suite, so that we can
        // compare it against the YMDDate values that are created during the tests.
        // This might cause problems if the test starts immediately before midnight...
        today = LocalDate.now();

        // We'll pick an arbitrary test date of Monday, April 21, 1997, for testing:
        testDate = LocalDate.of(1997, 4, 21);
    }

    @Test
    public void noArgsConstructor_shouldSetCurrentDate() {
        // GIVEN a YMDDate with no arguments:
        YMDDate date = new YMDDate();

        // THEN the date should be set to the current date:
        assertEquals(today.toString(), date.toString());
    }

    @Test
    public void constructor_withInvalidDate_shouldDefaultToTodayAndLogWarning() {
        TestLogHandler handler = new TestLogHandler();
        Logger logger = Logger.getLogger(YMDDate.class.getName());
        logger.addHandler(handler);

        try {
            // GIVEN a YMDDate with an invalid date string:
            YMDDate date = new YMDDate("invalid-date");

            // THEN the date should default to today:
            assertEquals(today.toString(), date.toString());

            // AND a warning should have been logged with "invalid date":
            assertTrue(handler.hasWarningContaining("invalid date"));
        }
        finally {
            logger.removeHandler(handler);
        }
    }

    @Test
    public void isValidYMD_withValidValues_shouldReturnTrue() {
        // GIVEN some valid YMD date strings:
        String[] validDates = {"2024-01-01", "2024-12-31", "2024-02-29"};

        // THEN isValidYMD should return true for all of them:
        for (String dateStr : validDates) {
            assertTrue(YMDDate.isValidYMD(dateStr), "Expected '" + dateStr + "' to be valid");
        }
    }

    @Test
    public void isValidYMD_withInvalidValues_shouldReturnFalse() {
        // GIVEN some invalid YMD date strings:
        String[] invalidDates = {"2024-13-01", "2024-00-10", "2024-02-30", "not-a-date", "", null};

        // THEN isValidYMD should return false for all of them:
        for (String dateStr : invalidDates) {
            assertFalse(YMDDate.isValidYMD(dateStr), "Expected '" + dateStr + "' to be invalid");
        }
    }

    @Test
    public void getDayName_withValidDate_shouldReturnCorrectDayName() {
        // GIVEN a YMDDate representing April 21, 1997:
        Locale previousLocale = Locale.getDefault();
        try {
            Locale.setDefault(Locale.ENGLISH);

            YMDDate date = new YMDDate(testDate);

            // THEN getDayName should return "Monday" in the English locale:
            assertEquals("Monday", date.getDayName());
        } finally {
            // Restore the original default locale so other tests are not affected:
            Locale.setDefault(previousLocale);
        }
    }

    @Test
    public void getTomorrow_withValidDate_shouldReturnNextDay() {
        // GIVEN a YMDDate representing April 21, 1997:
        YMDDate date = new YMDDate(testDate);

        // THEN getTomorrow should return April 22, 1997:
        assertEquals(LocalDate.of(1997, 4, 22), date.getTomorrow().date);
    }

    @Test
    public void getYesterday_withValidDate_shouldReturnPreviousDay() {
        // GIVEN a YMDDate representing April 21, 1997:
        YMDDate date = new YMDDate(testDate);

        // THEN getYesterday should return April 20, 1997:
        assertEquals(LocalDate.of(1997, 4, 20), date.getYesterday().date);
    }

    @Test
    public void getYearStr_withValidDate_shouldReturnFourDigitYear() {
        // GIVEN a YMDDate representing April 21, 1997:
        YMDDate date = new YMDDate(testDate);

        // THEN getYearStr should return "1997":
        assertEquals("1997", date.getYearStr());
    }

    @Test
    public void getMonthStr_withValidDate_shouldReturnTwoDigitMonth() {
        // GIVEN a YMDDate representing April 21, 1997:
        YMDDate date = new YMDDate(testDate);

        // THEN getMonthStr should return "04" and NOT "4"!:
        assertEquals("04", date.getMonthStr());
    }

    @Test
    public void getDayStr_withValidDate_shouldReturnTwoDigitDay() {
        // GIVEN a YMDDate representing April 21, 1997:
        YMDDate date = new YMDDate(testDate);

        // THEN getDayStr should return "21":
        assertEquals("21", date.getDayStr());
    }

    /**
     * This test class can be used to intercept log warnings.
     */
    static class TestLogHandler extends Handler {
        private final List<LogRecord> records = new ArrayList<>();

        @Override
        public void publish(LogRecord record) {
            records.add(record);
        }

        public boolean hasWarningContaining(String message) {
            return records.stream()
                          .anyMatch(r -> r.getLevel() == Level.WARNING &&
                                  r.getMessage().toLowerCase().contains(message.toLowerCase()));
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() throws SecurityException {
        }
    }
}