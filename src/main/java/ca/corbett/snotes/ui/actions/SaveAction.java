package ca.corbett.snotes.ui.actions;

import ca.corbett.extras.EnhancedAction;
import ca.corbett.snotes.ui.MainWindow;

import java.awt.event.ActionEvent;

/**
 * What I want to do: have Ctrl+S mapped as a keyboard shortcut on each individual WriterFrame.
 * But, WriterFrame is a JInternalFrame, and our KeyStrokeManager only works with Window instances.
 * So, instead, we will have one global Ctrl+S shortcut on the MainWindow, and we'll just
 * find all WriterFrames and invoke save() on them.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 * @since Snotes 2.0
 */
public class SaveAction extends EnhancedAction {

    public SaveAction() {
        super("Save all");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        MainWindow.getInstance().saveAll();
    }
}
