package ca.corbett.snotes.ui;

import ca.corbett.extras.MessageUtil;
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
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
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
    private final List<Note> notes;
    private final Query query;
    private MultiNoteViewer noteViewer;
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
        this.notes = notes == null ? List.of() : new ArrayList<>(notes);
        this.query = query; // fine if null
        setSize(new Dimension(500, 400));
        setMinimumSize(new Dimension(500, 400));
        setDefaultCloseOperation(JInternalFrame.DISPOSE_ON_CLOSE);
        setFrameIcon(Resources.getLogoIcon(16));
        initComponents();
    }

    @Override
    public void dispose() {
        // Clean up any resources we have open:
        if (noteViewer != null) {
            noteViewer.dispose();
        }
        super.dispose();
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
        noteViewer = new MultiNoteViewer(notes);
        if (notes.isEmpty()) {
            if (query == null) {
                // A query returning an empty list is no big deal - the filters are just too strict.
                // However... if we were created without a Query, and we were given no notes,
                // it's not technically an error, but it is kind of suspicious, so let's log it:
                log.warning("ReaderFrame created with an empty list of notes!");
            }
        }
        noteViewer.addNoteSelectedListener(this::onNoteSelected);
        return noteViewer;
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
     * Invoked from our MultiNoteViewer when the user selects a note.
     * We'll show its location in the detail panel, and enable the "Edit" button if it has a source file.
     */
    private void onNoteSelected(Note note) {
        detailNote = note;
        // getRelativePath() handles null Notes:
        String path = Note.getRelativePath(detailNote, AppConfig.getInstance().getDataDirectory());
        detailLabel.setText(path.isBlank() ? " (n/a) " : path);
        setDetailPanelVisible(note != null);
    }

    private MessageUtil getMessageUtil() {
        if (messageUtil == null) {
            messageUtil = new MessageUtil(this, log);
        }
        return messageUtil;
    }
}
