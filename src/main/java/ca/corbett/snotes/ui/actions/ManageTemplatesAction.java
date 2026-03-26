package ca.corbett.snotes.ui.actions;

import ca.corbett.extras.EnhancedAction;
import ca.corbett.snotes.ui.template.ManageTemplatesDialog;

import java.awt.event.ActionEvent;

/**
 * Brings up the ManageTemplatesDialog, which allows for viewing, editing, or deleting saved Templates.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 * @since Snotes 2.0
 */
public class ManageTemplatesAction extends EnhancedAction {

    public ManageTemplatesAction() {
        super("Manage Templates");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        new ManageTemplatesDialog().setVisible(true);
    }
}
