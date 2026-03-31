package ca.corbett.snotes;

import ca.corbett.extras.LookAndFeelManager;
import ca.corbett.extras.SingleInstanceManager;
import ca.corbett.snotes.extensions.SnotesExtensionManager;
import ca.corbett.snotes.ui.MainWindow;
import ca.corbett.updates.UpdateSources;

import javax.swing.SwingUtilities;
import java.io.File;
import java.io.IOException;
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

    public static final int SINGLE_INSTANCE_PORT = 56624; // arbitrary random port choice

    public static void main(String[] args) {
        // Before we do anything else, set up logging:
        configureLogging();

        // Ensure only a single instance is running (if configured to do so):
        boolean isSingleInstanceEnabled = Boolean.parseBoolean(AppConfig.peek(AppConfig.SINGLE_INSTANCE_PROP));

        if (isSingleInstanceEnabled) {
            SingleInstanceManager instanceManager = SingleInstanceManager.getInstance();
            if (!instanceManager.tryAcquireLock(Main::handleStartArgs, SINGLE_INSTANCE_PORT)) {
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

        // Make sure our resources are present and loadable:
        if (!Resources.loadAll()) {
            logger.severe("Unable to load application resources - the jar was not packaged correctly.");
            System.exit(1); // No point in proceeding if basic resources are missing
            return; // to satisfy the compiler
        }

        // Load all extra Look and Feels:
        LookAndFeelManager.installExtraLafs();

        // Load all extensions:
        SnotesExtensionManager extManager = SnotesExtensionManager.getInstance();
        extManager.loadAll();
        extManager.activateAll();
        logger.log(Level.INFO, "Loaded {0} extensions ({1} enabled).",
                   new Object[]{extManager.getLoadedExtensionCount(), extManager.getEnabledLoadedExtensions().size()});

        // Load up our application configuration:
        AppConfig.getInstance().load();

        // Get MainWindow ready but don't show it just yet:
        final MainWindow mainWindow = MainWindow.getInstance();
        SwingUtilities.invokeLater(() -> {
            LookAndFeelManager.switchLaf(AppConfig.getInstance().getLookAndFeelClassName());
            mainWindow.processStartArgs(Arrays.asList(args));
        });

        // Parse the update sources file if it was provided,
        // so that dynamic extension discovery and download will be available:
        parseUpdateSources();

        // Now show the main window. It has an async loader on startup that will show
        // a progress dialog after the main window comes up:
        SwingUtilities.invokeLater(() -> mainWindow.setVisible(true));
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
     * If an update sources json was provided, we can parse it here and make it automatically
     * available to our ExtensionManager implementation. This enables dynamic extension
     * discovery and download at runtime.
     */
    private static void parseUpdateSources() {
        Logger logger = Logger.getLogger(Main.class.getName());
        if (Version.UPDATE_SOURCES_FILE != null) {
            try {
                UpdateSources updateSources = UpdateSources.fromFile(Version.UPDATE_SOURCES_FILE);
                SnotesUpdateManager updateManager = new SnotesUpdateManager(updateSources);
                MainWindow.getInstance().setUpdateManager(updateManager);

                // See the javadocs in SnotesUpdateManager for an explanation of why we
                // aren't registering a shutdown hook here. Don't remove the following commented code!
                // We'll restore it when the underlying bug in UpdateManager is fixed.

                // Let's register a shutdown hook for when UpdateManager restarts the app to pick up new extensions:
//                updateManager.registerShutdownHook(() -> {
//                    if (SwingUtilities.isEventDispatchThread()) {
//                        MainWindow.getInstance().cleanup();
//                    } else {
//                        try {
//                            SwingUtilities.invokeAndWait(() -> MainWindow.getInstance().cleanup());
//                        } catch (Exception e) {
//                            Logger.getLogger(Main.class.getName())
//                                  .log(Level.WARNING, "Error during MainWindow cleanup in shutdown hook.", e);
//                        }
//                    }
//                });

                // Let our AboutInfo know about this too, so the About dialog can do application version checks:
                Version.getAboutInfo().updateManager = updateManager;

                logger.info("Update sources provided. Dynamic extension discovery is enabled.");
            }
            catch (Exception e) {
                logger.log(Level.SEVERE,
                           "Unable to parse update sources. Extension download will not be available. Error: "
                               + e.getMessage(),
                           e);
            }
        }
        else {
            // Not an error, just means that we won't be able to pick up extensions dynamically.
            // User can still load extensions by manually dropping the jar file into our extensions directory.
            logger.log(Level.INFO, "No update sources provided. Dynamic extension discovery disabled.");
        }
    }
}
