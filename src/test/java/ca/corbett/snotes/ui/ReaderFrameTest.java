package ca.corbett.snotes.ui;

import ca.corbett.snotes.model.Note;
import ca.corbett.snotes.model.Query;
import ca.corbett.snotes.model.Tag;
import ca.corbett.snotes.model.filter.TagFilter;
import ca.corbett.snotes.model.filter.TextFilter;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.InternalFrameListener;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for ReaderFrame's automatic text-highlighting behavior: when the frame
 * is created from a Query that contains a TextFilter, a text search for the
 * filtered text is run automatically the first time the frame is activated,
 * so that all occurrences are highlighted.
 * <p>
 * Most tests drive {@link ReaderFrame#performAutoHighlight()} directly, which
 * is the method the frame's activation listener calls. The listener wiring
 * itself is covered by frameActivation_shouldTriggerAutoHighlight, which
 * dispatches a synthetic INTERNAL_FRAME_ACTIVATED event to the listener(s)
 * the constructor registered. In headless tests JInternalFrame.setSelected()
 * is a no-op unless the frame is actually showing, so the event can't be
 * triggered the natural way here - but getInternalFrameListeners() is public,
 * so we can reach the wiring without a window.
 * </p>
 */
class ReaderFrameTest {

    @Test
    void performAutoHighlight_queryWithTextFilter_shouldHighlightAllOccurrencesAndShowFindPanel() {
        // GIVEN a ReaderFrame created from a query that filters for "fox",
        // with notes containing "fox" three times in total:
        Query query = new Query();
        query.addFilter(new TextFilter("fox"));
        ReaderFrame frame = new ReaderFrame(notesWithFoxes(), query);

        // WHEN the frame's auto-highlight runs, as it does on first activation:
        frame.performAutoHighlight();

        // THEN all three occurrences are highlighted...:
        assertEquals(3, highlightCount(frame));

        // ...and the find panel is visible with case sensitivity off, so the
        // user can adjust the search or press ESC to clear the highlights:
        TextFindPanel findPanel = frame.getNoteViewer().getTextFindPanel();
        assertTrue(findPanel.isVisible());
        assertFalse(findPanel.isCaseSensitive());
    }

    @Test
    void performAutoHighlight_withCaseSensitiveTextFilter_shouldOnlyHighlightExactCaseMatches() {
        // GIVEN a ReaderFrame created from a query with a case-sensitive text filter:
        Query query = new Query();
        query.addFilter(new TextFilter("apple", true));
        ReaderFrame frame = new ReaderFrame(List.of(note("Apple apple APPLE")), query);

        // WHEN the frame's auto-highlight runs:
        frame.performAutoHighlight();

        // THEN only the exact-case occurrence is highlighted, and the find
        // panel's case-sensitivity checkbox reflects the filter's setting:
        assertEquals(1, highlightCount(frame));
        assertTrue(frame.getNoteViewer().getTextFindPanel().isCaseSensitive());
    }

    @Test
    void performAutoHighlight_queryWithoutTextFilter_shouldNotHighlightAnything() {
        // GIVEN a ReaderFrame created from a query that only filters by tag:
        Query query = new Query();
        query.addFilter(new TagFilter(List.of(new Tag("fox")), TagFilter.FilterType.ALL));
        ReaderFrame frame = new ReaderFrame(notesWithFoxes(), query);

        // WHEN the frame's auto-highlight runs:
        frame.performAutoHighlight();

        // THEN nothing is highlighted and the find panel stays hidden:
        assertEquals(0, highlightCount(frame));
        assertFalse(frame.getNoteViewer().getTextFindPanel().isVisible());
    }

    @Test
    void performAutoHighlight_withNullQuery_shouldNotHighlightAnything() {
        // GIVEN a ReaderFrame created without a query:
        ReaderFrame frame = new ReaderFrame(notesWithFoxes());

        // WHEN the frame's auto-highlight runs:
        frame.performAutoHighlight();

        // THEN nothing is highlighted and the find panel stays hidden:
        assertEquals(0, highlightCount(frame));
        assertFalse(frame.getNoteViewer().getTextFindPanel().isVisible());
    }

    @Test
    void performAutoHighlight_withBlankTextFilter_shouldNotHighlightAnything() {
        // GIVEN a ReaderFrame created from a query whose text filter is blank
        // (a hand-built or loaded Query could contain one; the find panel
        // has nothing searchable to run for it):
        Query query = new Query();
        query.addFilter(new TextFilter("   "));
        ReaderFrame frame = new ReaderFrame(notesWithFoxes(), query);

        // WHEN the frame's auto-highlight runs:
        frame.performAutoHighlight();

        // THEN nothing is highlighted and the find panel stays hidden:
        assertEquals(0, highlightCount(frame));
        assertFalse(frame.getNoteViewer().getTextFindPanel().isVisible());
    }

    @Test
    void performAutoHighlight_withMultipleTextFilters_shouldUseTheFirstOne() {
        // GIVEN a ReaderFrame created from a query with two text filters:
        Query query = new Query();
        query.addFilter(new TextFilter("fox"));
        query.addFilter(new TextFilter("elephant"));
        ReaderFrame frame = new ReaderFrame(List.of(note("a fox, an elephant, another fox")), query);

        // WHEN the frame's auto-highlight runs:
        frame.performAutoHighlight();

        // THEN the first filter's term ("fox") is highlighted, not the second:
        assertEquals(2, highlightCount(frame));
    }

    @Test
    void performAutoHighlight_calledTwice_shouldOnlyHighlightOnce() {
        // GIVEN a ReaderFrame with a text filter, whose auto-highlight ran on
        // first activation and highlighted all three "fox" occurrences:
        Query query = new Query();
        query.addFilter(new TextFilter("fox"));
        ReaderFrame frame = new ReaderFrame(notesWithFoxes(), query);
        frame.performAutoHighlight();
        assertEquals(3, highlightCount(frame));

        // WHEN the user dismisses the find panel (ESC clears the highlights),
        // and the frame is activated again (e.g. the user clicks another
        // frame, then clicks back), triggering another auto-highlight call:
        frame.getNoteViewer().getTextFindPanel().reset();
        assertEquals(0, highlightCount(frame));
        frame.performAutoHighlight();

        // THEN the auto-highlight is NOT re-run:
        assertEquals(0, highlightCount(frame));
    }

    @Test
    void frameActivation_shouldTriggerAutoHighlight() {
        // GIVEN a ReaderFrame created from a query that filters for "fox":
        Query query = new Query();
        query.addFilter(new TextFilter("fox"));
        ReaderFrame frame = new ReaderFrame(notesWithFoxes(), query);
        assertEquals(0, highlightCount(frame)); // nothing yet - the ctor must not highlight

        // WHEN the desktop pane activates the frame, as it does when the frame
        // is shown and selected. JInternalFrame.setSelected() is a no-op in
        // headless tests, so dispatch a synthetic activation event straight
        // at the listener(s) the constructor registered:
        for (InternalFrameListener listener : frame.getInternalFrameListeners()) {
            listener.internalFrameActivated(
                new InternalFrameEvent(frame, InternalFrameEvent.INTERNAL_FRAME_ACTIVATED));
        }

        // THEN the wiring works end to end and all three occurrences are highlighted:
        assertEquals(3, highlightCount(frame));
    }

    private static List<Note> notesWithFoxes() {
        return List.of(
            note("one fox two foxes three fox"),
            note("no animals here")
        );
    }

    private static Note note(String text) {
        return new Note().setText(text);
    }

    private static int highlightCount(ReaderFrame frame) {
        return frame.getNoteViewer().getTextPane().getHighlighter().getHighlights().length;
    }
}
