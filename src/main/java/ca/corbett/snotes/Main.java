package ca.corbett.snotes;

import ca.corbett.extras.LookAndFeelManager;
import ca.corbett.extras.SingleInstanceManager;
import ca.corbett.extras.image.ImageUtil;
import ca.corbett.extras.progress.SimpleProgressWorker;
import ca.corbett.extras.progress.SplashProgressWindow;
import ca.corbett.snotes.ui.MainWindow;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * The entry point for the application. There are no command line parameters,
 * but you can specify a few system properties to achieve different things:
 * <ul>
 *     <li><b>java.util.logging.config.file</b> - if set, this is the full path
 *     and name of your custom logging.properties file. If not set, the default
 *     logging.properties will be used (from the application jar file).
 *     By default, all log output goes to the console. You can specify a custom
 *     logging.properties to easily change that.</li>
 *     <li><b>SETTINGS_DIR</b> - This defaults to a directory named ".Snotes"
 *     in the user's home directory, but can be overridden. The application
 *     configuration file lives here.</li>
 *     <li><b>EXTENSIONS_DIR</b> - This defaults to a directory named "extensions"
 *     inside SETTINGS_DIR, but can be overridden. This is the
 *     directory from which extension jars will be loaded.</li>
 * </ul>
 * <p>
 *     <b>Note:</b> If you used the installer script to install the application,
 *     these system properties will already have been set for you in the
 *     launcher script, and you don't have to worry about them.
 * </p>
 *
 * @author scorbo2
 */
public class Main {

    public static URL logoIconUrl;
    public static URL logoWideUrl;
    public static BufferedImage logoWideImage;

    public static void main(String[] args) {
        // Before we do anything else, set up logging:
        configureLogging();

        // Ensure only a single instance is running (if configured to do so):
        //boolean isSingleInstanceEnabled = Boolean.parseBoolean(AppConfig.peek("UI.General.singleInstance"));

        // TODO wire up AppConfig... for now, just hard-code it to true
        boolean isSingleInstanceEnabled = true;

        if (isSingleInstanceEnabled) {
            SingleInstanceManager instanceManager = SingleInstanceManager.getInstance();
            if (!instanceManager.tryAcquireLock(Main::handleStartArgs)) {
                // Another instance is already running, let's send our args to it and exit:
                // Send even if empty, as this will force the main window to the front.
                SingleInstanceManager.getInstance().sendArgsToRunningInstance(args);
                return;
            }
        }

        // We are the only instance running, so we can start up normally:
        Logger logger = Logger.getLogger(Main.class.getName());
        logger.log(Level.INFO,
                   Version.FULL_NAME + " starting up: installDir={0}, settingsDir={1}, extensionsDir={2}",
                   new Object[]{Version.INSTALL_DIR, Version.SETTINGS_DIR, Version.EXTENSIONS_DIR});

        // Prepare our splash screen:
        logoWideUrl = Main.class.getResource("/ca/corbett/snotes/images/logo_wide.jpg");
        logoIconUrl = Main.class.getResource("/ca/corbett/snotes/images/logo.png");
        if (logoWideUrl == null || logoIconUrl == null) {
            logger.severe("Unable to load splash image - the jar was not packaged correctly.");
            System.exit(1); // No point in proceeding if basic resources are missing
            return; // to satisfy the compiler
        }

        // Get the splash progress screen ready:
        // (do this before MainWindow.getInstance() so our logo image is loaded):
        SplashProgressWindow splashWindow;
        try {
            logoWideImage = ImageUtil.loadImage(logoWideUrl);
            splashWindow = new SplashProgressWindow(Color.GRAY, Color.BLACK, logoWideImage);
        }
        catch (IOException ioe) {
            logger.log(Level.SEVERE, "Unable to load logo image.", ioe);
            System.exit(1); // No point in proceeding if basic resources are missing
            return; // to satisfy the compiler
        }

        // Load all extra Look and Feels:
        LookAndFeelManager.installExtraLafs();
        final MainWindow mainWindow = MainWindow.getInstance();

        // Load up our application configuration:
        AppConfig.getInstance().load();

        // Get MainWindow ready but don't show it just yet:
        SwingUtilities.invokeLater(() -> {
            LookAndFeelManager.switchLaf(AppConfig.getInstance().getLookAndFeelClassName());
            mainWindow.processStartArgs(Arrays.asList(args));
        });

        // Show the splash progress screen, which will show the main window when done:
        splashWindow.runWorker(new StartupWorker(mainWindow));
    }

    /**
     * Invoked internally to handle start arguments on the EDT.
     * This is invoked when a second instance tries to start up when
     * single instance mode is enabled. In that case, the new instance
     * will send its args to the running instance and immediately terminate.
     * The running instance can then process those args.
     */
    private static void handleStartArgs(List<String> args) {
        SwingUtilities.invokeLater(() -> MainWindow.getInstance().processStartArgs(args));
    }

    /**
     * Logging can use the built-in configuration, or you can supply your own logging properties file.
     * <ol>
     *     <li><b>Built-in logging.properties</b>: the jar file comes packaged with a default logging.properties
     *     file that you can use. You don't need to do anything to activate this config: this is the default.</li>
     *     <li><b>Specify your own</b>: you can create a logging.properties file and put it $SETTINGS_DIR,
     *     OR you can start the application with the -Djava.util.logging.config.file=
     *     option, in which case you can point it to wherever your logging.properties file lives.</li>
     * </ol>
     */
    private static void configureLogging() {
        // If the java.util.logging.config.file System property exists, do nothing.
        // It will be used automatically.
        if (System.getProperties().containsKey("java.util.logging.config.file")) {
            //System.out.println("Using custom log file: " + System.getProperty("java.util.logging.config.file"));
            return;
        }

        // Otherwise, see if we can spot a logging.properties file in the application dir:
        File propsFile = new File(Version.SETTINGS_DIR, "logging.properties");
        if (propsFile.exists() && propsFile.canRead()) {
            System.setProperty("java.util.logging.config.file", propsFile.getAbsolutePath());
            //System.out.println("Using auto-detected log file: " + propsFile.getAbsolutePath());
            return;
        }

        // Otherwise, load the built-in config:
        try {
            //System.out.println("Using built-in logging.");
            LogManager.getLogManager()
                      .readConfiguration(Main.class.getResourceAsStream("/ca/corbett/snotes/logging.properties"));
        }
        catch (IOException ioe) {
            System.out.println("WARN: Unable to load log configuration: " + ioe.getMessage());
        }
    }

    /**
     * A worker thread to perform one-time startup tasks and report progress to the splash screen progress bar.
     */
    private static class StartupWorker extends SimpleProgressWorker {

        private final JFrame window;

        public StartupWorker(JFrame window) {
            this.window = window;
        }

        @Override
        public void run() {
            // TODO here is where we load all Snotes files, which may take quite some time.
            // We can fire progress events as we go, and the splash screen's progress bar will update.
            // For now, let's just pretend we finished the work in one step:
            fireProgressComplete();

            // Once complete, we can show the main window on the EDT thread:
            SwingUtilities.invokeLater(() -> window.setVisible(true));
        }
    }
}
