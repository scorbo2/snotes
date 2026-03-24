package ca.corbett.snotes.ui.actions;

import ca.corbett.extras.EnhancedAction;
import ca.corbett.snotes.ui.query.ManageQueriesDialog;

import java.awt.event.ActionEvent;

/**
 * Brings up the ManageQueriesDialog, which allows for viewing, editing, or deleting saved Queries.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 * @since Snotes 2.0
 */
public class ManageQueriesAction extends EnhancedAction {

    public ManageQueriesAction() {
        super("Manage Queries");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        new ManageQueriesDialog().setVisible(true);
    }
}
