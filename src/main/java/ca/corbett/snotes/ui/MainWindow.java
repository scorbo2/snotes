package ca.corbett.snotes.ui;

import ca.corbett.extras.CustomizableDesktopPane;
import ca.corbett.extras.SingleInstanceManager;
import ca.corbett.extras.io.KeyStrokeManager;
import ca.corbett.extras.logging.LogConsole;
import ca.corbett.extras.properties.KeyStrokeProperty;
import ca.corbett.snotes.AppConfig;
import ca.corbett.snotes.Resources;
import ca.corbett.snotes.Version;
import ca.corbett.snotes.extensions.SnotesExtensionManager;
import ca.corbett.snotes.ui.actions.UIReloadAction;

import javax.swing.JFrame;
import javax.swing.JSplitPane;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.logging.Logger;

/**
 * The main window for the Snotes application.
 * TODO this needs a lot of work...
 *
 * @author scorbo2
 */
public class MainWindow extends JFrame implements UIReloadable {

    private static final Logger logger = Logger.getLogger(MainWindow.class.getName());
    private static MainWindow instance = null;
    private final ActionPanelManager actionPanelManager;
    private final KeyStrokeManager keyStrokeManager;
    private boolean cleanupComplete;

    private CustomizableDesktopPane desktopPane;

    private MainWindow() {
        super(Version.FULL_NAME);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setMinimumSize(new Dimension(500, 400));
        setLocationRelativeTo(null); // center on default display
        keyStrokeManager = new KeyStrokeManager(this);
        actionPanelManager = new ActionPanelManager();
        initComponents();
        addWindowListener(new WindowCloseHandler());
        cleanupComplete = false;
    }

    public static MainWindow getInstance() {
        if (instance == null) {
            instance = new MainWindow();
        }
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
            instance.setIconImage(Resources.getLogoIcon());
            LogConsole.getInstance().setIconImage(Resources.getLogoIcon());
            UIReloadAction.getInstance().registerReloadable(this);
            reloadUI(); // trigger an immediate reload to set everything up according to current settings
        }
    }

    public void processStartArgs(java.util.List<String> args) {
        // TODO implement argument processing - is there any for this app?
    }

    private void initComponents() {
        desktopPane = new CustomizableDesktopPane(Resources.getLogoWide(),
                                                  AppConfig.getInstance().getDesktopLogoPlacement(),
                                                  AppConfig.getInstance().getDesktopLogoAlpha(),
                                                  AppConfig.getInstance().getDesktopGradient());

        JSplitPane splitPane = new JSplitPane();
        splitPane.setOneTouchExpandable(false); // Sadly, this does not play well with some look and feels
        splitPane.setLeftComponent(actionPanelManager.getActionPanel());
        splitPane.setRightComponent(desktopPane);
        splitPane.setDividerLocation(0.25);

        setLayout(new BorderLayout());
        add(splitPane, BorderLayout.CENTER);
    }

    private void cleanup() {
        // Make the method idempotent, just in case:
        if (cleanupComplete) {
            return;
        }

        cleanupComplete = true;
        logger.info("Shutting down: MainWindow cleanup invoked.");

        actionPanelManager.dispose();
        keyStrokeManager.dispose();
        SnotesExtensionManager.getInstance().deactivateAll();
        SingleInstanceManager.getInstance().release();
    }

    /**
     * Invoked when the application preferences dialog or the extension manager dialog
     * have been okayed, and we need to reload the UI to reflect any changes.
     */
    @Override
    public void reloadUI() {
        // Update our desktop pane with new settings:
        desktopPane.setLogoImageTransparency(AppConfig.getInstance().getDesktopLogoAlpha());
        desktopPane.setGradientConfig(AppConfig.getInstance().getDesktopGradient());
        desktopPane.setLogoImagePlacement(AppConfig.getInstance().getDesktopLogoPlacement());
        desktopPane.repaint();

        // The actions in our ActionManager may need refreshing:
        actionPanelManager.reload();

        // Clear all keystrokes and reload, since they are all user-configurable:
        keyStrokeManager.clear();
        for (KeyStrokeProperty prop : AppConfig.getInstance().getKeyStrokeProperties()) {
            // If there's no Action attached, or if there is no keystroke assigned to it, skip it:
            if (prop.getAction() == null || prop.getKeyStroke() == null) {
                continue;
            }

            // Register it! This will update the shortcut attached to our menu items as well:
            keyStrokeManager.registerHandler(prop.getKeyStroke(), prop.getAction());
        }
    }

    /**
     * This class can ensure that our cleanup() method is invoked
     * whenever the main window is closed, whether by user action
     * or programmatically.
     */
    private static class WindowCloseHandler extends WindowAdapter {
        /**
         * Invoked when the user manually closes a window by clicking its X button
         * or using a keyboard shortcut like Ctrl+Q or whatever. This event handler
         * is NOT invoked when you manually dispose() the window (at least in my
         * testing on linux mint). We need BOTH windowClosing() and windowClosed() handlers
         * to ensure cleanup() is always invoked.
         */
        @Override
        public void windowClosing(WindowEvent e) {
            MainWindow.getInstance().cleanup();
        }

        /**
         * Invoked when you programmatically dispose() of the window. Note that the
         * user manually closing the window via the OS does NOT invoke this handler
         * (at least in my testing on linux mint). We need BOTH windowClosing() and windowClosed() handlers
         * to ensure cleanup() is always invoked.
         */
        @Override
        public void windowClosed(WindowEvent e) {
            MainWindow.getInstance().cleanup();
        }
    }
}
