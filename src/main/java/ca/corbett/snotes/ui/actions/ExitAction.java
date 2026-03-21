package ca.corbett.snotes.ui.actions;

import ca.corbett.extras.EnhancedAction;
import ca.corbett.snotes.ui.MainWindow;

import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.util.logging.Logger;

/**
 * A simple action to exit the application.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
public class ExitAction extends EnhancedAction {

    private static final Logger log = Logger.getLogger(ExitAction.class.getName());

    public ExitAction() {
        super("Exit");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        log.info("Exiting application...");

        // Close the MainWindow, which will trigger any cleanup required and then exit:
        MainWindow.getInstance().dispatchEvent(new WindowEvent(MainWindow.getInstance(), WindowEvent.WINDOW_CLOSING));
    }
}
