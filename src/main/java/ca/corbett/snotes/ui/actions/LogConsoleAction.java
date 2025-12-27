package ca.corbett.snotes.ui.actions;

import ca.corbett.extras.logging.LogConsole;

import javax.swing.AbstractAction;
import java.awt.event.ActionEvent;

/**
 * An action for showing the log console.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 * @since Snotes 2.0
 */
public class LogConsoleAction extends AbstractAction {

    public LogConsoleAction() {
        super("Show Log Console");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        LogConsole.getInstance().setVisible(true);
    }
}
