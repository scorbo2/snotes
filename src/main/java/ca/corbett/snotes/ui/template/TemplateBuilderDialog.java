package ca.corbett.snotes.ui.template;

import ca.corbett.extras.MessageUtil;
import ca.corbett.extras.io.KeyStrokeManager;
import ca.corbett.forms.Alignment;
import ca.corbett.forms.FormPanel;
import ca.corbett.forms.fields.ComboField;
import ca.corbett.forms.fields.LabelField;
import ca.corbett.forms.fields.ShortTextField;
import ca.corbett.snotes.io.DataManager;
import ca.corbett.snotes.model.Query;
import ca.corbett.snotes.model.Tag;
import ca.corbett.snotes.model.TagList;
import ca.corbett.snotes.model.Template;
import ca.corbett.snotes.ui.MainWindow;
import ca.corbett.snotes.ui.UniqueNameValidator;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.util.Arrays;
import java.util.logging.Logger;

/**
 * A dialog for creating, viewing, or editing a Template.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 * @since Snotes 2.0
 */
public class TemplateBuilderDialog extends JDialog {
    private static final Logger log = Logger.getLogger(TemplateBuilderDialog.class.getName());

    private final Template templateToEdit; // null if we're creating a new Template
    private boolean wasOkayed;
    private final KeyStrokeManager keyManager;
    private FormPanel formPanel;
    private ShortTextField nameField;
    private ComboField<Template.DateOption> dateOptionField;
    private ComboField<Template.Context> contextField;
    private ShortTextField tagField;
    private MessageUtil messageUtil;

    public TemplateBuilderDialog(Window owner) {
        this(owner, null);
    }

    public TemplateBuilderDialog(Window owner, Template templateToEdit) {
        super(owner,
              templateToEdit == null ? "New Template" : "Edit Template: " + templateToEdit.getName(),
              ModalityType.APPLICATION_MODAL);
        setSize(new Dimension(500, 310));
        setResizable(false);
        setLocationRelativeTo((owner != null) ? owner : MainWindow.getInstance());
        this.templateToEdit = templateToEdit;
        this.keyManager = new KeyStrokeManager(this);
        initKeyBindings();
        initComponents();
        wasOkayed = false;
    }

    /**
     * Returns true if the user clicked "OK" to close this dialog.
     * If this is true, the form has been validated, and calling
     * getTemplate() is guaranteed to return a non-null Template with the values the user entered.
     */
    public boolean wasOkayed() {
        return wasOkayed;
    }

    /**
     * Check wasOkayed() first! If the user canceled or closed the dialog,
     * this method will return null! If wasOkayed() returns true, then
     * this method will return a non-null Template instance with the values
     * that the user entered. If we were editing an existing Template, this
     * method is guaranteed to return the same instance that was passed in,
     * but with the values updated as needed.
     */
    public Template getTemplate() {
        if (!wasOkayed) {
            return null;
        }

        // We're either creating a new one or editing an existing one:
        Template template = templateToEdit == null ? new Template() : templateToEdit;

        // Set all properties and return it:
        template.setName(nameField.getText());
        template.setDateOption(dateOptionField.getSelectedItem());
        template.setContext(contextField.getSelectedItem());
        TagList tagList = TagList.fromRawString(tagField.getText());
        for (Tag tag : tagList.getTags()) {
            template.addTag(tag.getTag());
        }
        return template;
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
        formPanel.setBorderMargin(12);
        formPanel.add(LabelField.createBoldHeaderLabel("Template Setup"));

        nameField = new ShortTextField("Name:", 15);
        String existingName = (templateToEdit == null) ? null : templateToEdit.getName();
        nameField.setText((templateToEdit == null) ? Template.DEFAULT_NAME : existingName);
        nameField.setAllowBlank(false);
        DataManager dataManager = MainWindow.getInstance().getDataManager();
        nameField.addFieldValidator(new UniqueNameValidator(dataManager::isTemplateNameAvailable,
                                                            Query.NAME_LENGTH_LIMIT,
                                                            existingName));
        formPanel.add(nameField);

        dateOptionField = new ComboField<>("Date Option:", Arrays.asList(Template.DateOption.values()), 0, false);
        if (templateToEdit != null) {
            dateOptionField.setSelectedItem(templateToEdit.getDateOption());
        }
        formPanel.add(dateOptionField);

        contextField = new ComboField<>("Context:", Arrays.asList(Template.Context.values()), 0, false);
        if (templateToEdit != null) {
            contextField.setSelectedItem(templateToEdit.getContext());
        }
        formPanel.add(contextField);

        tagField = new ShortTextField("Tags:", 20);
        tagField.setHelpText("<html>Enter tags separated by commas or spaces." +
                                 "<br>Example: <i>work, personal, todo</i></html>");
        if (templateToEdit != null) {
            tagField.setText(TagList.fromTagList(templateToEdit.getTagList()).getPersistenceString());
        }
        formPanel.add(tagField);

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
