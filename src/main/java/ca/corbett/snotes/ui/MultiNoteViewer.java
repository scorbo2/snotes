package ca.corbett.snotes.ui;

import ca.corbett.extras.ScrollUtil;
import ca.corbett.snotes.model.Note;

import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import java.awt.BorderLayout;
import java.awt.Font;
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
public class MultiNoteViewer extends JPanel {

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
        textPane.addMouseListener(new TextFieldMouseListener());
        setLayout(new BorderLayout());
        add(ScrollUtil.buildScrollPane(textPane), BorderLayout.CENTER);
        setNotes(notes);
    }

    /**
     * Replaces whatever content is currently shown with the given List of Notes.
     * If the given list is empty or null, the MultiNoteViewer will be cleared and show "(no content)".
     */
    public void setNotes(List<Note> notes) {
        this.notes.clear();
        this.positionOffsets.clear();
        if (notes != null) {
            this.notes.addAll(notes);
        }

        if (this.notes.isEmpty()) {
            textPane.setText("(no content)");
            return;
        }

        setStyles();
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

    private void fireNoteSelectionEvent(Note note) {
        for (NoteSelectedListener listener : new ArrayList<>(listeners)) {
            listener.onNoteSelected(note);
        }
    }

    /**
     * A future ticket will make font colors, faces, and other styles customizable.
     * For now, we will go with a hard-coded style set.
     * <p>
     *  TODO when this gets wired up to AppConfig, we'll have to respond to UIReload events...
     * </p>
     */
    private void setStyles() {
        Style defaultStyle = StyleContext.getDefaultStyleContext().getStyle(StyleContext.DEFAULT_STYLE);
        Style tagStyle = textPane.addStyle("tag", defaultStyle);
        StyleConstants.setFontFamily(tagStyle, Font.MONOSPACED);
        StyleConstants.setFontSize(tagStyle, 16);
        StyleConstants.setBold(tagStyle, true);
        Style noteStyle = textPane.addStyle("note", defaultStyle);
        StyleConstants.setFontFamily(noteStyle, Font.SERIF);
        StyleConstants.setFontSize(noteStyle, 14);
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
}
