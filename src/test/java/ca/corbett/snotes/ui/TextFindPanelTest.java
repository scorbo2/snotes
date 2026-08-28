package ca.corbett.snotes.ui;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.JButton;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
        // GIVEN a document with "apple" in three different cases and the caret at the start:
        textPane.setText("Apple apple APPLE");

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
        assertEquals(2, highlightCount());

        // WHEN the user enables case sensitivity:
        panel.setCaseSensitive(true);

        // THEN only the exact-case match remains highlighted:
        assertEquals(1, highlightCount());
    }

    @Test
    void reset_withCaseSensitivityOn_shouldRestoreInitialState() {
        // GIVEN an active case-sensitive search:
        textPane.setText("Foo foo");
        panel.setCaseSensitive(true);
        panel.findNext("foo");
        assertEquals(1, highlightCount());

        // WHEN the panel is reset (e.g. the user pressed ESC):
        panel.reset();

        // THEN the checkbox is back to its default (unchecked) state and highlights are cleared:
        assertFalse(panel.isCaseSensitive());
        assertEquals(0, highlightCount());
    }

    private int highlightCount() {
        return textPane.getHighlighter().getHighlights().length;
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
