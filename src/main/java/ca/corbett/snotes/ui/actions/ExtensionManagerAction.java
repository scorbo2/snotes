package ca.corbett.snotes.ui.actions;

import ca.corbett.extras.EnhancedAction;
import ca.corbett.snotes.AppConfig;
import ca.corbett.snotes.ui.MainWindow;

import java.awt.event.ActionEvent;

/**
 * Action to open the Extension Manager dialog.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 * @since Snotes 2.0
 */
public class ExtensionManagerAction extends EnhancedAction {

    public ExtensionManagerAction() {
        super("Extension Manager");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (AppConfig.getInstance().showExtensionDialog(MainWindow.getInstance(),
                                                        MainWindow.getInstance().getUpdateManager())) {
            // Reload the UI to reflect any changes in extensions:
            UIReloadAction.getInstance().actionPerformed(null);
        }
    }
}
