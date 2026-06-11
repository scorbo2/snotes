package ca.corbett.snotes.ui.actions;

import ca.corbett.extras.EnhancedAction;
import ca.corbett.snotes.ui.MainWindow;
import ca.corbett.snotes.ui.TagListDialog;

import java.awt.event.ActionEvent;

/**
 * An action for launching the TagListDialog.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 * @since Snotes 2.2
 */
public class TagListAction extends EnhancedAction {
    public TagListAction() {
        super("Tags");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        TagListDialog dialog = new TagListDialog(MainWindow.getInstance(), MainWindow.getInstance().getDataManager());
        dialog.setVisible(true);
    }
}
