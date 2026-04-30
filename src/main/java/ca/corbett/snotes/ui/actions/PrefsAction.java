package ca.corbett.snotes.ui.actions;

import ca.corbett.extras.EnhancedAction;
import ca.corbett.extras.MessageUtil;
import ca.corbett.snotes.AppConfig;
import ca.corbett.snotes.ui.MainWindow;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
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
                MainWindow.getInstance().saveAll(); // save everything first!
                MainWindow.getInstance().closeAll();
                try {
                    // Purge in-memory cache, switch to the new data directory, and reload the UI.
                    MainWindow.getInstance().getDataManager().switchDataDirectory(newDataDirectory, l -> {
                        MainWindow.getInstance().resetInitialLoad();
                        UIReloadAction.getInstance().actionPerformed(null);
                    });

                    // Avoid multiple UI reloads:
                    return;
                }
                catch (IOException ioe) {
                    log.severe("Failed to switch data directory: " + ioe.getMessage());
                    getMessageUtil().error("Failed to switch data directory: " + ioe.getMessage(), ioe);
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
