package ca.corbett.snotes.ui;

import ca.corbett.extras.MessageUtil;
import ca.corbett.extras.ScrollUtil;
import ca.corbett.extras.io.KeyStrokeManager;
import ca.corbett.forms.Alignment;
import ca.corbett.forms.FormPanel;
import ca.corbett.forms.fields.ShortTextField;
import ca.corbett.snotes.model.Note;
import ca.corbett.snotes.model.Query;
import ca.corbett.snotes.model.TagList;
import ca.corbett.snotes.model.filter.TagFilter;
import ca.corbett.snotes.model.filter.TextFilter;
import ca.corbett.snotes.ui.query.QueryFilterPanel;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import java.util.logging.Logger;

/**
 * A dialog for searching for notes. There are two search types:
 * <ul>
 *     <li><b>Simple search:</b> - enter text to find and/or tags to search for.</li>
 *     <li><b>Advanced search:</b> - includes the fields from simple search, and adds options
 *     for date ranges and other filter options.</li>
 * </ul>
 * <p>
 *     The dialog is very keyboard-friendly! Keyboard focus will be placed in the first
 *     text field when the dialog is opened. Type the text to search for and hit Enter,
 *     and the search will be performed. Hit ESC to cancel the search / close the dialog.
 * </p>
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 * @since Snotes 2.0
 */
public class SearchDialog extends JDialog {

    private static final Logger log = Logger.getLogger(SearchDialog.class.getName());
    private MessageUtil messageUtil;

    private enum Limit {
        ALL("Return all results", Integer.MAX_VALUE),
        RECENT5("5 most recent results", 5),
        RECENT10("10 most recent results", 10),
        RECENT20("20 most recent results", 20);

        private final int limit;
        private final String label;

        Limit(String label, int limit) {
            this.label = label;
            this.limit = limit;
        }

        public int getLimit() {
            return limit;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private final KeyStrokeManager keyManager;
    private JTabbedPane tabPane;
    private FormPanel simpleSearchForm;
    private ShortTextField simpleTextField;
    private ShortTextField simpleTagField;
    private JComboBox<Limit> limitComboBox;
    private QueryFilterPanel advancedSearchForm;

    /**
     * Creates a new SearchDialog with all empty fields.
     * We default to "simple" search type.
     */
    public SearchDialog() {
        super(MainWindow.getInstance(), "Search Notes", true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(550, 400);
        setMinimumSize(new Dimension(475, 260));
        setLocationRelativeTo(MainWindow.getInstance());
        initComponents();
        keyManager = new KeyStrokeManager(this);
        keyManager.registerHandler(KeyStrokeManager.parseKeyStroke("ESC"), e -> dispose());
        keyManager.registerHandler(KeyStrokeManager.parseKeyStroke("Enter"), e -> performSearch());
        addWindowListener(new WindowOpenListener());
    }

    @Override
    public void dispose() {
        keyManager.dispose();
        super.dispose();
    }

    private void performSearch() {
        Query transientQuery;
        int queryLimit = limitComboBox.getSelectedItem() == null
            ? Integer.MAX_VALUE
            : ((Limit)limitComboBox.getSelectedItem()).getLimit();
        if (tabPane.getSelectedComponent() == simpleSearchForm) {
            if (!simpleSearchForm.isFormValid()) {
                // Pointless, since there are no validators on our simple form.
                // We can't call setAllowBlank(false) on either field, because
                // it's valid to leave one blank and search for the other.
                // What we need is a validator that ensures that AT LEAST ONE
                // field has a value, but that doesn't exist.
                return;
            }
            // So, we validate manually:
            String textSearch = simpleTextField.getText();
            String tagSearch = simpleTagField.getText();
            if (textSearch.isBlank() && tagSearch.isBlank()) {
                getMessageUtil().error("Empty search", "Please enter some text or tags to search for.");
                return;
            }
            transientQuery = new Query();
            if (!textSearch.isBlank()) {
                transientQuery.addFilter(new TextFilter(textSearch));
            }
            if (!tagSearch.isBlank()) {
                TagList tagList = TagList.fromRawString(tagSearch);
                TagFilter tagFilter = new TagFilter(tagList.getTags(), TagFilter.FilterType.ALL);
                transientQuery.addFilter(tagFilter);
            }
        }

        else {
            if (!advancedSearchForm.isFormValid()) {
                return;
            }
            transientQuery = advancedSearchForm.getQuery();
        }

        List<Note> results = transientQuery.execute(MainWindow.getInstance().getDataManager().getNotes(), queryLimit);
        if (results.isEmpty()) {
            getMessageUtil().info("Nothing found", "No notes matched your search criteria.");
            return;
        }

        // There's at least one result, but let's make sure it's not a crazy number:
        if (results.size() > 100) {
            int result = getMessageUtil().askYesNo("Many results",
                                                   "Your search returned " + results.size()
                                                       + " results. Are you sure you want to view them all?");
            if (result != MessageUtil.YES) {
                return;
            }
        }

        ReaderFrame readerFrame = new ReaderFrame(results, transientQuery);
        MainWindow.getInstance().addInternalFrame(readerFrame);
        dispose();
    }

    private void initComponents() {
        tabPane = new JTabbedPane();
        tabPane.addTab("Simple Search", buildSimpleSearchPanel());
        tabPane.addTab("Advanced Search", ScrollUtil.buildScrollPane(buildAdvancedSearchPanel()));
        tabPane.addChangeListener(e -> {
            // When switching tabs, we want to give focus to the first text field in that tab.
            if (tabPane.getSelectedComponent() == simpleSearchForm) {
                SwingUtilities.invokeLater(() -> simpleTextField.getTextField().requestFocusInWindow());
            }
            else {
                SwingUtilities.invokeLater(() -> advancedSearchForm.requestKeyboardFocus());
            }
        });

        setLayout(new BorderLayout());
        add(tabPane, BorderLayout.CENTER);
        add(buildButtonPanel(), BorderLayout.SOUTH);
    }

    private JPanel buildSimpleSearchPanel() {
        simpleSearchForm = new FormPanel(Alignment.TOP_LEFT);
        simpleSearchForm.setBorderMargin(12);

        simpleTextField = new ShortTextField("Contains text:", 20);
        simpleTextField.setHelpText("Search will be case-insensitive.");
        simpleSearchForm.add(simpleTextField);

        simpleTagField = new ShortTextField("Has tag(s):", 20);
        simpleTagField.setHelpText("Comma or space-separated list of tags. Results must contain all given tags.");
        simpleSearchForm.add(simpleTagField);

        return simpleSearchForm;
    }

    private JPanel buildAdvancedSearchPanel() {
        advancedSearchForm = new QueryFilterPanel();

        return advancedSearchForm;
    }

    private JPanel buildButtonPanel() {
        JPanel buttonPanel = new JPanel(new BorderLayout());

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btn = new JButton("Search");
        btn.addActionListener(e -> performSearch());
        btn.setPreferredSize(new Dimension(110, 24));
        rightPanel.add(btn);
        btn = new JButton("Cancel");
        btn.addActionListener(e -> dispose());
        btn.setPreferredSize(new Dimension(110, 24));
        rightPanel.add(btn);

        // We'll put the limit combo box down here on the button panel,
        // because it applies equally to both of our tab panes, and I don't
        // want two combos for this. This is slightly non-standard UI design,
        // but eh, it works, and it keeps our tabs from getting cluttered.
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        limitComboBox = new JComboBox<>(Limit.values());
        limitComboBox.setEditable(false);
        limitComboBox.setSelectedItem(Limit.ALL);
        leftPanel.add(limitComboBox);

        buttonPanel.add(leftPanel, BorderLayout.CENTER);
        buttonPanel.add(rightPanel, BorderLayout.EAST);
        buttonPanel.setBorder(BorderFactory.createRaisedBevelBorder());
        return buttonPanel;
    }

    private MessageUtil getMessageUtil() {
        if (messageUtil == null) {
            messageUtil = new MessageUtil(this, log);
        }
        return messageUtil;
    }

    private class WindowOpenListener extends WindowAdapter {
        @Override
        public void windowOpened(WindowEvent e) {
            // Try to give focus to the text field in the simple search tab when the dialog is shown,
            // since that's likely where the user will want to start.
            // Unfortunately, this isn't guaranteed to work - all we can do is request it.
            simpleTextField.getTextField().requestFocusInWindow();
        }
    }
}
