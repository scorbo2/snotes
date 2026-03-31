package ca.corbett.snotes.ui.actions;

import ca.corbett.extras.EnhancedAction;
import ca.corbett.extras.MessageUtil;
import ca.corbett.snotes.AppConfig;
import ca.corbett.snotes.ui.MainWindow;
import ca.corbett.updates.UpdateManager;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.logging.Logger;

/**
 * An action for launching the preferences dialog.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 * @since Snotes 2.0
 */
public class PrefsAction extends EnhancedAction {

    private static final Logger log = Logger.getLogger(PrefsAction.class.getName());
    private MessageUtil messageUtil;

    public PrefsAction() {
        super("Preferences");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        File dataDirectory = AppConfig.getInstance().getDataDirectory(); // Make a note of the old one
        if (AppConfig.getInstance().showPropertiesDialog(MainWindow.getInstance())) {

            // If the data directory changed, we need to restart the application:
            File newDataDirectory = AppConfig.getInstance().getDataDirectory();
            if (!dataDirectory.getAbsolutePath().equals(newDataDirectory.getAbsolutePath())) {

                // We can restart automatically, if an UpdateManager is configured:
                UpdateManager updateManager = MainWindow.getInstance().getUpdateManager();
                if (updateManager != null) {
                    // Will restart if user confirms:
                    updateManager.showApplicationRestartPrompt(MainWindow.getInstance());
                }

                // Otherwise, all we can do is nag the user to do it for us:
                else {
                    getMessageUtil().info("Restart required",
                                          "Changing the data directory requires restarting the application.");
                }
            }

            // If the user clicked OK, reload the UI:
            UIReloadAction.getInstance().actionPerformed(null);
        }
    }

    private MessageUtil getMessageUtil() {
        if (messageUtil == null) {
            messageUtil = new MessageUtil(MainWindow.getInstance(), log);
        }
        return messageUtil;
    }
}
