package ca.corbett.snotes.model.filter;

import ca.corbett.snotes.model.Note;
import ca.corbett.snotes.model.Tag;
import ca.corbett.snotes.model.YMDDate;
import org.junit.jupiter.api.BeforeAll;

import java.util.ArrayList;
import java.util.List;

/**
 * The abstract base test class for all Filter tests.
 */
public abstract class FilterTest {

    // ---- Test contents ----
    protected static final String TEXT_TO_FIND = "SomeUniqueStringThatWillOnlyAppearInOneNote";
    protected static final YMDDate SPECIAL_DATE = new YMDDate("1997-04-21");
    protected static final YMDDate JAN_1_2020 = new YMDDate("2020-01-01");
    protected static final YMDDate FEB_15_2020 = new YMDDate("2020-02-15");
    protected static final YMDDate VERY_OLD_DATE = new YMDDate("1900-01-01");
    protected static final YMDDate VERY_FUTURE_DATE = new YMDDate("2100-01-01");
    protected static final Tag NON_DATE_TAG1 = new Tag("work1");
    protected static final Tag NON_DATE_TAG2 = new Tag("work2");

    // ---- Test Note instances ----
    protected static final Note NOTE_WITH_TEXT_TO_FIND = new Note().setText("A note containing the unique text: "
                                                                                + TEXT_TO_FIND
                                                                                + " blah blah");
    protected static final Note NOTE_UNDATED_UNTAGGED = new Note().setText("An undated and untagged note.");
    protected static final Note NOTE_NO_TEXT = new Note().tag(NON_DATE_TAG1);
    protected static final Note NOTE_DATED_UNTAGGED = new Note().setText("A note with a date tag but no non-date tags.")
                                                                .setDate(SPECIAL_DATE);
    protected static final Note NOTE_DATED_TAGGED = new Note().setText("A note with a date tag and a non-date tag.")
                                                              .setDate(SPECIAL_DATE)
                                                              .tag(NON_DATE_TAG1);
    protected static final Note NOTE_VERY_OLD = new Note().setText("A note with a date that is very old.")
                                                          .setDate(VERY_OLD_DATE);
    protected static final Note NOTE_VERY_FUTURE = new Note().setText("A note with a date that is very future.")
                                                             .setDate(VERY_FUTURE_DATE);
    protected static final Note NOTE_JAN_1_2020 = new Note().setText("Random note from Jan 2020").setDate(JAN_1_2020);
    protected static final Note NOTE_FEB_15_2020 = new Note().setText("Random note from Feb 2020").setDate(FEB_15_2020);
    protected static final Note NOTE_MULTIPLE_TAGS = new Note().setText("With multiple tags").tag(NON_DATE_TAG1)
                                                               .tag(NON_DATE_TAG2);
    protected static final List<Note> unfilteredList = new ArrayList<>();

    @BeforeAll
    public static void setup() {
        // Create an unfiltered list of test Notes that our
        // subclass tests can use. Modify this list with caution!
        // It will require updating test expectations in some or all subclasses.
        // The order doesn't matter, but adding or removing Notes will likely cause some tests to fail.
        unfilteredList.clear();
        unfilteredList.add(NOTE_UNDATED_UNTAGGED);
        unfilteredList.add(NOTE_DATED_UNTAGGED);
        unfilteredList.add(NOTE_DATED_TAGGED);
        unfilteredList.add(NOTE_NO_TEXT);
        unfilteredList.add(NOTE_VERY_OLD);
        unfilteredList.add(NOTE_VERY_FUTURE);
        unfilteredList.add(NOTE_JAN_1_2020);
        unfilteredList.add(NOTE_WITH_TEXT_TO_FIND);
        unfilteredList.add(NOTE_FEB_15_2020);
        unfilteredList.add(NOTE_MULTIPLE_TAGS);
    }
}