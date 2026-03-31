package ca.corbett.snotes.ui.actions;

import ca.corbett.extras.EnhancedAction;
import ca.corbett.extras.MessageUtil;
import ca.corbett.snotes.io.DataManager;
import ca.corbett.snotes.model.Note;
import ca.corbett.snotes.model.Query;
import ca.corbett.snotes.ui.MainWindow;
import ca.corbett.snotes.ui.ReaderFrame;

import java.awt.event.ActionEvent;
import java.util.List;
import java.util.logging.Logger;

/**
 * This action takes a Query and executes it, showing the results in a new frame.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
public class ExecuteQueryAction extends EnhancedAction {

    private static final Logger log = Logger.getLogger(ExecuteQueryAction.class.getName());
    private MessageUtil messageUtil;

    private final Query query;

    public ExecuteQueryAction(Query query) {
        super(query == null ? "(empty query)" : query.getName()); // The query name is our action label
        this.query = query;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (query == null) {
            getMessageUtil().error("No query", "This action doesn't have a query to execute!");
            return;
        }

        DataManager dataManager = MainWindow.getInstance().getDataManager();
        List<Note> results = query.execute(dataManager.getNotes());
        ReaderFrame readerFrame = new ReaderFrame(results, query); // Let reader frame handle the empty results case
        MainWindow.getInstance().addInternalFrame(readerFrame);
    }

    private MessageUtil getMessageUtil() {
        if (messageUtil == null) {
            messageUtil = new MessageUtil(MainWindow.getInstance(), log);
        }
        return messageUtil;
    }
}
