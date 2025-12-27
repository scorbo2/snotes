package ca.corbett.snotes.ui.actions;

import ca.corbett.extras.about.AboutDialog;
import ca.corbett.snotes.Version;
import ca.corbett.snotes.ui.MainWindow;

import javax.swing.AbstractAction;
import java.awt.event.ActionEvent;

public class AboutAction extends AbstractAction {

    public AboutAction() {
        super("About");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        new AboutDialog(MainWindow.getInstance(), Version.getAboutInfo()).setVisible(true);
    }
}
