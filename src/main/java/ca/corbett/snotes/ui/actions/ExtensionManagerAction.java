package ca.corbett.snotes.ui.actions;

import ca.corbett.snotes.AppConfig;
import ca.corbett.snotes.ui.MainWindow;

import javax.swing.AbstractAction;
import java.awt.event.ActionEvent;

/**
 * Action to open the Extension Manager dialog.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 * @since Snotes 2.0
 */
public class ExtensionManagerAction extends AbstractAction {

    public ExtensionManagerAction() {
        super("Extensions...");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        AppConfig.getInstance().showExtensionDialog(MainWindow.getInstance());
    }
}
