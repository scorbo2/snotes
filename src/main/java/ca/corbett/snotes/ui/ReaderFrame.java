package ca.corbett.snotes.ui;

import ca.corbett.extras.MessageUtil;
import ca.corbett.extras.ScrollUtil;
import ca.corbett.forms.Alignment;
import ca.corbett.forms.FormPanel;
import ca.corbett.forms.fields.LabelField;
import ca.corbett.snotes.AppConfig;
import ca.corbett.snotes.Resources;
import ca.corbett.snotes.model.Note;
import ca.corbett.snotes.model.Query;
import ca.corbett.snotes.model.filter.Filter;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Provides a read-only view of a List of Note instances.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 * @since Snotes 2.0
 */
public class ReaderFrame extends JInternalFrame {

    private static final Logger log = Logger.getLogger(ReaderFrame.class.getName());

    private MessageUtil messageUtil;
    private final List<Integer> positionOffsets;
    private final List<Note> notes;
    private final Query query;
    private JTextPane textPane;
    private JPanel detailPanel;
    private JLabel detailLabel;
    private Note detailNote;
    private JButton closeDetailsButton;

    /**
     * Creates a new ReaderFrame with the given List of Notes.
     * If the list is empty or null, you get an empty ReaderFrame.
     */
    public ReaderFrame(List<Note> notes) {
        this(notes, null);
    }

    /**
     * Creates a new ReaderFrame with the given List of Notes, generated
     * from the given Query. If the list is empty or null, you get an empty ReaderFrame.
     * If the query is non-null, its filter settings will be displayed in a read-only
     * section at the top of the ReaderFrame, for reference.
     */
    public ReaderFrame(List<Note> notes, Query query) {
        super(query == null ? "(untitled)" : query.getName(), true, true, true, true);
        this.notes = notes == null ? List.of() : notes;
        this.query = query; // fine if null
        this.positionOffsets = new ArrayList<>(this.notes.size());
        setSize(new Dimension(500, 400));
        setMinimumSize(new Dimension(500, 400));
        setDefaultCloseOperation(JInternalFrame.DISPOSE_ON_CLOSE);
        setFrameIcon(Resources.getLogoIcon(16));
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        if (query != null) {
            add(buildQueryPanel(), BorderLayout.NORTH);
        }
        add(buildTextPane(), BorderLayout.CENTER);
        add(buildLocationPanel(), BorderLayout.SOUTH);
    }

    /**
     * Builds an informational panel for showing a read-only view of the filters
     * that were used to generate this list of notes. Only visible if a Query
     * is supplied to the constructor (otherwise, we have no idea where they came from).
     */
    private JPanel buildQueryPanel() {
        FormPanel filterPanel = new FormPanel(Alignment.TOP_LEFT);
        filterPanel.setBorderMargin(8);

        if (query != null) {
            filterPanel.add(LabelField.createBoldHeaderLabel("Filters", 14, 0, 0));
            for (Filter filter : query.getFilters()) {
                LabelField label = new LabelField(filter.toString());
                label.getMargins().setTop(0).setBottom(0);
                filterPanel.add(label);
            }
        }

        return filterPanel;
    }

    /**
     * Builds our custom JTextPane and populates it with our content.
     */
    private JComponent buildTextPane() {
        textPane = new JTextPane();
        textPane.setEditable(false);

        if (notes.isEmpty()) {
            if (query == null) {
                // A query returning an empty list is no big deal - the filters are just too strict.
                // However... if we were created without a Query, and we were given no notes,
                // it's not technically an error, but it is kind of suspicious, so let's log it:
                log.warning("ReaderFrame created with an empty list of notes!");
            }
            textPane.setText("(no content)");
            return ScrollUtil.buildScrollPane(textPane);
        }

        setStyles();
        textPane.addMouseListener(new TextFieldMouseListener());
        Document doc = textPane.getDocument();
        for (Note note : notes) {
            positionOffsets.add(doc.getLength());
            try {
                doc.insertString(doc.getLength(), note.getHumanTagLine() + "\n", textPane.getStyle("tag"));
                doc.insertString(doc.getLength(), note.getText() + "\n\n", textPane.getStyle("note"));
            }
            catch (BadLocationException ble) {
                // Should never happen, but let's not ignore it:
                log.warning("ReaderFrame: unexpected BadLocationException: " + ble.getMessage());
            }
        }

        return ScrollUtil.buildScrollPane(textPane);
    }

    /**
     * Builds the detailPanel, which will show information when the user clicks on any Note in the text pane.
     * This shows the relative path of the clicked Note and gives a button to launch a WriterFrame for that Note.
     * The detailPanel includes a button that will "close" itself (hide it until something else is clicked).
     */
    private JPanel buildLocationPanel() {
        detailPanel = new JPanel(new BorderLayout());
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        leftPanel.add(new JLabel("Selected:"));
        detailLabel = new JLabel("(nothing)");
        leftPanel.add(detailLabel);
        JButton editButton = new JButton("Edit");
        editButton.setPreferredSize(new Dimension(80, 24));
        editButton.addActionListener(e -> editSelectedNote());
        leftPanel.add(editButton);
        detailPanel.add(leftPanel, BorderLayout.WEST);

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        closeDetailsButton = new JButton("X");
        closeDetailsButton.addActionListener(e -> setDetailPanelVisible(false));
        closeDetailsButton.setPreferredSize(new Dimension(24, 24));
        rightPanel.add(closeDetailsButton);
        detailPanel.add(rightPanel, BorderLayout.EAST);

        detailPanel.setVisible(false); // we'll show or hide the whole thing based on whether a Note is selected
        return detailPanel;
    }

    private void setDetailPanelVisible(boolean visible) {
        // It is technically possible to get a Note without a source file, so let's be careful:
        closeDetailsButton.setEnabled(detailNote != null && detailNote.getSourceFile() != null);
        detailPanel.setVisible(visible);
        detailPanel.revalidate();
        detailPanel.repaint();
    }

    /**
     * Pops a WriterFrame for the currently selected Note, if any.
     */
    private void editSelectedNote() {
        if (detailNote == null) {
            getMessageUtil().info("Nothing selected.");
            return;
        }

        // Show a new WriterFrame for the selected Note.
        WriterFrame writerFrame = new WriterFrame(detailNote);
        MainWindow.getInstance().addInternalFrame(writerFrame);
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

    private MessageUtil getMessageUtil() {
        if (messageUtil == null) {
            messageUtil = new MessageUtil(this, log);
        }
        return messageUtil;
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
            for (int i = 0; i < positionOffsets.size(); i++) {
                int offset = positionOffsets.get(i);
                if (clickPosition >= offset) {
                    // This is the Note that was clicked:
                    detailNote = notes.get(i);
                    String path = Note.getRelativePath(detailNote, AppConfig.getInstance().getDataDirectory());
                    detailLabel.setText(path.isBlank() ? " (n/a) " : path);
                    setDetailPanelVisible(true);
                }
            }
        }
    }
}
