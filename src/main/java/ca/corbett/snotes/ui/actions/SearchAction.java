package ca.corbett.snotes.ui.actions;

import ca.corbett.extras.EnhancedAction;
import ca.corbett.snotes.ui.SearchDialog;

import java.awt.event.ActionEvent;

/**
 * An action to bring up the SearchDialog for searching for notes.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 * @since Snotes 2.0
 */
public class SearchAction extends EnhancedAction {

    public SearchAction() {
        super("Search");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        new SearchDialog().setVisible(true);
    }
}
