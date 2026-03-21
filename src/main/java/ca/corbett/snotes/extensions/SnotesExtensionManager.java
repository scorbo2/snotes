package ca.corbett.snotes.extensions;

import ca.corbett.extensions.ExtensionManager;
import ca.corbett.extras.EnhancedAction;
import ca.corbett.snotes.Version;
import ca.corbett.snotes.extensions.builtin.TestExtension;
import ca.corbett.snotes.ui.actions.ActionGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * Manages extensions for Snotes, and provides wrapper methods to make it
 * easy for the application code to interrogate extensions as to their capabilities.
 * <p>
 *     Extensions are loaded from the ${EXTENSIONS_DIR} at application startup.
 *     If you installed the application via the installer script, this directory
 *     is already configured for you. You can override it by setting the EXTENSIONS_DIR
 *     system property when launching the application, as shown below:
 * </p>
 * <pre>java -DEXTENSIONS_DIR=/path/to/extensions/dir -jar snotes.jar</pre>
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 * @since Snotes 2.0
 */
public class SnotesExtensionManager extends ExtensionManager<SnotesExtension> {

    private static final SnotesExtensionManager instance;

    static {
        instance = new SnotesExtensionManager();
    }

    protected SnotesExtensionManager() {
    }

    public static SnotesExtensionManager getInstance() {
        return instance;
    }

    /**
     * Scans our EXTENSIONS_DIR looking for jar files containing classes that extend SnotesExtension.
     * All found classes will be instantiated and made available as extensions, enabled by default.
     */
    public void loadAll() {
        // Load our built-in extensions first:
        if (isTestExtensionRequired()) {
            addExtension(new TestExtension(), true);
        }

        // Now look for external extensions in jar files in our EXTENSIONS_DIR:
        try {
            loadExtensions(Version.EXTENSIONS_DIR,
                           SnotesExtension.class,
                           Version.NAME,     // Extensions must target this application name!
                           Version.VERSION); // Extensions must target our major version!
        }
        catch (LinkageError le) {
            // The parent class is pretty good about trapping errors that occur during extension load.
            // These are presented to the user on an "errors" tab that will be added automatically
            // to the ExtensionManagerDialog. For example, an extension may target an older version
            // of our application, or perhaps a malformed jar file was copied to our extensions dir.
            // But, just in case, let's have additional logging here.
            logger.log(Level.SEVERE, "One or more extensions could not be loaded.", le);
        }
    }

    /**
     * Queries all loaded extensions for any additional ActionGroups that should be displayed
     * in the main ActionPanel on the main window, and returns a combined list of all of them.
     */
    public List<ActionGroup> getActionGroups() {
        List<ActionGroup> groups = new ArrayList<>();
        for (SnotesExtension ext : getEnabledLoadedExtensions()) {
            List<ActionGroup> extGroups = ext.getActionGroups();
            if (extGroups != null && !extGroups.isEmpty()) {
                groups.addAll(extGroups);
            }
        }
        return groups;
    }

    /**
     * Returns a list of all actions provided by enabled extensions
     * for the given action group name.
     */
    public List<EnhancedAction> getExtraActions(String actionGroupName) {
        List<EnhancedAction> actions = new ArrayList<>();
        for (SnotesExtension ext : getEnabledLoadedExtensions()) {
            List<EnhancedAction> extActions = ext.getExtraActions(actionGroupName);
            if (extActions != null && !extActions.isEmpty()) {
                actions.addAll(extActions);
            }
        }
        return actions;
    }

    /**
     * If our version contains "SNAPSHOT", or if the undocumented system property
     * "snotes.enableTestExtension" is set to "true", we will install and
     * enable the built-int "TestExtension", which exercises all of the
     * abilities of a SnotesExtension.
     */
    private static boolean isTestExtensionRequired() {
        // Deliberately not documented anywhere, but -Dsnotes.enableTestExtension will enable our test extension
        String testExtProp = System.getProperty("snotes.enableTestExtension", null);
        return Version.getAboutInfo().applicationVersion.contains("SNAPSHOT") || testExtProp != null;
    }
}
