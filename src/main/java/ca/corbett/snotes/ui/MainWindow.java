package ca.corbett.snotes.ui;

import ca.corbett.snotes.Version;

import javax.swing.JFrame;

/**
 * The main window for the Snotes application.
 * TODO this needs a lot of work...
 *
 * @author scorbo2
 */
public class MainWindow extends JFrame {

    private static final MainWindow instance = new MainWindow();

    private MainWindow() {
        super(Version.FULL_NAME);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
    }

    public static MainWindow getInstance() {
        return instance;
    }

    public void processStartArgs(java.util.List<String> args) {
        // TODO implement argument processing
    }

}
