package ca.corbett.snotes.ui.actions;

import ca.corbett.extras.EnhancedAction;
import ca.corbett.extras.MessageUtil;
import ca.corbett.snotes.model.Note;
import ca.corbett.snotes.model.Query;
import ca.corbett.snotes.model.Template;
import ca.corbett.snotes.model.filter.TagFilter;
import ca.corbett.snotes.ui.MainWindow;
import ca.corbett.snotes.ui.WriterFrame;

import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

/**
 * This action takes a Template and uses it to create a new Note, showing the results in a new frame.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 * @since Snotes 2.0
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

            WriterFrame writerFrame = new WriterFrame(note, gatherContext());
            MainWindow.getInstance().addInternalFrame(writerFrame);
        }
        catch (IOException ioe) {
            getMessageUtil().error("Execution error",
                                   "An error occurred while executing the template: " + ioe.getMessage(),
                                   ioe);
        }
    }

    /**
     * Creates and executes a Query based on our template's context settings,
     * and returns the resulting Notes as a List.
     */
    private List<Note> gatherContext() {
        if (template.getContext() == null
            || template.getContext() == Template.Context.NONE
            || template.getTagList().isEmpty()) {
            return List.of();
        }

        Query contextQuery = new Query();
        contextQuery.addFilter(new TagFilter(template.getTagList(), TagFilter.FilterType.ANY));
        List<Note> results = contextQuery.execute(MainWindow.getInstance().getDataManager().getNotes());
        if (results.size() > template.getContext().getLimit()) {
            results = results.subList(0, template.getContext().getLimit());
        }
        return results;
    }

    private MessageUtil getMessageUtil() {
        if (messageUtil == null) {
            messageUtil = new MessageUtil(MainWindow.getInstance(), log);
        }
        return messageUtil;
    }
}
