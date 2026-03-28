package ca.corbett.snotes.ui.query;

import ca.corbett.extras.MessageUtil;
import ca.corbett.extras.io.KeyStrokeManager;
import ca.corbett.forms.Alignment;
import ca.corbett.forms.FormPanel;
import ca.corbett.forms.fields.LabelField;
import ca.corbett.forms.fields.PanelField;
import ca.corbett.forms.fields.ShortTextField;
import ca.corbett.snotes.io.DataManager;
import ca.corbett.snotes.model.Query;
import ca.corbett.snotes.ui.MainWindow;
import ca.corbett.snotes.ui.UniqueNameValidator;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.util.logging.Logger;

/**
 * A Dialog for viewing, editing, or creating a Query.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 * @since Snotes 2.0
 */
public class QueryBuilderDialog extends JDialog {

    private static final Logger log = Logger.getLogger(QueryBuilderDialog.class.getName());

    private final Query queryToEdit; // null if we're creating a new Query
    private final KeyStrokeManager keyManager;
    private FormPanel formPanel;
    private QueryFilterPanel queryFilterPanel;
    private ShortTextField nameField;
    private MessageUtil messageUtil;
    private boolean wasOkayed;

    /**
     * Creates a QueryBuilderDialog suitable for creating a new Query.
     * All fields will start with blank values, and the user can
     * populate them to create a new Query.
     *
     * @param owner the parent window for this dialog
     */
    public QueryBuilderDialog(Window owner) {
        this(owner, null);
    }

    /**
     * Creates a QueryBuilderDialog for editing an existing Query. The fields will
     * be pre-populated with the values from the given Query, and the user can edit
     * them to modify the Query.
     *
     * @param owner the parent window for this dialog
     * @param query the Query to edit. This dialog will be pre-populated with the values from this Query.
     */
    public QueryBuilderDialog(Window owner, Query query) {
        super(owner, (query == null) ? "New Query" : "Edit Query: " + query.getName(), ModalityType.APPLICATION_MODAL);
        setSize(new Dimension(580, 480));
        setResizable(false);
        setLocationRelativeTo((owner != null) ? owner : MainWindow.getInstance());
        this.queryToEdit = query;
        keyManager = new KeyStrokeManager(this);
        queryFilterPanel = new QueryFilterPanel(queryToEdit);
        initKeyBindings();
        initComponents();
        wasOkayed = false;
    }

    /**
     * Returns true if the user clicked "OK" to close this dialog.
     * If this is true, the form has been validated, and calling
     * getQuery() is guaranteed to return a non-null Query with the values the user entered.
     */
    public boolean wasOkayed() {
        return wasOkayed;
    }

    @Override
    public void dispose() {
        keyManager.dispose();
        super.dispose();
    }

    /**
     * Check wasOkayed() first! If the user canceled or closed the dialog,
     * this method will return null! If wasOkayed() returns true, then
     * this method will return a non-null Query instance with the values
     * that the user entered. If we were editing an existing Query, this
     * method is guaranteed to return the same instance that was passed in,
     * but with the values updated as needed.
     */
    public Query getQuery() {
        if (!wasOkayed) {
            return null;
        }

        Query query = queryFilterPanel.getQuery();
        query.setName(nameField.getText()); // User may have renamed it.
        return query;
    }

    private void buttonHandler(boolean isOkay) {
        if (isOkay) {
            boolean isNameOkay = formPanel.isFormValid();
            boolean areFiltersOkay = queryFilterPanel.isFormValid();
            if (!isNameOkay || !areFiltersOkay) {
                // isFormValid() will display validation errors inline as a side effect.
                // So, the user can see what's wrong and can fix it before trying again.
                // We return here so that the dialog stays open until the form is good or the user cancels.
                return;
            }
            wasOkayed = true;
        }

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
        String existingName = (queryToEdit == null) ? null : queryToEdit.getName();
        nameField.setText((queryToEdit == null) ? Query.DEFAULT_NAME : existingName);
        nameField.setAllowBlank(false);
        DataManager dataManager = MainWindow.getInstance().getDataManager();
        nameField.addFieldValidator(new UniqueNameValidator(dataManager::isQueryNameAvailable,
                                                            Query.NAME_LENGTH_LIMIT,
                                                            existingName));
        formPanel.add(nameField);

        PanelField panelField = new PanelField(new BorderLayout());
        panelField.getPanel().add(queryFilterPanel, BorderLayout.CENTER);
        panelField.setShouldExpand(true);
        formPanel.add(panelField);

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
}
