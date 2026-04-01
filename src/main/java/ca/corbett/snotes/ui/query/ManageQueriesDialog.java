package ca.corbett.snotes.ui.query;

import ca.corbett.extras.EnhancedAction;
import ca.corbett.extras.MessageUtil;
import ca.corbett.extras.io.KeyStrokeManager;
import ca.corbett.forms.Alignment;
import ca.corbett.forms.FormPanel;
import ca.corbett.forms.SwingFormsResources;
import ca.corbett.forms.actions.ListItemMoveAction;
import ca.corbett.forms.fields.ListField;
import ca.corbett.snotes.io.DataManager;
import ca.corbett.snotes.model.Query;
import ca.corbett.snotes.ui.MainWindow;
import ca.corbett.snotes.ui.actions.UIReloadAction;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * Displays a list of all saved Queries, and allows the user to edit or delete them.
 * You can also create a new Query from this dialog.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 * @since Snotes 2.0
 */
public class ManageQueriesDialog extends JDialog {

    private static final Logger log = Logger.getLogger(ManageQueriesDialog.class.getName());
    private MessageUtil messageUtil;

    private final ListListener listListener;
    private ListItemMoveAction<Query> moveUpAction;
    private ListItemMoveAction<Query> moveDownAction;

    private final KeyStrokeManager keyManager;
    private final DataManager dataManager;
    private ListField<Query> queryListField;
    private boolean listReordered;

    public ManageQueriesDialog() {
        super(MainWindow.getInstance(), "Manage Queries", true);
        setSize(500, 320);
        setResizable(false);
        setLocationRelativeTo(MainWindow.getInstance());
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        this.listListener = new ListListener();
        this.listReordered = false;
        this.dataManager = MainWindow.getInstance().getDataManager();
        keyManager = new KeyStrokeManager(this);
        keyManager.registerHandler(KeyStrokeManager.parseKeyStroke("ESC"), e -> closeDialog());
        keyManager.registerHandler(KeyStrokeManager.parseKeyStroke("Ctrl+up"),
                                   e -> moveUpAction.actionPerformed(e));
        keyManager.registerHandler(KeyStrokeManager.parseKeyStroke("Ctrl+down"),
                                   e -> moveDownAction.actionPerformed(e));
        setLayout(new BorderLayout());
        add(buildFormPanel(), BorderLayout.CENTER);
        add(buildButtonPanel(), BorderLayout.SOUTH);
    }

    public void closeDialog() {
        keyManager.dispose();
        setVisible(false);
        dispose();
        
        if (listReordered) {
            // Trigger a UI reload so the list of queries in the ActionPanel are updated:
            UIReloadAction.getInstance().actionPerformed(null);
        }
    }

    /**
     * Forces a refresh of the list of Queries displayed here.
     */
    private void refresh() {
        queryListField.removeListDataListener(listListener);
        try {
            queryListField.getListModel().clear();
            queryListField.getListModel().addAll(dataManager.getQueries());
            queryListField.getList().revalidate();
            queryListField.getList().repaint();
        }
        finally {
            queryListField.addListDataListener(listListener);
        }
    }

    private JPanel buildFormPanel() {
        FormPanel formPanel = new FormPanel(Alignment.TOP_LEFT);
        formPanel.setBorderMargin(12);

        queryListField = new ListField<>("Saved Queries", dataManager.getQueries());
        queryListField.setVisibleRowCount(8);
        queryListField.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        queryListField.setShouldExpand(true);
        queryListField.setButtonPosition(ListField.ButtonPosition.BOTTOM);
        queryListField.setButtonLayout(FlowLayout.CENTER, 2, 2);
        queryListField.setButtonPanelBorder(BorderFactory.createLoweredBevelBorder());
        queryListField.addButton(new AddQueryAction());
        queryListField.addButton(new EditQueryAction());
        queryListField.addButton(new DeleteQueryAction());
        this.moveUpAction = new ListItemMoveAction<>(SwingFormsResources.getMoveUpIcon(16), queryListField,
                                                     ListItemMoveAction.Direction.UP);
        moveUpAction.setTooltip(moveUpAction.getTooltip() + " (Ctrl+Up)");
        this.moveDownAction = new ListItemMoveAction<>(SwingFormsResources.getMoveDownIcon(16), queryListField,
                                                       ListItemMoveAction.Direction.DOWN);
        moveDownAction.setTooltip(moveDownAction.getTooltip() + " (Ctrl+Down)");
        queryListField.addButton(moveUpAction);
        queryListField.addButton(moveDownAction);
        queryListField.addListDataListener(new ListListener());

        formPanel.add(queryListField);

        return formPanel;
    }

    private JPanel buildButtonPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton newButton = new JButton("Close");
        newButton.setPreferredSize(new Dimension(110, 24));
        newButton.addActionListener(e -> closeDialog());
        buttonPanel.add(newButton);
        return buttonPanel;
    }

    /**
     * An internal action to show the QueryBuilderDialog to create a new Query.
     * After the dialog is closed, if a new Query was created, it will be saved
     * and the list of Queries will be refreshed.
     */
    private class AddQueryAction extends EnhancedAction {
        public AddQueryAction() {
            super("", SwingFormsResources.getAddIcon(16));
            setTooltip("Create a new query");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            QueryBuilderDialog dialog = new QueryBuilderDialog(ManageQueriesDialog.this);
            dialog.setVisible(true);
            if (dialog.wasOkayed()) {
                try {
                    dataManager.saveQuery(dialog.getQuery());

                    // Trigger a reload so the new query shows up in the ActionPanel:
                    UIReloadAction.getInstance().actionPerformed(null);

                    // Also refresh our own list:
                    refresh();
                }
                catch (IOException ioe) {
                    getMessageUtil().error("Save error",
                                           "An error occurred while saving the new query: " + ioe.getMessage(),
                                           ioe);
                }
            }
        }
    }

    /**
     * An internal action to show the QueryBuilderDialog to edit an existing Query.
     * After the dialog is closed, if the Query was updated, it will be saved
     * and the list of Queries will be refreshed.
     */
    private class EditQueryAction extends EnhancedAction {
        public EditQueryAction() {
            super("", SwingFormsResources.getEditIcon(16));
            setTooltip("Edit the selected query");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Query queryToEdit = queryListField.getList().getSelectedValue();
            if (queryToEdit == null) {
                getMessageUtil().info("No query selected", "Please select a query to edit.");
                return;
            }

            QueryBuilderDialog dialog = new QueryBuilderDialog(ManageQueriesDialog.this, queryToEdit);
            dialog.setVisible(true);
            if (dialog.wasOkayed()) {
                try {
                    dataManager.saveQuery(dialog.getQuery());

                    // Trigger a reload so the updated query shows up in the ActionPanel:
                    UIReloadAction.getInstance().actionPerformed(null);

                    // Also refresh our own list:
                    refresh();
                }
                catch (IOException ioe) {
                    getMessageUtil().error("Save error",
                                           "An error occurred while saving the updated query: " + ioe.getMessage(),
                                           ioe);
                }
            }
        }
    }

    /**
     * An internal action to delete the selected Query.
     * After confirming with the user, the Query will be deleted and the list of Queries will be refreshed.
     */
    private class DeleteQueryAction extends EnhancedAction {
        public DeleteQueryAction() {
            super("", SwingFormsResources.getRemoveIcon(16));
            setTooltip("Delete the selected query");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Query queryToDelete = queryListField.getList().getSelectedValue();
            if (queryToDelete == null) {
                getMessageUtil().info("No query selected", "Please select a query to delete.");
                return;
            }

            int confirm = getMessageUtil().askYesNo("Confirm delete",
                                                    "Are you sure you want to delete the query \"" + queryToDelete.getName() + "\"?");
            if (confirm == MessageUtil.YES) {
                dataManager.delete(queryToDelete);

                // Trigger a reload so the deleted query is removed from the ActionPanel:
                UIReloadAction.getInstance().actionPerformed(null);

                // Also refresh our own list:
                refresh();
            }
        }
    }

    /**
     * Listens for list re-ordering events, so we can update our Queries accordingly.
     * Note that we do this against the model objects directly - this dialog does not
     * have an OK/Cancel setup. Any changes you make here are live.
     */
    private class ListListener implements ListDataListener {

        @Override
        public void intervalAdded(ListDataEvent e) {
            boolean isDirty = false;
            for (int i = 0; i < queryListField.getListModel().size(); i++) {
                Query query = queryListField.getListModel().get(i);
                query.setOrder(i);
                isDirty = isDirty || query.isDirty();
            }

            // If any query is dirty, save 'em all:
            if (isDirty) {
                try {
                    for (int i = 0; i < queryListField.getListModel().size(); i++) {
                        dataManager.saveQuery(queryListField.getListModel().get(i));
                    }

                    // Make a note that we did this so we can reload the UI when the dialog closes:
                    listReordered = true;
                }
                catch (IOException ioe) {
                    getMessageUtil().error("Save error",
                                           "An error occurred while saving the updated query order: " + ioe.getMessage(),
                                           ioe);
                }
            }
        }

        @Override
        public void intervalRemoved(ListDataEvent e) {
            // ignored
        }

        @Override
        public void contentsChanged(ListDataEvent e) {
            // ignored
        }
    }

    private MessageUtil getMessageUtil() {
        if (messageUtil == null) {
            messageUtil = new MessageUtil(this, log);
        }
        return messageUtil;
    }
}
