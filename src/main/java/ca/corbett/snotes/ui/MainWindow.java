package ca.corbett.snotes.ui;

import ca.corbett.extras.CustomizableDesktopPane;
import ca.corbett.extras.logging.LogConsole;
import ca.corbett.snotes.AppConfig;
import ca.corbett.snotes.Main;
import ca.corbett.snotes.Version;
import ca.corbett.snotes.ui.actions.AboutAction;
import ca.corbett.snotes.ui.actions.LogConsoleAction;
import ca.corbett.snotes.ui.actions.PrefsAction;
import ca.corbett.snotes.ui.actions.UIReloadAction;
import org.jdesktop.swingx.JXTaskPane;
import org.jdesktop.swingx.JXTaskPaneContainer;

import javax.swing.JFrame;
import javax.swing.JSplitPane;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Toolkit;
import java.util.logging.Logger;

/**
 * The main window for the Snotes application.
 * TODO this needs a lot of work...
 *
 * @author scorbo2
 */
public class MainWindow extends JFrame implements UIReloadable {

    private static final Logger logger = Logger.getLogger(MainWindow.class.getName());
    private static final MainWindow instance = new MainWindow();

    private CustomizableDesktopPane desktopPane;
    private JXTaskPane readTaskPane;
    private JXTaskPane writeTaskPane;

    private MainWindow() {
        super(Version.FULL_NAME);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setMinimumSize(new Dimension(500, 400));
        initComponents();
    }

    public static MainWindow getInstance() {
        return instance;
    }

    /**
     * Overridden here so we can do some initialization when the
     * window is first shown.
     */
    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (visible) {
            Image image = Toolkit.getDefaultToolkit().createImage(Main.logoIconUrl);
            instance.setIconImage(image);
            LogConsole.getInstance().setIconImage(image);
            UIReloadAction.getInstance().registerReloadable(this);
        }
    }

    public void processStartArgs(java.util.List<String> args) {
        // TODO implement argument processing
    }

    private void initComponents() {
        desktopPane = new CustomizableDesktopPane(Main.logoWideImage,
                                                  AppConfig.getInstance().getDesktopLogoPlacement(),
                                                  AppConfig.getInstance().getDesktopLogoAlpha(),
                                                  AppConfig.getInstance().getDesktopGradient());

        JSplitPane splitPane = new JSplitPane();
        splitPane.setOneTouchExpandable(false); // Sadly, this does not play well with some look and feels
        splitPane.setLeftComponent(buildTaskPaneContainer());
        splitPane.setRightComponent(desktopPane);
        splitPane.setDividerLocation(0.25);

        setLayout(new BorderLayout());
        add(splitPane, BorderLayout.CENTER);
    }

    private JXTaskPaneContainer buildTaskPaneContainer() {
        JXTaskPaneContainer container = new JXTaskPaneContainer();

        readTaskPane = new JXTaskPane();
        readTaskPane.setTitle("Read Notes");
        // TODO add read task pane components

        writeTaskPane = new JXTaskPane();
        writeTaskPane.setTitle("Write Notes");
        // TODO add write task pane components

        // TODO interrogate extensions for additional task panes...

        JXTaskPane otherPane = new JXTaskPane();
        otherPane.setTitle("Other");
        otherPane.add(new PrefsAction());
        otherPane.add(new LogConsoleAction());
        otherPane.add(new AboutAction());

        container.add(readTaskPane);
        container.add(writeTaskPane);
        container.add(otherPane);

        return container;
    }

    /**
     * Invoked when the application preferences dialog or the extension manager dialog
     * have been okayed, and we need to reload the UI to reflect any changes.
     */
    @Override
    public void reloadUI() {
        desktopPane.setLogoImageTransparency(AppConfig.getInstance().getDesktopLogoAlpha());
        desktopPane.setGradientConfig(AppConfig.getInstance().getDesktopGradient());
        desktopPane.setLogoImagePlacement(AppConfig.getInstance().getDesktopLogoPlacement());
        desktopPane.repaint();
    }
}
