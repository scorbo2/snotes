package ca.corbett.snotes.ui;

import ca.corbett.extras.MessageUtil;
import ca.corbett.extras.ScrollUtil;
import ca.corbett.extras.ToggleableTabbedPane;
import ca.corbett.forms.Alignment;
import ca.corbett.forms.FormPanel;
import ca.corbett.forms.fields.ShortTextField;
import ca.corbett.snotes.AppConfig;
import ca.corbett.snotes.Resources;
import ca.corbett.snotes.io.DataManager;
import ca.corbett.snotes.model.Note;
import ca.corbett.snotes.model.Tag;
import ca.corbett.snotes.model.TagList;
import ca.corbett.snotes.model.YMDDate;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JInternalFrame;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides an editable view of a single Note.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 * @since Snotes 2.0
 */
public class WriterFrame extends JInternalFrame {

    private static final Logger log = Logger.getLogger(WriterFrame.class.getName());
    private MessageUtil messageUtil;

    private final DataManager dataManager;
    private final Note note;
    private final List<Note> context;
    private ToggleableTabbedPane tabPane;
    private MultiNoteViewer contextViewer;
    private FormPanel headerForm;
    private ShortTextField dateField;
    private ShortTextField tagField;
    private JTextPane textPane;
    private boolean isDirty;
    private final Timer autoSaveTimer;

    /**
     * Creates a new WriterFrame with a new, blank scratch Note, and no context.
     */
    public WriterFrame() {
        this(MainWindow.getInstance().getDataManager().newNote());
    }

    /**
     * Creates a new WriterFrame for the given Note.
     * If the given Note is null, a scratch Note will be created.
     * No context will be shown.
     */
    public WriterFrame(Note note) {
        this(note, null);
    }

    /**
     * Creates a new WriterFrame for the given Note, with the given context.
     * If the given Note is null, a scratch Note will be created.
     *
     * @param note    the Note to edit in this frame. If null, a new scratch Note will be created.
     * @param context an optional List of other Notes to show in the context panel. Can be null or empty.
     */
    public WriterFrame(Note note, List<Note> context) {
        super(Note.getRelativePath(note, AppConfig.getInstance().getDataDirectory()),
              true, true, true, true);
        this.dataManager = MainWindow.getInstance().getDataManager();
        this.context = new ArrayList<>();
        if (context != null) {
            this.context.addAll(context);
        }
        if (note == null) {
            log.warning("WriterFrame created with null note. Creating new scratch note.");
            note = dataManager.newNote();
            setTitle(Note.getRelativePath(note, AppConfig.getInstance().getDataDirectory()));
        }
        this.note = note;
        setSize(new Dimension(500, 400));
        setMinimumSize(new Dimension(500, 400));
        setDefaultCloseOperation(JInternalFrame.DISPOSE_ON_CLOSE);
        setFrameIcon(Resources.getLogoIcon(16));
        this.addInternalFrameListener(new FrameCloseListener());
        initComponents();
        if (dataManager.isScratchNote(note)) {
            // For scratch notes, we want to auto-save every minute, so we set up a timer to do that.
            autoSaveTimer = new Timer(60 * 1000, e -> SwingUtilities.invokeLater(this::save));
            autoSaveTimer.setRepeats(true);
            autoSaveTimer.start();
        }
        else {
            autoSaveTimer = null; // No need for an auto-save timer for real notes
        }
    }

    @Override
    public void dispose() {
        if (autoSaveTimer != null) {
            autoSaveTimer.stop();
        }
        super.dispose();
    }

    public Note getNote() {
        return note;
    }

    /**
     * This is sent to us from MainWindow if the note that we're currently editing
     * had been deleted. We simply close here - no confirmation, we're just done.
     */
    void noteDeleted() {
        isDirty = false;
        dispose();
    }

    /**
     * Saves the current Note WITHOUT closing this internal frame. This is intended for external
     * use mostly (for example, user hits Ctrl+S anywhere in the main window).
     * <p>
     * <b>Important notes about save behavior:</b>
     * </p>
     * <ol>
     *     <li>If the current note is a "scratch" note, this does NOT promote the note to a "real" note.
     *     We will simply perform a scratch save, to save the current contents to the scratch directory.
     *     This allows scratch notes to persist across application restarts, but will not allow them
     *     to appear in search results.</li>
     *     <li>If the current note is a "real" note, this will commit the current contents to the data
     *     directory. This may move the file! Note locations are based on their date and/or tag, both
     *     of which are editable in this frame. If the user has changed either, the file in the data
     *     directory may be moved and/or renamed as needed as a result of this operation. The Note instance
     *     that was passed to the constructor will be updated with a new source file in that case.</li>
     * </ol>
     * <p>
     *     The only way to promote a scratch note to a real note is for the user to hit the "save" button
     *     in this frame, which will execute saveInternal() instead of this method.
     * </p>
     */
    public void save() {
        if (!headerForm.isFormValid()) {
            return; // Validation errors will show on the form (invalid date or missing tags).
        }
        if (!isDirty) {
            return;
        }
        if (dataManager.isScratchNote(note)) {
            try {
                note.setDate(getDate());
                TagList tagList = TagList.fromRawString(tagField.getText());
                for (Tag tag : tagList.getTags()) {
                    note.tag(tag);
                }
                note.setText(textPane.getText());
                dataManager.saveScratch(note);
                isDirty = false;
            }
            catch (IOException ioe) {
                log.log(Level.SEVERE, "Failed to save scratch note: " + note.getSourceFile(), ioe);
            }
        }
        else {
            // Defer to saveInternal but keep the frame open:
            saveInternal(false);
        }
    }

    /**
     * If the current note is a scratch note, we will promote it to a real note.
     * The save path for the note will be determined by its tags and date.
     * This means that existing notes may move within our data directory as a result of this save.
     * Collisions are handled intelligently by data manager.
     *
     * @return true if the save completed successfully, false otherwise (user canceled collision dialog, validation failed)
     */
    private boolean saveInternal(boolean disposeIfSuccessful) {
        if (!headerForm.isFormValid()) {
            return false; // Validation errors will show on the form (invalid date or missing tags).
        }
        if (!isDirty) {
            return true; // counts as successful save, since there are no changes to save!
        }

        note.clearAllTags(); // we will nuke and pave to overwrite old settings
        note.setDate(getDate());
        TagList tagList = TagList.fromRawString(tagField.getText());
        for (Tag tag : tagList.getTags()) {
            note.tag(tag);
        }
        note.setText(textPane.getText());

        DataManager.CollisionStrategy strategy = DataManager.CollisionStrategy.ABORT; // safe default
        if (dataManager.hasCollision(note)) {
            String selection = getMessageUtil().askSelect("Collision detected",
                                                          "There is an existing note with this date and/or tag list." +
                                                              "\nWhat do you want to do?",
                                                          new String[]{"Overwrite it", "Append to it", "Cancel"},
                                                          "Append to it");
            if (selection == null || "Cancel".equals(selection)) {
                return false; // User canceled the save, so we do nothing.
            }
            strategy = "Overwrite it".equals(selection)
                ? DataManager.CollisionStrategy.OVERWRITE
                : DataManager.CollisionStrategy.APPEND;
        }

        try {
            dataManager.save(note, strategy);
            setTitle(Note.getRelativePath(note, AppConfig.getInstance().getDataDirectory())); // path may have changed
            isDirty = false;
            if (disposeIfSuccessful) {
                dispose();
            }
        }
        catch (IOException ioe) {
            getMessageUtil().error("Save error",
                                   "An error occurred while saving the note: " + ioe.getMessage(),
                                   ioe);
            return false;
        }

        return true;
    }

    /**
     * Returns a YMDDate based on the current value in our date field, or null if the field is blank or invalid.
     */
    private YMDDate getDate() {
        if (YMDDate.isValidYMD(dateField.getText())) {
            return new YMDDate(dateField.getText());
        }
        else {
            return null;
        }
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        add(buildHeaderPanel(), BorderLayout.NORTH);
        add(buildTextPanel(), BorderLayout.CENTER);
        add(buildButtonPanel(), BorderLayout.SOUTH);
    }

    private JPanel buildHeaderPanel() {
        headerForm = new FormPanel(Alignment.TOP_LEFT);
        headerForm.setBorderMargin(8);
        dateField = new ShortTextField("Date:", 10);
        dateField.setAllowBlank(true);
        dateField.setHelpText("Optional. If provided, must be in yyyy-MM-dd format.");
        dateField.addValueChangedListener(f -> isDirty = true);
        if (note.hasDate()) {
            dateField.setText(note.getDate().toString());
        }
        if (dataManager.isScratchNote(note) && !note.hasDate()) {
            // Default to today for scratch notes that aren't already dated:
            dateField.setText(new YMDDate().toString());
        }
        headerForm.add(dateField);
        tagField = new ShortTextField("Tag(s):", 20);
        tagField.setAllowBlank(false);
        tagField.addValueChangedListener(f -> isDirty = true);
        tagField.setHelpText("Comma or space-separated list of tags. At least one tag is required.");
        tagField.setText(TagList.fromTagList(note.getTags()).getNonDateTagsAsCommaSeparatedString());
        headerForm.add(tagField);
        return headerForm;
    }

    /**
     * Builds and returns a tabbed pane for the main edit text pane, and an
     * optional context tab if there is context to show.
     */
    private JComponent buildTextPanel() {
        tabPane = new ToggleableTabbedPane();
        int selectedTab = 0;
        if (!context.isEmpty()) {
            contextViewer = new MultiNoteViewer(context);
            tabPane.addTab("Context", contextViewer);
            selectedTab = 1; // start on the edit tab, always
        }

        textPane = new JTextPane();
        textPane.setText(note.getText());
        isDirty = false;

        // Set up a listener such that any edit in this text pane sets our isDirty flag:
        textPane.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                isDirty = true;
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                isDirty = true;
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                isDirty = true;
            }
        });

        tabPane.addTab("Edit", ScrollUtil.buildScrollPane(textPane));
        if (context.isEmpty()) {
            tabPane.setTabHeaderVisible(false);
        }
        tabPane.setSelectedIndex(selectedTab);
        return tabPane;
    }

    private JPanel buildButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        panel.setBorder(BorderFactory.createRaisedBevelBorder());
        JButton btn = new JButton("Save");
        btn.addActionListener(e -> saveInternal(true));
        btn.setPreferredSize(new Dimension(110, 24));
        panel.add(btn);
        return panel;
    }

    private MessageUtil getMessageUtil() {
        if (messageUtil == null) {
            messageUtil = new MessageUtil(MainWindow.getInstance(), log);
        }
        return messageUtil;
    }

    /**
     * We will listen for internal frame close events, and prompt the user about
     * unsaved changes.
     */
    private class FrameCloseListener extends InternalFrameAdapter {

        @Override
        public void internalFrameOpened(InternalFrameEvent e) {
            SwingUtilities.invokeLater(() -> textPane.requestFocusInWindow());
        }

        @Override
        public void internalFrameClosing(InternalFrameEvent e) {
            if (isDirty) {
                int result = getMessageUtil().askYesNoCancel("Unsaved changes",
                                                             "You have unsaved changes. Do you want to save before closing?");
                if (result == MessageUtil.CANCEL) {
                    // User canceled the close, so we need to prevent the frame from closing.
                    setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
                    return;
                }
                else if (result == MessageUtil.YES) {
                    // User wants to save, so we save the note and allow the frame to close.
                    setDefaultCloseOperation(DISPOSE_ON_CLOSE);
                    if (!saveInternal(true)) {
                        // If the save failed (for example, due to a collision that the user canceled),
                        // we need to prevent the frame from closing.
                        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
                        return;
                    }
                }
            }
            else {
                // There are no unsaved changes, but if this is a scratch note, we need
                // to know if the user wants to keep it in the scratch directory, or
                // discard it entirely (delete it).
                if (dataManager.isScratchNote(note)) {
                    int result = getMessageUtil().askYesNo("Discard scratch note?",
                                                           "This is a scratch note. Do you want to discard it?");
                    if (result == MessageUtil.YES) {
                        dataManager.delete(note);
                    }
                }
            }

            // If we get here, the frame can just close:
            setDefaultCloseOperation(DISPOSE_ON_CLOSE);

            // Clean up our context viewer, if we have one.
            if (contextViewer != null) {
                contextViewer.dispose();
            }
        }
    }
}
