package ca.corbett.snotes.ui;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Represents a text find panel that can be associated with a JTextPane for the purpose
 * of finding text within that text pane. A text box is provided to enter search text,
 * a checkbox to make the search case sensitive, and "Next" and "Prev" buttons are
 * provided to navigate through the search results.
 * A status label shows the number of found matches.
 * All matches are highlighted, and the match that "Next"/"Prev" is currently on
 * uses a distinct, more prominent highlight so it can be distinguished from the
 * other matches.
 * <p>
 * Pressing ESC while focus is in the text search field will trigger the configured
 * closeSearch action. Callers would typically use this to hide the TextFindPanel.
 * </p>
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 * @since Snotes 2.2
 */
public class TextFindPanel extends JPanel {

    private static final Logger log = Logger.getLogger(TextFindPanel.class.getName());

    /**
     * Implement this to receive notification when the user has hit ESC with focus in
     * the search field. This typically would be used to hide the TextFindPanel.
     */
    @FunctionalInterface
    public interface OnCloseSearch {
        void closeSearch();
    }

    private final JTextPane textPane;
    private JTextField searchField;
    private JCheckBox caseSensitiveCheckbox;
    private JButton findNextButton;
    private JButton findPreviousButton;
    private JLabel statusLabel;
    private final OnCloseSearch closeSearch;
    private final DefaultHighlighter highlighter;
    private final DefaultHighlighter.DefaultHighlightPainter matchHighlightPainter;
    private final DefaultHighlighter.DefaultHighlightPainter currentMatchHighlightPainter;

    /**
     * Creates a new TextFindPanel and associates it with the given JTextPane.
     * The closeSearch callback will be triggered when the user hits ESC with focus in the search field.
     *
     * @param textPane the JTextPane to search within (must not be null)
     * @param closeSearch the optional callback to trigger when the user hits ESC with focus in the search field
     */
    public TextFindPanel(JTextPane textPane, OnCloseSearch closeSearch) {
        if (textPane == null) {
            throw new IllegalArgumentException("textPane must not be null");
        }
        this.textPane = textPane;
        this.closeSearch = closeSearch;
        this.highlighter = (DefaultHighlighter) textPane.getHighlighter();
        this.matchHighlightPainter = new DefaultHighlighter.DefaultHighlightPainter(Color.YELLOW);
        // A distinct, more prominent color for the match that "Next"/"Prev" is
        // currently on, so it stands out when many matches are highlighted:
        this.currentMatchHighlightPainter = new DefaultHighlighter.DefaultHighlightPainter(Color.ORANGE);
        initComponents();
        reset();
    }

    /**
     * Call this to blank out the TextFindPanel and reset it to its initial state,
     * which includes the case sensitive checkbox being unchecked.
     */
    public void reset() {
        searchField.setText("");
        caseSensitiveCheckbox.setSelected(false);
        statusLabel.setText("0 matches");
        findNextButton.setEnabled(false);
        findPreviousButton.setEnabled(false);
        highlighter.removeAllHighlights();
    }

    /**
     * Attempts to set the current focus to our text search field.
     */
    public void focusTextField() {
        searchField.requestFocusInWindow();
    }

    /**
     * Returns true if searches should be case sensitive, i.e. if the
     * "Case Sensitive" checkbox is selected.
     */
    public boolean isCaseSensitive() {
        return caseSensitiveCheckbox.isSelected();
    }

    /**
     * Sets whether searches should be case sensitive. If a search term is
     * currently entered, the highlights and match count are updated to
     * reflect the new sensitivity.
     *
     * @param caseSensitive true for case sensitive matching, false for case insensitive
     */
    public void setCaseSensitive(boolean caseSensitive) {
        caseSensitiveCheckbox.setSelected(caseSensitive);
    }

    /**
     * Pre-populates the search field with the given term, applies the given
     * case-sensitivity, and immediately highlights all occurrences of the term
     * in the associated text pane.
     * <p>
     * Unlike pressing ENTER in the search field, this does not select or
     * scroll to the first match: the caret and scroll position are left
     * alone. The term remains visible and editable in the search field,
     * and the user can press ESC to clear the highlights and close the panel.
     * </p>
     *
     * @param searchTerm the term to search for. null or blank is a no-op.
     * @param caseSensitive whether matching should be case-sensitive
     * @return true if at least one occurrence was highlighted, false otherwise
     */
    public boolean populateAndHighlight(String searchTerm, boolean caseSensitive) {
        // A null or blank term is a true no-op: don't touch the search field,
        // the case-sensitivity checkbox, or any existing highlights:
        if (searchTerm == null || searchTerm.isBlank()) {
            return false;
        }
        // Case sensitivity must be set before the search text, because the
        // checkbox's item listener re-runs the search whenever the field is
        // non-blank: setting the text first would trigger a redundant search.
        setCaseSensitive(caseSensitive);
        searchField.setText(searchTerm);
        return findAllAndHighlight(searchTerm) != -1;
    }

    /**
     * Find all occurrences of the search term and highlight them.
     * Whether matching is case sensitive is determined by the "Case Sensitive" checkbox.
     * @return the index of the first match, or -1 if not found
     */
    public int findAllAndHighlight(String searchTerm) {
        List<Integer> matchStarts = findMatches(searchTerm, !isCaseSensitive());
        paintHighlights(matchStarts, searchTerm, -1);
        updateMatchStatus(matchStarts.size());
        return matchStarts.isEmpty() ? -1 : matchStarts.get(0);
    }

    /**
     * Finds the next occurrence of the search term strictly after the current caret
     * position, wrapping around to the first match if none are further ahead. A match
     * starting exactly at the caret is skipped, so repeated calls advance through
     * the matches in order. Selects the match, scrolls to it, and highlights it with
     * a more prominent highlight so it stands out from the other matches.
     * Whether matching is case sensitive is determined by the "Case Sensitive" checkbox.
     *
     * @param searchTerm The text to search for.
     * @return true if a match was found and selected, false otherwise.
     */
    public boolean findNext(String searchTerm) {
        List<Integer> matchStarts = findMatches(searchTerm, !isCaseSensitive());
        updateMatchStatus(matchStarts.size());
        if (matchStarts.isEmpty()) {
            // Clear any highlights left over from a previous search term:
            paintHighlights(matchStarts, searchTerm, -1);
            return false;
        }

        int caretPos = textPane.getCaretPosition();

        // The next match is the first one that starts strictly after the caret;
        // a match that starts exactly at the caret is skipped, so repeatedly
        // pressing "Next" advances through the matches:
        int foundIndex = -1;
        for (int matchStart : matchStarts) {
            if (matchStart > caretPos) {
                foundIndex = matchStart;
                break;
            }
        }

        // Once we're past the last match, wrap around to the first:
        if (foundIndex == -1) {
            foundIndex = matchStarts.get(0);
        }

        paintHighlights(matchStarts, searchTerm, foundIndex);
        return selectAndScroll(foundIndex, searchTerm.length());
    }

    /**
     * Finds the previous occurrence, selecting it, scrolling to it, and highlighting
     * it with a more prominent highlight so it stands out from the other matches.
     * Whether matching is case sensitive is determined by the "Case Sensitive" checkbox.
     *
     * @param searchTerm The text to search for.
     * @return true if a match was found and selected, false otherwise.
     */
    public boolean findPrevious(String searchTerm) {
        List<Integer> matchStarts = findMatches(searchTerm, !isCaseSensitive());
        updateMatchStatus(matchStarts.size());
        if (matchStarts.isEmpty()) {
            // Clear any highlights left over from a previous search term:
            paintHighlights(matchStarts, searchTerm, -1);
            return false;
        }

        int caretPos = textPane.getCaretPosition();

        // The previous match is the last one that ends strictly before the caret
        // (i.e. starts at or before caret position - search term length - 1):
        int lastAllowedStart = Math.max(0, caretPos - searchTerm.length()) - 1;
        int foundIndex = -1;
        for (int i = matchStarts.size() - 1; i >= 0; i--) {
            if (matchStarts.get(i) <= lastAllowedStart) {
                foundIndex = matchStarts.get(i);
                break;
            }
        }

        // If no match ends before the caret, wrap around to the last match:
        if (foundIndex == -1) {
            foundIndex = matchStarts.get(matchStarts.size() - 1);
        }

        paintHighlights(matchStarts, searchTerm, foundIndex);
        return selectAndScroll(foundIndex, searchTerm.length());
    }

    /**
     * Finds the start offsets of all occurrences of the search term, including
     * overlapping ones (e.g. "ana" in "banana" matches twice).
     * Returns an empty list for null or blank search terms.
     */
    private List<Integer> findMatches(String searchTerm, boolean ignoreCase) {
        List<Integer> matchStarts = new ArrayList<>();
        if (searchTerm == null || searchTerm.isBlank()) {
            return matchStarts;
        }
        String documentText = textPane.getText();
        int index = 0;
        while (index <= documentText.length() - searchTerm.length()) {
            int found = indexOf(documentText, searchTerm, index, ignoreCase);
            if (found == -1) {
                break;
            }
            matchStarts.add(found);
            // Move one past the start of this match so overlapping matches are found too:
            index = found + 1;
        }
        return matchStarts;
    }

    /**
     * Re-paints the highlight for every found match: the base (yellow) highlight,
     * except for the current match (if given), which gets the more prominent one.
     * <p>
     * The highlights are re-painted wholesale rather than overlaying a "current
     * match" mark on top of the base highlights, because DefaultHighlighter paints
     * highlights in reverse insertion order: a mark added after the base highlights
     * would be painted underneath them and never be visible.
     * </p>
     *
     * @param currentMatchStart the start offset of the current match, or -1 for none
     */
    private void paintHighlights(List<Integer> matchStarts, String searchTerm, int currentMatchStart) {
        highlighter.removeAllHighlights();
        if (searchTerm == null) {
            return;
        }
        for (int matchStart : matchStarts) {
            try {
                DefaultHighlighter.DefaultHighlightPainter painter =
                    matchStart == currentMatchStart ? currentMatchHighlightPainter : matchHighlightPainter;
                highlighter.addHighlight(matchStart, matchStart + searchTerm.length(), painter);
            } catch (BadLocationException e) {
                log.warning("Unexpected BadLocationException while highlighting search match: " + e.getMessage());
            }
        }
    }

    /**
     * Selects the given range in the text pane and scrolls it into view.
     *
     * @return true if the range could be selected, false on unexpected document state
     */
    private boolean selectAndScroll(int start, int length) {
        try {
            textPane.select(start, start + length);
            Rectangle2D visibleRect = textPane.modelToView2D(start);
            if (visibleRect != null && visibleRect.getBounds() != null) {
                textPane.scrollRectToVisible(visibleRect.getBounds());
            }
            return true;
        } catch (BadLocationException e) {
            log.warning("Unexpected BadLocationException while selecting search result: " + e.getMessage());
            return false;
        }
    }

    private void updateMatchStatus(int matchCount) {
        statusLabel.setText(matchCount + " match" + (matchCount == 1 ? "" : "es"));
        findNextButton.setEnabled(matchCount > 0);
        findPreviousButton.setEnabled(matchCount > 0);
    }

    private void initComponents() {
        setLayout(new GridBagLayout());

        searchField = new JTextField(20);
        caseSensitiveCheckbox = new JCheckBox("Case Sensitive", false);
        caseSensitiveCheckbox.setToolTipText("When selected, only matches with the exact same case will be found.");
        // An ItemListener (rather than ActionListener) is required here, because
        // programmatic calls to setSelected() only fire ItemEvents:
        caseSensitiveCheckbox.addItemListener(e -> {
            if (!searchField.getText().isBlank()) {
                // Re-highlight so the existing highlights and match count
                // reflect the new case sensitivity:
                findAllAndHighlight(searchField.getText());
            }
        });
        findNextButton = new JButton("Next");
        findNextButton.setPreferredSize(new Dimension(70, 24));
        findNextButton.addActionListener(e -> {
            String searchTerm = searchField.getText();
            if (!findNext(searchTerm)) {
                // findNext handles search wrapping, so if
                // it returns false, it can only mean there are no matches at all:
                JOptionPane.showMessageDialog(this, "No matches found.", "Find text", JOptionPane.INFORMATION_MESSAGE);
            }
        });
        findPreviousButton = new JButton("Prev");
        findPreviousButton.setPreferredSize(new Dimension(70, 24));
        findPreviousButton.addActionListener(e -> {
            String searchTerm = searchField.getText();
            if (!findPrevious(searchTerm)) {
                // findPrevious handles search wrapping, so if
                // it returns false, it can only mean there are no matches at all:
                JOptionPane.showMessageDialog(this, "No matches found.", "Find text", JOptionPane.INFORMATION_MESSAGE);
            }
        });
        statusLabel = new JLabel("0 matches");

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new java.awt.Insets(2, 2, 2, 2);
        gbc.anchor = GridBagConstraints.WEST;

        // Search field - expandable, takes all available horizontal space
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.gridx = 0;
        gbc.gridy = 0;
        add(searchField, gbc);

        // Case sensitive checkbox - fixed width
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0.0;
        gbc.gridx = 1;
        add(caseSensitiveCheckbox, gbc);

        // Next button - fixed width
        gbc.gridx = 2;
        add(findNextButton, gbc);

        // Previous button - fixed width
        gbc.gridx = 3;
        add(findPreviousButton, gbc);

        // Status label - fixed width
        gbc.gridx = 4;
        add(statusLabel, gbc);

        // Add listeners to our text field:
        // - ENTER will trigger find next
        // - SHIFT+ENTER will trigger find previous
        // - ESC will trigger the closeSearch callback
        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    if (e.isShiftDown()) {
                        findPreviousButton.doClick();
                    } else {
                        findNextButton.doClick();
                    }
                } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    reset(); // clear highlights and reset state
                    if (closeSearch != null) {
                        // Caller can handle hiding or dismissing us as they see fit:
                        closeSearch.closeSearch();
                    }
                }
            }
        });

        // Add a document listener to update the status label and enable/disable buttons based on the search term
        searchField.getDocument().addDocumentListener(new DocumentListener() {

            private void textUpdated() {
                boolean hasContent = !searchField.getText().isEmpty();
                findNextButton.setEnabled(hasContent);
                findPreviousButton.setEnabled(hasContent);
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                textUpdated();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                textUpdated();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                textUpdated();
            }
        });
    }

    /**
     * Case (in)sensitive indexOf. Callers must guarantee that needle is non-empty,
     * as empty search terms are filtered out by the public search methods.
     */
    private static int indexOf(String haystack, String needle, int fromIndex, boolean ignoreCase) {
        if (!ignoreCase) {
            return haystack.indexOf(needle, fromIndex);
        }
        // regionMatches(true, ...) instead of lower-casing both strings, because
        // lower-casing can change the length of some Unicode strings (e.g. 'İ' U+0130),
        // which would break index alignment with the original document:
        int start = Math.max(fromIndex, 0);
        int maxStart = haystack.length() - needle.length();
        for (int i = start; i <= maxStart; i++) {
            if (haystack.regionMatches(true, i, needle, 0, needle.length())) {
                return i;
            }
        }
        return -1;
    }

}
