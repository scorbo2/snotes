package ca.corbett.snotes.ui.actions;

import ca.corbett.extras.EnhancedAction;
import ca.corbett.extras.MessageUtil;
import ca.corbett.snotes.AppConfig;
import ca.corbett.snotes.ui.MainWindow;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
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
        File dataDirectory = AppConfig.getInstance().getDataDirectory();
        if (AppConfig.getInstance().showPropertiesDialog(MainWindow.getInstance())) {

            // If the data directory changed, we need to restart the application:
            File newDataDirectory = AppConfig.getInstance().getDataDirectory();
            try {
                if (!Files.isSameFile(dataDirectory.toPath(), newDataDirectory.toPath())) {
                    getMessageUtil().info("Restart required",
                                          "Changing the data directory requires restarting the application.");

                    // TODO once we have wired up the UpdateManager (future ticket),
                    //      we can restart the application automatically.
                    //      Until then, all we can do is nag the user to do it for us.
                }
            }
            catch (IOException ioe) {
                log.severe("Error checking data directory: " + ioe.getMessage());
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
