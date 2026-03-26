package ca.corbett.snotes.ui.actions;

import ca.corbett.extras.EnhancedAction;
import ca.corbett.extras.MessageUtil;
import ca.corbett.snotes.model.Note;
import ca.corbett.snotes.model.Template;
import ca.corbett.snotes.ui.MainWindow;

import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * This action takes a Template and uses it to create a new Note, showing the results in a new frame.
 * TODO this is a placeholder until the UI is ready for it.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
public class ExecuteTemplateAction extends EnhancedAction {
    private static final Logger log = Logger.getLogger(ExecuteTemplateAction.class.getName());
    private MessageUtil messageUtil;

    private final Template template;

    public ExecuteTemplateAction(Template template) {
        super(template == null ? "(empty template)" : template.getName()); // The template name is our action label
        this.template = template;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (template == null) {
            getMessageUtil().error("No template", "This action doesn't have a template to execute!");
            return;
        }

        try {
            // Execute the Template and get a new "scratch" Note:
            Note note = template.execute(MainWindow.getInstance().getDataManager());
            if (note == null) {
                getMessageUtil().info("No results", "The template failed to execute.");
                return;
            }

            // TODO: when the UI is ready, we also need to gather context based on the
            //       "context" option within the Template. For now, we skip this step.

            // The UI for showing/editing Notes is not yet written. So, here is our placeholder:
            getMessageUtil().info("Results", "Well, here's where I would show the " +
                "newly-created Note, if I had a UI to show it!");
        }
        catch (IOException ioe) {
            getMessageUtil().error("Execution error",
                                   "An error occurred while executing the template: " + ioe.getMessage(),
                                   ioe);
        }
    }

    private MessageUtil getMessageUtil() {
        if (messageUtil == null) {
            messageUtil = new MessageUtil(MainWindow.getInstance(), log);
        }
        return messageUtil;
    }
}
