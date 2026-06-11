package ca.corbett.snotes.ui;

import ca.corbett.extras.MessageUtil;
import ca.corbett.extras.ScrollUtil;
import ca.corbett.snotes.io.DataManager;
import ca.corbett.snotes.model.Note;
import ca.corbett.snotes.model.Query;
import ca.corbett.snotes.model.TagList;
import ca.corbett.snotes.model.TagUsage;
import ca.corbett.snotes.model.filter.TagFilter;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.logging.Logger;

/**
 * Shows a table of all unique non-date tags that are used at least once by any non-scratch Note.
 * Each row displays the tag name and the number of notes using that tag. The table supports
 * sorting by clicking column headers. Double-clicking a row or selecting multiple rows and clicking
 * the search button launches a search for the selected tag(s).
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 * @since Snotes 2.2
 */
public class TagListDialog extends JDialog {

    private static final Logger log = Logger.getLogger(TagListDialog.class.getName());
    private final DataManager dataManager;
    private MessageUtil messageUtil;
    private final JTable tagTable;
    private final TagUsageTableModel tableModel;

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
        tableModel = new TagUsageTableModel(dataManager.getUniqueTags());
        tagTable = new JTable(tableModel);
        tagTable.setAutoCreateRowSorter(true);
        tagTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        tagTable.getColumnModel().getColumn(0).setPreferredWidth(200);
        tagTable.getColumnModel().getColumn(1).setPreferredWidth(80);
        tagTable.getColumnModel().getColumn(1).setCellRenderer(new RightAlignedIntegerRenderer());
        setSize(300, 500);
        setResizable(true);
        setLayout(new BorderLayout());
        add(ScrollUtil.buildScrollPane(tagTable), BorderLayout.CENTER);
        add(buildButtonPanel(), BorderLayout.SOUTH);
        setLocationRelativeTo(owner);

        // Double-clicking a row in the tag table searches for that tag.
        // Double-clicking on blank space should do nothing.
        tagTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    int viewRow = tagTable.getSelectedRow();
                    if (viewRow >= 0) {
                        searchSelected();
                    }
                }
            }
        });
    }

    private void searchSelected() {
        int[] selectedViewRows = tagTable.getSelectedRows();
        if (selectedViewRows.length == 0) {
            return;
        }
        TagList toSearch = new TagList();
        for (int viewRow : selectedViewRows) {
            int modelRow = tagTable.convertRowIndexToModel(viewRow);
            TagUsage usage = tableModel.getTagUsage(modelRow);
            toSearch.addTag(usage.tag().getTag());
        }
        Query query = new Query();
        query.addFilter(new TagFilter(toSearch.getTags(), TagFilter.FilterType.ALL));
        List<Note> results = query.execute(dataManager.getNotes());
        if (results.isEmpty()) {
            // Each tag in our list is guaranteed to be used with at least one note.
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

    // -----------------------------------------------------------------------
    // Table model
    // -----------------------------------------------------------------------

    private static final int TAG_COLUMN = 0;
    private static final int COUNT_COLUMN = 1;

    private class TagUsageTableModel extends AbstractTableModel {

        private final List<TagUsage> data;

        TagUsageTableModel(List<TagUsage> data) {
            this.data = data;
        }

        @Override
        public int getRowCount() {
            return data.size();
        }

        @Override
        public int getColumnCount() {
            return 2;
        }

        @Override
        public String getColumnName(int column) {
            return switch (column) {
                case TAG_COLUMN -> "Tag";
                case COUNT_COLUMN -> "Notes";
                default -> "";
            };
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return switch (columnIndex) {
                case COUNT_COLUMN -> Integer.class;
                default -> String.class;
            };
        }

        @Override
        public Object getValueAt(int row, int column) {
            TagUsage usage = data.get(row);
            return switch (column) {
                case TAG_COLUMN -> usage.tag().getTag();
                case COUNT_COLUMN -> usage.count();
                default -> null;
            };
        }

        TagUsage getTagUsage(int row) {
            return data.get(row);
        }
    }

    // -----------------------------------------------------------------------
    // Cell renderer
    // -----------------------------------------------------------------------

    private static class RightAlignedIntegerRenderer extends DefaultTableCellRenderer {

        private RightAlignedIntegerRenderer() {
            setHorizontalAlignment(JLabel.RIGHT);
        }
    }
}
