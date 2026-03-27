package ca.corbett.snotes.ui;

import ca.corbett.extras.CustomizableDesktopPane;
import ca.corbett.extras.MessageUtil;
import ca.corbett.extras.SingleInstanceManager;
import ca.corbett.extras.io.KeyStrokeManager;
import ca.corbett.extras.logging.LogConsole;
import ca.corbett.extras.properties.KeyStrokeProperty;
import ca.corbett.snotes.AppConfig;
import ca.corbett.snotes.Main;
import ca.corbett.snotes.Resources;
import ca.corbett.snotes.Version;
import ca.corbett.snotes.extensions.SnotesExtensionManager;
import ca.corbett.snotes.io.DataManager;
import ca.corbett.snotes.ui.actions.UIReloadAction;

import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JSplitPane;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyVetoException;
import java.io.IOException;
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
    private final MessageUtil messageUtil;
    private boolean isSingleInstanceModeEnabled;
    private final ActionPanelManager actionPanelManager;
    private final KeyStrokeManager keyStrokeManager;
    private final DataManager dataManager;
    private boolean cleanupComplete;

    private CustomizableDesktopPane desktopPane;

    private MainWindow() {
        super(Version.FULL_NAME);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setMinimumSize(new Dimension(500, 400));
        setLocationRelativeTo(null); // center on default display
        isSingleInstanceModeEnabled = AppConfig.getInstance().isSingleInstanceEnabled();
        messageUtil = new MessageUtil(this, logger);
        dataManager = new DataManager();
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
        if (visible) {
            restoreWindowState(); // do this BEFORE we call super.setVisible()
        }
        super.setVisible(visible);
        if (visible) {
            instance.setIconImage(Resources.getLogoIcon());
            LogConsole.getInstance().setIconImage(Resources.getLogoIcon());
            UIReloadAction.getInstance().registerReloadable(this);

            // Tell our DataManager to load everything (background thread), and then trigger a UI reload when finished:
            try {
                dataManager.loadAll(AppConfig.getInstance().getDataDirectory(), e -> reloadUI());
            }
            catch (IOException ioe) {
                messageUtil.error("Load error", "An error occurred while loading data: " + ioe.getMessage(), ioe);
            }
        }
    }

    /**
     * Provides access to our DataManager.
     */
    public DataManager getDataManager() {
        return dataManager;
    }

    public void processStartArgs(java.util.List<String> args) {
        // TODO implement argument processing - is there any for this app?
    }

    /**
     * Can be invoked by application code or by application extensions to add a new internal frame to the desktop pane.
     *
     * @param frame The JInternalFrame to add to the desktop pane. Will be made visible and selected immediately.
     */
    public void addInternalFrame(JInternalFrame frame) {
        if (frame == null) {
            logger.warning("Attempted to add null internal frame to desktop pane. Ignoring.");
            return;
        }
        desktopPane.add(frame);
        frame.setVisible(true);
        try {
            frame.setSelected(true);
        }
        catch (PropertyVetoException e) {
            logger.warning("Failed to select internal frame: " + e.getMessage());
        }
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

        // Always save window state, even if "remember state" is disabled:
        AppConfig.getInstance().setWindowProps(getExtendedState(), getWidth(), getHeight(), getX(), getY());

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
        // Single instance mode may have changed, so check that:
        if (isSingleInstanceModeEnabled != AppConfig.getInstance().isSingleInstanceEnabled()) {
            toggleSingleInstanceMode();
        }

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
     * Invoked internally to toggle the state of single-instance mode.
     */
    private void toggleSingleInstanceMode() {
        // Toggle our cached value:
        isSingleInstanceModeEnabled = !isSingleInstanceModeEnabled;

        // If single instance mode is now enabled, try to acquire the lock:
        if (isSingleInstanceModeEnabled) {
            logger.info("Enabling single instance mode.");
            SingleInstanceManager instanceManager = SingleInstanceManager.getInstance();
            if (!instanceManager.tryAcquireLock(a -> MainWindow.getInstance().processStartArgs(a),
                                                Main.SINGLE_INSTANCE_PORT)) {
                // Another instance is already running, let's inform the user:
                messageUtil.error("Single Instance Mode",
                                  "Another instance of the application is already running.\n" +
                                      "Unable to enable single instance mode.");
                isSingleInstanceModeEnabled = false; // revert our cached value
            }
        }

        // Otherwise, if single instance mode is now disabled, release the lock if we have it:
        else {
            logger.info("Disabling single instance mode.");
            SingleInstanceManager.getInstance().release();
        }
    }

    /**
     * If "remember window size and position" is enabled, restores the
     * window size, position, and state (maximized, minimized, etc.) from the last application run.
     * Otherwise, does nothing.
     * <p>
     * Note, on a first-time application run, or if our config file was removed,
     * the values will be explicitly unset, and so we will stick with the
     * default size, position, and state. On every subsequent run,
     * if the option is enabled, we will restore the last size, position, and state.
     * </p>
     */
    private void restoreWindowState() {
        AppConfig appConfig = AppConfig.getInstance();
        if (!appConfig.isRememberSizeAndPositionEnabled()) {
            // Do nothing if the option is disabled.
            return;
        }

        int state = appConfig.getWindowState();
        int width = appConfig.getWindowWidth();
        int height = appConfig.getWindowHeight();
        int x = appConfig.getWindowLeft();
        int y = appConfig.getWindowTop();
        if (state == AppConfig.VALUE_NOT_SET
            || width == AppConfig.VALUE_NOT_SET
            || height == AppConfig.VALUE_NOT_SET
            || x == AppConfig.VALUE_NOT_SET
            || y == AppConfig.VALUE_NOT_SET) {
            // Do nothing if any of the values are missing or invalid.
            return;
        }

        // Special case: if the application was closed while minimized,
        // ignore that, and restore NORMAL state instead. It would be
        // terribly confusing to start the application and not see any window because it's minimized!
        if ((state & ICONIFIED) != 0) {
            state = NORMAL;
        }

        setSize(width, height);
        setLocation(x, y);
        setExtendedState(state);
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
