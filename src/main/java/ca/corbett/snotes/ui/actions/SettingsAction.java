package ca.corbett.snotes.ui.actions;

import ca.corbett.extras.EnhancedAction;
import ca.corbett.snotes.AppConfig;
import ca.corbett.snotes.ui.MainWindow;

import java.awt.event.ActionEvent;

/**
 * An action for showing the application settings dialog.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 * @since Snotes 2.0
 */
public class SettingsAction extends EnhancedAction {

    public SettingsAction() {
        super("Settings");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (AppConfig.getInstance().showPropertiesDialog(MainWindow.getInstance())) {
            UIReloadAction.getInstance().actionPerformed(null);
        }
    }
}
