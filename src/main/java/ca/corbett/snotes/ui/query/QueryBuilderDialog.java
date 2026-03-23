package ca.corbett.snotes.ui.query;

import ca.corbett.extras.MessageUtil;
import ca.corbett.extras.io.KeyStrokeManager;
import ca.corbett.forms.Alignment;
import ca.corbett.forms.FormPanel;
import ca.corbett.forms.fields.ButtonField;
import ca.corbett.forms.fields.LabelField;
import ca.corbett.forms.fields.ShortTextField;
import ca.corbett.snotes.model.Query;
import ca.corbett.snotes.model.filter.Filter;
import ca.corbett.snotes.ui.MainWindow;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * A Dialog for viewing, editing, or creating a Query.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 * @since Snotes 2.0
 */
public class QueryBuilderDialog extends JDialog {

    private static final Logger log = Logger.getLogger(QueryBuilderDialog.class.getName());

    /**
     * An arbitrary limit on the number of filters you can define
     * for a single Query. The Query class itself doesn't actually
     * have a limit, but the UI gets clunky if we allow limitless filters.
     */
    private static final int MAX_FILTERS = 8;

    private final List<QueryFilterField> filterFields;
    private final KeyStrokeManager keyManager;
    private FormPanel formPanel;
    private ShortTextField nameField;
    private int filterCount = 1;
    private MessageUtil messageUtil;
    private boolean wasOkayed;

    public QueryBuilderDialog(String title) {
        super(MainWindow.getInstance(), title, true);
        setSize(new Dimension(580, 480));
        setResizable(false);
        setLocationRelativeTo(MainWindow.getInstance());
        keyManager = new KeyStrokeManager(this);
        filterFields = new ArrayList<>(MAX_FILTERS);
        for (int i = 0; i < MAX_FILTERS; i++) {
            filterFields.add(new QueryFilterField());
        }
        initKeyBindings();
        initComponents();
        wasOkayed = false;
    }

    public boolean wasOkayed() {
        return wasOkayed;
    }

    public Query getQuery() {
        if (!wasOkayed) {
            return null;
        }

        List<Filter> filters = new ArrayList<>(filterCount);
        for (int i = 0; i < filterCount; i++) {
            filters.add(filterFields.get(i).getFilter());
        }
        Query query = new Query();
        query.setName(nameField.getText());
        for (Filter filter : filters) {
            query.addFilter(filter);
        }
        return query;
    }

    private void buttonHandler(boolean isOkay) {
        if (isOkay) {
            if (!formPanel.isFormValid()) {
                return; // dialog stays open until form is valid or user cancels
            }
            wasOkayed = true;
        }

        keyManager.dispose();
        dispose();
    }

    private void initKeyBindings() {
        keyManager.registerHandler(KeyStrokeManager.parseKeyStroke("ESC"), e -> buttonHandler(false));
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        add(buildFormPanel(), BorderLayout.CENTER);
        add(buildButtonPanel(), BorderLayout.SOUTH);
    }

    private JPanel buildFormPanel() {
        formPanel = new FormPanel(Alignment.TOP_LEFT);
        formPanel.setBorderMargin(8);
        formPanel.add(LabelField.createBoldHeaderLabel("Query Filters"));

        nameField = new ShortTextField("Name:", 15);
        nameField.setText(Query.DEFAULT_NAME);
        nameField.setAllowBlank(false);
        // TODO put a validator on here to ensure the chosen name is unique. Prevents problems on save.
        //      Requires changes in DataManager to check for duplicate names.
        formPanel.add(nameField);

        formPanel.add(filterFields.get(0)); // Default label is "Filter:"
        for (int i = 1; i < filterFields.size(); i++) {
            QueryFilterField filterField = filterFields.get(i);
            filterField.getFieldLabel().setText("AND:"); // only supported option currently, so make it the label.
            filterField.setVisible(false); // we'll show them as the user hits "add filter"
            formPanel.add(filterField);
        }
        ButtonField buttonField = new ButtonField();
        buttonField.addButton(new AddFilterAction());
        buttonField.addButton(new RemoveFilterAction());
        formPanel.add(buttonField);

        return formPanel;
    }

    private JPanel buildButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton button = new JButton("OK");
        button.addActionListener(e -> buttonHandler(true));
        button.setPreferredSize(new Dimension(100, 24));
        panel.add(button);
        button = new JButton("Cancel");
        button.addActionListener(e -> buttonHandler(false));
        button.setPreferredSize(new Dimension(100, 24));
        panel.add(button);
        return panel;
    }

    private MessageUtil getMessageUtil() {
        if (messageUtil == null) {
            messageUtil = new MessageUtil(this, log);
        }
        return messageUtil;
    }

    private class AddFilterAction extends AbstractAction {

        public AddFilterAction() {
            super("Add Filter");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (filterCount >= MAX_FILTERS) {
                getMessageUtil().info("Maximum filters reached",
                                      "You can only have " + MAX_FILTERS + " filters in a query.");
                return;
            }
            filterFields.get(filterCount++).setVisible(true);
        }
    }

    private class RemoveFilterAction extends AbstractAction {

        public RemoveFilterAction() {
            super("Remove Filter");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (filterCount <= 1) {
                getMessageUtil().info("Minimum filters reached",
                                      "A query must have at least one filter.");
                return;
            }
            filterFields.get(--filterCount).setVisible(false);
        }
    }
}
