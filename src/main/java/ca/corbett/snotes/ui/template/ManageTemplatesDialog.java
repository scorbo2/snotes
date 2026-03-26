package ca.corbett.snotes.ui.template;

import ca.corbett.extras.MessageUtil;
import ca.corbett.extras.io.KeyStrokeManager;
import ca.corbett.forms.Alignment;
import ca.corbett.forms.FormPanel;
import ca.corbett.forms.fields.ListField;
import ca.corbett.snotes.io.DataManager;
import ca.corbett.snotes.model.Template;
import ca.corbett.snotes.ui.MainWindow;
import ca.corbett.snotes.ui.actions.UIReloadAction;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * Displays a list of all saved Templates, and allows the user to edit or delete them.
 * You can also create a new Template from this dialog.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 * @since Snotes 2.0
 */
public class ManageTemplatesDialog extends JDialog {

    private static final Logger log = Logger.getLogger(ManageTemplatesDialog.class.getName());
    private MessageUtil messageUtil;

    private final KeyStrokeManager keyManager;
    private final DataManager dataManager;
    private ListField<Template> templateListField;

    public ManageTemplatesDialog() {
        super(MainWindow.getInstance(), "Manage Templates", true);
        setSize(500, 320);
        setResizable(false);
        setLocationRelativeTo(MainWindow.getInstance());
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        this.dataManager = MainWindow.getInstance().getDataManager();
        keyManager = new KeyStrokeManager(this);
        keyManager.registerHandler(KeyStrokeManager.parseKeyStroke("ESC"), e -> closeDialog());
        setLayout(new BorderLayout());
        add(buildFormPanel(), BorderLayout.CENTER);
        add(buildButtonPanel(), BorderLayout.SOUTH);
    }

    public void closeDialog() {
        keyManager.dispose();
        setVisible(false);
        dispose();
    }

    /**
     * Forces a refresh of the list of Templates displayed here.
     */
    private void refresh() {
        templateListField.getListModel().clear();
        templateListField.getListModel().addAll(dataManager.getTemplates());
        templateListField.getList().revalidate();
        templateListField.getList().repaint();
    }

    private JPanel buildFormPanel() {
        FormPanel formPanel = new FormPanel(Alignment.TOP_LEFT);
        formPanel.setBorderMargin(12);

        templateListField = new ListField<>("Saved Templates", dataManager.getTemplates());
        templateListField.setVisibleRowCount(8);
        templateListField.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        templateListField.setShouldExpand(true);
        templateListField.setButtonPosition(ListField.ButtonPosition.BOTTOM);
        templateListField.setButtonLayout(FlowLayout.LEFT, 6, 4);
        templateListField.addButton(new AddTemplateAction());
        templateListField.addButton(new EditTemplateAction());
        templateListField.addButton(new DeleteTemplateAction());
        formPanel.add(templateListField);

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
     * An internal action to show the TemplateBuilderDialog to create a new Template.
     * After the dialog is closed, if a new Template was created, it will be saved
     * and the list of Templates will be refreshed.
     */
    private class AddTemplateAction extends AbstractAction {
        public AddTemplateAction() {
            super("New");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            TemplateBuilderDialog dialog = new TemplateBuilderDialog(ManageTemplatesDialog.this);
            dialog.setVisible(true);
            if (dialog.wasOkayed()) {
                try {
                    dataManager.saveTemplate(dialog.getTemplate());

                    // Trigger a reload so the new template shows up in the ActionPanel:
                    UIReloadAction.getInstance().actionPerformed(null);

                    // Also refresh our own list:
                    refresh();
                }
                catch (IOException ioe) {
                    getMessageUtil().error("Save error",
                                           "An error occurred while saving the new template: " + ioe.getMessage(),
                                           ioe);
                }
            }
        }
    }

    /**
     * An internal action to show the TemplateBuilderDialog to edit an existing Template.
     * After the dialog is closed, if the Template was updated, it will be saved
     * and the list of Templates will be refreshed.
     */
    private class EditTemplateAction extends AbstractAction {
        public EditTemplateAction() {
            super("Edit");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Template templateToEdit = templateListField.getList().getSelectedValue();
            if (templateToEdit == null) {
                getMessageUtil().info("No template selected", "Please select a template to edit.");
                return;
            }

            TemplateBuilderDialog dialog = new TemplateBuilderDialog(ManageTemplatesDialog.this, templateToEdit);
            dialog.setVisible(true);
            if (dialog.wasOkayed()) {
                try {
                    dataManager.saveTemplate(dialog.getTemplate());

                    // Trigger a reload so the updated template shows up in the ActionPanel:
                    UIReloadAction.getInstance().actionPerformed(null);

                    // Also refresh our own list:
                    refresh();
                }
                catch (IOException ioe) {
                    getMessageUtil().error("Save error",
                                           "An error occurred while saving the updated template: " + ioe.getMessage(),
                                           ioe);
                }
            }
        }
    }

    /**
     * An internal action to delete the selected Template.
     * After confirming with the user, the Template will be deleted and the list of Templates will be refreshed.
     */
    private class DeleteTemplateAction extends AbstractAction {
        public DeleteTemplateAction() {
            super("Delete");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Template templateToDelete = templateListField.getList().getSelectedValue();
            if (templateToDelete == null) {
                getMessageUtil().info("No template selected", "Please select a template to delete.");
                return;
            }

            int confirm = getMessageUtil().askYesNo("Confirm delete",
                                                    "Are you sure you want to delete the template \"" + templateToDelete.getName() + "\"?");
            if (confirm == MessageUtil.YES) {
                dataManager.delete(templateToDelete);

                // Trigger a reload so the deleted template is removed from the ActionPanel:
                UIReloadAction.getInstance().actionPerformed(null);

                // Also refresh our own list:
                refresh();
            }
        }
    }

    private MessageUtil getMessageUtil() {
        if (messageUtil == null) {
            messageUtil = new MessageUtil(this, log);
        }
        return messageUtil;
    }
}
