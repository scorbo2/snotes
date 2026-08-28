package ca.corbett.snotes.ui;

import ca.corbett.extras.ScrollUtil;
import ca.corbett.snotes.AppConfig;
import ca.corbett.snotes.model.Note;
import ca.corbett.snotes.ui.actions.UIReloadAction;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

/**
 * Wraps a JTextPane in a JScrollPane, and provides a read-only view of a List of Note instances.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 * @since Snotes 2.0
 */
public class MultiNoteViewer extends JPanel implements UIReloadable {

    /**
     * Callers can listen for Note selection events by implementing this interface.
     * When the user clicks anywhere in the text of a given Note, we'll figure out exactly
     * which Note in the list was clicked, and then fire an event to the listener with
     * the Note that was clicked.
     */
    public interface NoteSelectedListener {
        /**
         * The given Note was just selected by the user.
         */
        void onNoteSelected(Note note);
    }

    private static final Logger log = Logger.getLogger(MultiNoteViewer.class.getName());

    private final List<NoteSelectedListener> listeners;
    private final List<Integer> positionOffsets;
    private final JTextPane textPane;
    private final List<Note> notes;
    private final TextFindPanel textFindPanel;

    /**
     * Creates a new MultiNoteViewer for the given List of Notes.
     * If the list is empty or null, you get an empty MultiNoteViewer.
     */
    public MultiNoteViewer(List<Note> notes) {
        this.listeners = new CopyOnWriteArrayList<>();
        this.notes = new ArrayList<>();
        this.positionOffsets = new ArrayList<>();
        this.textPane = new JTextPane();
        this.textPane.setEditable(false);
        this.textPane.addMouseListener(new RightClickListener(buildPopupMenu()));
        textPane.addMouseListener(new TextFieldMouseListener());
        textFindPanel = new TextFindPanel(textPane, this::hideTextFindPanel);
        textFindPanel.setVisible(false);
        setLayout(new BorderLayout());
        add(ScrollUtil.buildScrollPane(textPane), BorderLayout.CENTER);
        add(textFindPanel, BorderLayout.SOUTH);
        setStyles();
        setNotes(notes);
        UIReloadAction.getInstance().registerReloadable(this);

        // We unfortunately can't use KeyStrokeManager for this, because it
        // wants an owning Window, whereas we are in a JInternalFrame :(
        Action keyAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showTextFindPanel();
            }
        };
        InputMap inputMap = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        inputMap.put(KeyStroke.getKeyStroke("control F"), "textFindAction");
        getActionMap().put("textFindAction", keyAction);
    }

    /**
     * Invoke this when the MultiNoteViewer is no longer needed, to clean up resources and unregister from events.
     */
    public void dispose() {
        UIReloadAction.getInstance().unregisterReloadable(this);
    }

    /**
     * Scrolls this viewer to the top. This method must not be invoked until the
     * MultiNoteViewer is fully initialized and visible (otherwise nothing happens).
     */
    public void scrollToTop() {
        SwingUtilities.invokeLater(() -> {
            textPane.setCaretPosition(0);
            try {
                Rectangle rect = textPane.modelToView2D(0).getBounds();
                textPane.scrollRectToVisible(rect);
            }
            catch (BadLocationException e) {
                // ignore - 0 is always valid
            }
        });
    }

    /**
     * Scrolls this viewer to the bottom. This method must not be invoked until the
     * MultiNoteViewer is fully initialized and visible (otherwise nothing happens).
     */
    public void scrollToBottom() {
        SwingUtilities.invokeLater(() -> {
            int length = textPane.getDocument().getLength();
            textPane.setCaretPosition(length);
            try {
                Rectangle rect = textPane.modelToView2D(length).getBounds();
                textPane.scrollRectToVisible(rect);
            }
            catch (BadLocationException e) {
                // ignore - length is always valid
            }
        });
    }

    /**
     * Replaces whatever content is currently shown with the given List of Notes.
     * If the given list is empty or null, the MultiNoteViewer will be cleared and show "(no content)".
     */
    public void setNotes(List<Note> notes) {
        this.textPane.setText("");
        this.notes.clear();
        this.positionOffsets.clear();
        if (notes != null) {
            this.notes.addAll(notes);
        }

        if (this.notes.isEmpty()) {
            textPane.setText("(no content)");
            return;
        }

        Document doc = textPane.getDocument();
        for (Note note : this.notes) {
            positionOffsets.add(doc.getLength());
            try {
                doc.insertString(doc.getLength(), note.getHumanTagLine() + "\n", textPane.getStyle("tag"));
                doc.insertString(doc.getLength(), note.getText() + "\n\n", textPane.getStyle("note"));
            }
            catch (BadLocationException ble) {
                // Should never happen, but let's not ignore it:
                log.warning("MultiNoteViewer: unexpected BadLocationException: " + ble.getMessage());
            }
        }
    }

    public void addNoteSelectedListener(NoteSelectedListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener cannot be null");
        }
        listeners.add(listener);
    }

    public void removeNoteSelectedListener(NoteSelectedListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener cannot be null");
        }
        listeners.remove(listener);
    }

    @Override
    public void reloadUI() {
        setStyles();
    }

    public void showTextFindPanel() {
        textFindPanel.setVisible(true);
        revalidate();
        repaint();
        textFindPanel.focusTextField();
    }

    public void hideTextFindPanel() {
        textFindPanel.setVisible(false);
        revalidate();
        repaint();
    }

    private void fireNoteSelectionEvent(Note note) {
        for (NoteSelectedListener listener : new ArrayList<>(listeners)) {
            listener.onNoteSelected(note);
        }
    }

    /**
     * Sets or updates the text styles for tag and note contents.
     * Can be invoked whenever the UI is reloaded, to ensure that the styles are up-to-date with the current theme.
     */
    private void setStyles() {
        textPane.setBackground(AppConfig.getInstance().getEditorBgColor());

        Style defaultStyle = StyleContext.getDefaultStyleContext().getStyle(StyleContext.DEFAULT_STYLE);
        Style tagStyle = textPane.getStyle("tag");
        if (tagStyle == null) {
            tagStyle = textPane.addStyle("tag", defaultStyle);
        }
        Font tagFont = AppConfig.getInstance().getTagFont();
        StyleConstants.setFontFamily(tagStyle, tagFont.getFamily());
        StyleConstants.setFontSize(tagStyle, tagFont.getSize());
        StyleConstants.setBold(tagStyle, tagFont.isBold());
        StyleConstants.setItalic(tagStyle, tagFont.isItalic());
        StyleConstants.setForeground(tagStyle, AppConfig.getInstance().getTagFontColor());

        Style noteStyle = textPane.getStyle("note");
        if (noteStyle == null) {
            noteStyle = textPane.addStyle("note", defaultStyle);
        }
        Font noteFont = AppConfig.getInstance().getNoteFont();
        StyleConstants.setFontFamily(noteStyle, noteFont.getFamily());
        StyleConstants.setFontSize(noteStyle, noteFont.getSize());
        StyleConstants.setBold(noteStyle, noteFont.isBold());
        StyleConstants.setItalic(noteStyle, noteFont.isItalic());
        StyleConstants.setForeground(noteStyle, AppConfig.getInstance().getNoteFontColor());

        // Re-insert all content to force the styles to be applied to everything that's already there.
        // We can only get away with this because this is a read-only viewer, so we don't have to
        // worry that the user may have changed our content since we originally inserted it.
        int pos = textPane.getCaretPosition();
        textPane.setText("");
        Document doc = textPane.getDocument();
        for (Note note : notes) {
            try {
                doc.insertString(doc.getLength(), note.getHumanTagLine() + "\n", textPane.getStyle("tag"));
                doc.insertString(doc.getLength(), note.getText() + "\n\n", textPane.getStyle("note"));
            }
            catch (BadLocationException ble) {
                // Should never happen, but let's not ignore it:
                log.warning("MultiNoteViewer: unexpected BadLocationException: " + ble.getMessage());
            }
        }
        textPane.setCaretPosition(pos);
    }

    /**
     * When the user clicks anywhere in the text of a given Note, we'll figure out exactly
     * which Note in the list was clicked, and then show its details in the detailPanel.
     * The user will have the ability to pop a WriterFrame for that Note.
     */
    private class TextFieldMouseListener extends MouseAdapter {
        @Override
        public void mouseClicked(MouseEvent e) {
            int clickPosition = textPane.viewToModel2D(e.getPoint());
            for (int i = positionOffsets.size() - 1; i >= 0; i--) {
                int offset = positionOffsets.get(i);
                if (clickPosition >= offset) {
                    // This is the note that was selected:
                    fireNoteSelectionEvent(notes.get(i));
                    return;
                }
            }
        }
    }

    private JPopupMenu buildPopupMenu() {
        JPopupMenu popupMenu = new JPopupMenu();
        JMenuItem copyItem = new JMenuItem("Copy");
        copyItem.addActionListener(e -> textPane.copy());
        JMenuItem selectAllItem = new JMenuItem("Select All");
        selectAllItem.addActionListener(e -> textPane.selectAll());

        popupMenu.add(copyItem);
        popupMenu.add(selectAllItem);

        popupMenu.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                boolean hasSelection = textPane.getSelectionStart() != textPane.getSelectionEnd();
                copyItem.setEnabled(hasSelection);
                selectAllItem.setEnabled(textPane.getDocument().getLength() > 0);
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                // no-op
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
                // no-op
            }
        });

        return popupMenu;
    }
}
