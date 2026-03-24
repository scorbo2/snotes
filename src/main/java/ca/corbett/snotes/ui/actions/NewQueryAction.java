package ca.corbett.snotes.ui.actions;

import ca.corbett.extras.EnhancedAction;
import ca.corbett.extras.MessageUtil;
import ca.corbett.snotes.ui.MainWindow;
import ca.corbett.snotes.ui.query.QueryBuilderDialog;

import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * An action which will show the QueryBuilderDialog to create a new Query.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 * @since Snotes 2.0
 */
public class NewQueryAction extends EnhancedAction {

    private static final Logger log = Logger.getLogger(NewQueryAction.class.getName());
    private MessageUtil messageUtil;

    public NewQueryAction() {
        super("New query");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        QueryBuilderDialog dialog = new QueryBuilderDialog(MainWindow.getInstance());
        dialog.setVisible(true);
        if (dialog.wasOkayed()) {
            try {
                MainWindow.getInstance().getDataManager().saveQuery(dialog.getQuery());

                // Trigger a reload so the new query shows up in the ActionPanel:
                UIReloadAction.getInstance().actionPerformed(null);
            }
            catch (IOException ioe) {
                getMessageUtil().error("Save error",
                                       "An error occurred while saving the new query: " + ioe.getMessage(),
                                       ioe);
            }
        }
    }

    private MessageUtil getMessageUtil() {
        if (messageUtil == null) {
            messageUtil = new MessageUtil(MainWindow.getInstance(), log);
        }
        return messageUtil;
    }
}
