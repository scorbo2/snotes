package ca.corbett.snotes.ui;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.JButton;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.text.Highlighter;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TextFindPanelTest {

    private JTextPane textPane;
    private TextFindPanel panel;

    @BeforeEach
    void setUp() {
        // GIVEN a text pane with some mixed-case content for searching:
        textPane = new JTextPane();
        panel = new TextFindPanel(textPane, null);
    }

    @Test
    void newPanel_caseSensitiveCheckbox_shouldDefaultToUnchecked() {
        // WHEN a panel is freshly constructed:
        // THEN case sensitivity is off by default:
        assertFalse(panel.isCaseSensitive());
    }

    @Test
    void newPanel_buttons_shouldHaveShortenedLabels() {
        // GIVEN the buttons were shortened to make room for the case sensitive checkbox:
        List<String> buttonLabels = new ArrayList<>();
        for (Component component : panel.getComponents()) {
            if (component instanceof JButton button) {
                buttonLabels.add(button.getText());
            }
        }

        // THEN the labels are "Next" and "Prev" in that order:
        assertEquals(List.of("Next", "Prev"), buttonLabels);
    }

    @Test
    void findAllAndHighlight_caseSensitivityOff_shouldMatchAnyCase() {
        // GIVEN a document with "the" in three different cases:
        textPane.setText("The quick brown fox the THE");

        // WHEN we search for "the" with case sensitivity off:
        int firstMatch = panel.findAllAndHighlight("the");

        // THEN all three matches are highlighted, starting at index 0:
        assertEquals(0, firstMatch);
        assertEquals(3, highlightCount());
    }

    @Test
    void findAllAndHighlight_caseSensitivityOn_shouldOnlyMatchExactCase() {
        // GIVEN a document with "the" in three different cases:
        textPane.setText("The quick brown fox the THE");
        panel.setCaseSensitive(true);

        // WHEN we search for "the" with case sensitivity on:
        int firstMatch = panel.findAllAndHighlight("the");

        // THEN only the exact-case match at index 20 is highlighted:
        assertEquals(20, firstMatch);
        assertEquals(1, highlightCount());
    }

    @Test
    void findNext_caseSensitivityOn_shouldSkipMatchesWithDifferentCase() {
        // GIVEN a document with "apple" in three different cases and the caret at the start
        // (set explicitly, because setText() can leave the caret at the end depending on the thread it runs on):
        textPane.setText("Apple apple APPLE");
        textPane.setCaretPosition(0);

        // WHEN we search for "apple" (lowercase) with case sensitivity on:
        boolean found = panel.findNext("apple");

        // THEN the lowercase match at [6, 11) is selected:
        assertTrue(found);
        assertEquals(6, textPane.getSelectionStart());
        assertEquals(11, textPane.getSelectionEnd());
    }

    @Test
    void findNext_caseSensitivityOff_shouldMatchAnyCase() {
        // GIVEN a document with "apple" in three different cases and the caret at the end:
        textPane.setText("Apple apple APPLE");
        textPane.setCaretPosition(textPane.getDocument().getLength());

        // WHEN we search for "APPLE" (uppercase) with case sensitivity off,
        // no match is found from the end, so the search wraps around:
        boolean found = panel.findNext("APPLE");

        // THEN the first (any-case) match at [0, 5) is selected:
        assertTrue(found);
        assertEquals(0, textPane.getSelectionStart());
        assertEquals(5, textPane.getSelectionEnd());
    }

    @Test
    void findNext_caseSensitivityOn_withNoExactCaseMatches_shouldReturnFalse() {
        // GIVEN a document that only contains "Apple" (capitalized):
        textPane.setText("Apple");
        panel.setCaseSensitive(true);

        // WHEN we search for lowercase "apple":
        boolean found = panel.findNext("apple");

        // THEN no match is found:
        assertFalse(found);
    }

    @Test
    void findPrevious_caseSensitivityOn_shouldSkipMatchesWithDifferentCase() {
        // GIVEN a document with "apple" in three different cases and the caret at the end:
        textPane.setText("Apple apple APPLE");
        textPane.setCaretPosition(textPane.getDocument().getLength());
        panel.setCaseSensitive(true);

        // WHEN we search backwards for "apple" (lowercase):
        boolean found = panel.findPrevious("apple");

        // THEN the lowercase match at [6, 11) is selected, not the uppercase "APPLE" at the end:
        assertTrue(found);
        assertEquals(6, textPane.getSelectionStart());
        assertEquals(11, textPane.getSelectionEnd());
    }

    @Test
    void setCaseSensitive_withExistingSearch_shouldRehighlightWithNewSensitivity() {
        // GIVEN an active case-insensitive search that found two matches
        // (the search term was entered in the search field, as the user would type it):
        textPane.setText("Foo foo");
        searchField().setText("foo");
        panel.findNext("foo");
        // One base highlight on the first match, plus the distinct
        // "current match" highlight on the second one:
        assertEquals(2, highlightCount());

        // WHEN the user enables case sensitivity:
        panel.setCaseSensitive(true);

        // THEN only the exact-case match remains highlighted
        // (the current match highlight is cleared along with all highlights):
        assertEquals(1, highlightCount());
    }

    @Test
    void reset_withCaseSensitivityOn_shouldRestoreInitialState() {
        // GIVEN an active case-sensitive search:
        textPane.setText("Foo foo");
        panel.setCaseSensitive(true);
        panel.findNext("foo");
        // One "current match" highlight:
        assertEquals(1, highlightCount());

        // WHEN the panel is reset (e.g. the user pressed ESC):
        panel.reset();

        // THEN the checkbox is back to its default (unchecked) state and highlights are cleared:
        assertFalse(panel.isCaseSensitive());
        assertEquals(0, highlightCount());
    }

    @Test
    void findNext_withMultipleMatches_shouldMarkCurrentMatchWithDistinctHighlight() {
        // GIVEN a document with three matches and the caret at the start
        // (the match at index 0 is skipped, because the caret is already on it;
        // set explicitly, because setText() can leave the caret at the end
        // depending on the thread it runs on):
        textPane.setText("apple apple apple");
        textPane.setCaretPosition(0);

        // WHEN we search for the next match:
        boolean found = panel.findNext("apple");

        // THEN the found match at [6, 11) uses a distinct highlight painter,
        // while the other matches use the base highlight painter:
        assertTrue(found);
        List<Highlighter.HighlightPainter> otherPainters = highlightPaintersAt(0, 5);
        List<Highlighter.HighlightPainter> currentPainters = highlightPaintersAt(6, 11);
        assertEquals(1, otherPainters.size());
        assertEquals(1, currentPainters.size());
        assertNotEquals(otherPainters.get(0), currentPainters.get(0));
    }

    @Test
    void findNext_pressingNextTwice_shouldMoveCurrentMarkToNextMatch() {
        // GIVEN a document with three matches and the caret at the start
        // (set explicitly, because setText() can leave the caret at the end
        // depending on the thread it runs on):
        textPane.setText("apple apple apple");
        textPane.setCaretPosition(0);

        // WHEN we press next twice:
        panel.findNext("apple");
        panel.findNext("apple");

        // THEN the distinct highlight has moved to the last match [12, 17), and the
        // other matches [0, 5) and [6, 11) only carry the base highlight again:
        List<Highlighter.HighlightPainter> currentPainters = highlightPaintersAt(12, 17);
        List<Highlighter.HighlightPainter> otherPainters = highlightPaintersAt(6, 11);
        assertEquals(1, currentPainters.size());
        assertEquals(1, otherPainters.size());
        assertEquals(1, highlightPaintersAt(0, 5).size());
        assertNotEquals(otherPainters.get(0), currentPainters.get(0));
        // The two non-current matches share the base highlight painter:
        assertEquals(otherPainters.get(0), highlightPaintersAt(0, 5).get(0));
    }

    @Test
    void findNext_withNoMatches_shouldLeaveNoHighlightsBehind() {
        // GIVEN a document with no occurrences of the search term:
        textPane.setText("banana");

        // WHEN we search for a term that is not present:
        boolean found = panel.findNext("apple");

        // THEN no highlights of any kind (base or current mark) remain:
        assertFalse(found);
        assertEquals(0, highlightCount());
    }

    @Test
    void findNext_withNewTermThatHasNoMatches_shouldClearPreviousHighlights() {
        // GIVEN an active search that highlighted two matches:
        textPane.setText("apple apple");
        textPane.setCaretPosition(0);
        assertTrue(panel.findNext("apple"));
        assertEquals(2, highlightCount());

        // WHEN the search term is changed to one that does not match:
        boolean found = panel.findNext("zebra");

        // THEN the stale highlights from the previous term are cleared:
        assertFalse(found);
        assertEquals(0, highlightCount());
    }

    @Test
    void findPrevious_withNewTermThatHasNoMatches_shouldClearPreviousHighlights() {
        // GIVEN an active search that highlighted two matches (caret at the end):
        textPane.setText("apple apple");
        textPane.setCaretPosition(textPane.getDocument().getLength());
        assertTrue(panel.findPrevious("apple"));
        assertEquals(2, highlightCount());

        // WHEN the search term is changed to one that does not match:
        boolean found = panel.findPrevious("zebra");

        // THEN the stale highlights from the previous term are cleared:
        assertFalse(found);
        assertEquals(0, highlightCount());
    }

    private int highlightCount() {
        return textPane.getHighlighter().getHighlights().length;
    }

    private List<Highlighter.HighlightPainter> highlightPaintersAt(int start, int end) {
        List<Highlighter.HighlightPainter> painters = new ArrayList<>();
        for (Highlighter.Highlight highlight : textPane.getHighlighter().getHighlights()) {
            if (highlight.getStartOffset() == start && highlight.getEndOffset() == end) {
                painters.add(highlight.getPainter());
            }
        }
        return painters;
    }

    private JTextField searchField() {
        for (Component component : panel.getComponents()) {
            if (component instanceof JTextField textField) {
                return textField;
            }
        }
        throw new AssertionError("TextFindPanel should contain a JTextField");
    }
}
