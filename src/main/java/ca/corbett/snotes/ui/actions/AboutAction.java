package ca.corbett.snotes.ui.actions;

import ca.corbett.extras.EnhancedAction;
import ca.corbett.extras.about.AboutDialog;
import ca.corbett.snotes.Version;
import ca.corbett.snotes.ui.MainWindow;

import java.awt.event.ActionEvent;

public class AboutAction extends EnhancedAction {

    public AboutAction() {
        super("About");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        new AboutDialog(MainWindow.getInstance(), Version.getAboutInfo()).setVisible(true);
    }
}
