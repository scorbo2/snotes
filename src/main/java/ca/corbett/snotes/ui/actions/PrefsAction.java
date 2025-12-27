package ca.corbett.snotes.ui.actions;

import ca.corbett.snotes.AppConfig;
import ca.corbett.snotes.ui.MainWindow;

import javax.swing.AbstractAction;
import java.awt.event.ActionEvent;

/**
 * An action for launching the preferences dialog.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 * @since Snotes 2.0
 */
public class PrefsAction extends AbstractAction {

    public PrefsAction() {
        super("Preferences");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (AppConfig.getInstance().showPropertiesDialog(MainWindow.getInstance())) {
            // If the user clicked OK, reload the UI:
            UIReloadAction.getInstance().actionPerformed(null);
        }
    }
}
