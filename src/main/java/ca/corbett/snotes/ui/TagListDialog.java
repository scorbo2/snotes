package ca.corbett.snotes.ui;

import ca.corbett.extras.MessageUtil;
import ca.corbett.extras.ScrollUtil;
import ca.corbett.snotes.io.DataManager;
import ca.corbett.snotes.model.Note;
import ca.corbett.snotes.model.Query;
import ca.corbett.snotes.model.TagList;
import ca.corbett.snotes.model.filter.TagFilter;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.logging.Logger;

/**
 * Shows a list of all unique non-date tags that are used at least once by any non-scratch Note.
 * The list offers options for launching a search for selected tags, or double-clicking a single
 * tag for a single-tag search.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 * @since Snotes 2.2
 */
public class TagListDialog extends JDialog {

    private static final Logger log = Logger.getLogger(TagListDialog.class.getName());
    private final DataManager dataManager;
    private MessageUtil messageUtil;
    private final JList<String> tagList;

    /**
     * Creates a TagListDialog with the given parent and data manager.
     * The tag list will be retrieved and populated immediately.
     * The dialog will be modal to the given parent window.
     *
     * @param owner the parent window for this dialog
     * @param dataManager the data manager to retrieve tags from
     */
    public TagListDialog(Window owner, DataManager dataManager) {
        super(owner, "Tag List", ModalityType.APPLICATION_MODAL);
        this.dataManager = dataManager;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        DefaultListModel<String> listModel = new DefaultListModel<>();
        listModel.addAll(dataManager.getUniqueTags().stream().map(t -> t.getTag()).toList());
        tagList = new JList<>(listModel);
        setSize(300, 500);
        setResizable(true);
        setLayout(new BorderLayout());
        add(ScrollUtil.buildScrollPane(tagList), BorderLayout.CENTER);
        add(buildButtonPanel(), BorderLayout.SOUTH);
        setLocationRelativeTo(owner);

        // Double-clicking an item in the tag list searches for that item.
        // Double-clicking on the blank space in the list should do nothing.
        tagList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    String selectedTag = tagList.getSelectedValue();
                    if (selectedTag != null) {
                        searchSelected();
                    }
                }
            }
        });
    }

    private void searchSelected() {
        int[] selectedIndexes = tagList.getSelectedIndices();
        if (selectedIndexes.length == 0) {
            return;
        }
        TagList toSearch = new TagList();
        for (int selectedIndex : selectedIndexes) {
            toSearch.addTag(tagList.getModel().getElementAt(selectedIndex));
        }
        Query query = new Query();
        query.addFilter(new TagFilter(toSearch.getTags(), TagFilter.FilterType.ALL));
        List<Note> results = query.execute(dataManager.getNotes());
        if (results.isEmpty()) {
            // Each note in our list is guaranteed to be used with at least one note.
            // But, if the user selected more than one tag, there's no guarantee
            // that our search in mode ALL will return anything.
            getMessageUtil().info("There are no notes with the selected tags.");
            return; // leave this dialog open so they can try again.
        }

        ReaderFrame readerFrame = new ReaderFrame(results, query);
        MainWindow.getInstance().addInternalFrame(readerFrame);
        dispose(); // debatable, but we can assume the user is done with this dialog.
    }

    private JPanel buildButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton button = new JButton("Search selected tags");
        button.addActionListener(e -> searchSelected());
        panel.add(button);
        return panel;
    }

    private MessageUtil getMessageUtil() {
        if (messageUtil == null) {
            messageUtil = new MessageUtil(this, log);
        }
        return messageUtil;
    }
}
