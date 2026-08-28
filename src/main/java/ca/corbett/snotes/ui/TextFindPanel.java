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
import java.util.logging.Logger;

/**
 * Represents a text find panel that can be associated with a JTextPane for the purpose
 * of finding text within that text pane. A text box is provided to enter search text,
 * a checkbox to make the search case sensitive, and "Next" and "Prev" buttons are
 * provided to navigate through the search results.
 * A status label shows the number of found matches.
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
    private final DefaultHighlighter.DefaultHighlightPainter highlightPainter;

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
        this.highlightPainter = new DefaultHighlighter.DefaultHighlightPainter(Color.YELLOW);
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
     * Find all occurrences of the search term and highlight them.
     * Whether matching is case sensitive is determined by the "Case Sensitive" checkbox.
     * @return the index of the first match, or -1 if not found
     */
    public int findAllAndHighlight(String searchTerm) {
        boolean ignoreCase = !isCaseSensitive();
        // Clear existing highlights
        highlighter.removeAllHighlights();

        if (searchTerm == null || searchTerm.isBlank()) {
            return -1;
        }

        String documentText = textPane.getText();
        int firstMatch = -1;
        int index = 0;
        int matchCount = 0;

        while (index <= documentText.length() - searchTerm.length()) {
            int found = indexOf(documentText, searchTerm, index, ignoreCase);
            if (found == -1) {
                break;
            }

            // Add highlight for this match
            try {
                matchCount++;
                highlighter.addHighlight(found, found + searchTerm.length(), highlightPainter);
            } catch (BadLocationException e) {
                break;
            }

            if (firstMatch == -1) {
                firstMatch = found;
            }

            // Move past this match (use found + 1 to find overlapping matches)
            // (example: searching for "ana" in "banana" should find two matches, not just one)
            index = found + 1;
        }

        statusLabel.setText(matchCount + " match" + (matchCount == 1 ? "" : "es"));
        findNextButton.setEnabled(matchCount > 0);
        findPreviousButton.setEnabled(matchCount > 0);

        return firstMatch;
    }

    /**
     * Finds the next occurrence of the search term starting from the current caret position.
     * Selects the match and scrolls to it.
     * Whether matching is case sensitive is determined by the "Case Sensitive" checkbox.
     *
     * @param searchTerm The text to search for.
     * @return true if a match was found and selected, false otherwise.
     */
    public boolean findNext(String searchTerm) {
        boolean ignoreCase = !isCaseSensitive();
        findAllAndHighlight(searchTerm);
        if (searchTerm == null || searchTerm.isEmpty()) {
            return false;
        }

        String documentText = textPane.getText();
        int caretPos = textPane.getCaretPosition();

        // Start searching from the current caret position
        int startIndex = caretPos;
        int foundIndex = -1;

        // Search from current position to end
        foundIndex = indexOf(documentText, searchTerm, startIndex, ignoreCase);

        // If the found index is our current caret position, it means we're already
        // at the match, so look for the next one instead:
        if (foundIndex == caretPos) {
            foundIndex = indexOf(documentText, searchTerm, startIndex + 1, ignoreCase);
        }

        // If not found, wrap around to the beginning
        if (foundIndex == -1) {
            foundIndex = indexOf(documentText, searchTerm, 0, ignoreCase);
        }

        if (foundIndex != -1) {
            try {
                textPane.select(foundIndex, foundIndex + searchTerm.length());
                Rectangle2D visibleRect = textPane.modelToView2D(foundIndex);
                if (visibleRect != null && visibleRect.getBounds() != null) {
                    textPane.scrollRectToVisible(visibleRect.getBounds());
                }
            } catch (BadLocationException e) {
                log.warning("Unexpected BadLocationException while highlighting search result: " + e.getMessage());
                return false;
            }

            return true;
        }

        return false;
    }

    /**
     * Finds the previous occurrence.
     * Whether matching is case sensitive is determined by the "Case Sensitive" checkbox.
     */
    public boolean findPrevious(String searchTerm) {
        boolean ignoreCase = !isCaseSensitive();
        findAllAndHighlight(searchTerm);
        if (searchTerm == null || searchTerm.isEmpty()) {
            return false;
        }

        String documentText = textPane.getText();
        int caretPos = textPane.getCaretPosition();
        int docLength = documentText.length();

        // Start searching from just before the caret
        int startIndex = caretPos - searchTerm.length();
        if (startIndex < 0) startIndex = 0;

        int foundIndex = -1;

        // Search backwards from current position
        foundIndex = lastIndexOf(documentText, searchTerm, startIndex - 1, ignoreCase);

        // If not found, wrap around to the end
        if (foundIndex == -1) {
            foundIndex = lastIndexOf(documentText, searchTerm, docLength - 1, ignoreCase);
        }

        if (foundIndex != -1) {
            try {
                textPane.select(foundIndex, foundIndex + searchTerm.length());
                Rectangle2D visibleRect = textPane.modelToView2D(foundIndex);
                if (visibleRect != null && visibleRect.getBounds() != null) {
                    textPane.scrollRectToVisible(visibleRect.getBounds());
                }
            } catch (BadLocationException e) {
                log.warning("Unexpected BadLocationException while highlighting search result: " + e.getMessage());
                return false;
            }

            return true;
        }

        return false;
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

    /**
     * Case (in)sensitive lastIndexOf. See {@link #indexOf} for why regionMatches is used.
     * Callers must guarantee that needle is non-empty,
     * as empty search terms are filtered out by the public search methods.
     */
    private static int lastIndexOf(String haystack, String needle, int fromIndex, boolean ignoreCase) {
        if (!ignoreCase) {
            return haystack.lastIndexOf(needle, fromIndex);
        }
        // Matches String.lastIndexOf(): a negative fromIndex means no search
        if (fromIndex < 0) {
            return -1;
        }
        int start = Math.min(fromIndex, haystack.length() - needle.length());
        for (int i = start; i >= 0; i--) {
            if (haystack.regionMatches(true, i, needle, 0, needle.length())) {
                return i;
            }
        }
        return -1;
    }
}
