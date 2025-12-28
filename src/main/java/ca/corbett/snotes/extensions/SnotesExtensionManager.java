package ca.corbett.snotes.extensions;

import ca.corbett.extensions.ExtensionManager;
import ca.corbett.snotes.Version;
import ca.corbett.snotes.extensions.builtin.TestExtension;

import javax.swing.Action;
import javax.swing.ImageIcon;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
        // Let's try to scan for dynamically loaded extensions:
        loadExtensions(Version.EXTENSIONS_DIR, SnotesExtension.class, Version.NAME, Version.VERSION);

        // Add our built-in extensions here:
        addExtension(new TestExtension(), true);
    }

    public static SnotesExtensionManager getInstance() {
        return instance;
    }

    /**
     * Returns a sorted list of unique names of all extra task panes
     * provided by enabled extensions.
     */
    public List<String> getExtraTaskPaneNames() {
        Set<String> paneNames = new HashSet<>(); // strip duplicates
        for (SnotesExtension ext : getEnabledLoadedExtensions()) {
            List<String> extPaneNames = ext.getExtraTaskPaneNames();
            if (extPaneNames != null && !extPaneNames.isEmpty()) {
                paneNames.addAll(extPaneNames);
            }
        }

        // Sort and return:
        return paneNames.stream().sorted().toList();
    }

    /**
     * Returns a list of all actions provided by enabled extensions
     * for the given task pane name.
     */
    public List<Action> getTaskPaneActions(String taskPaneName) {
        List<Action> actions = new ArrayList<>();
        for (SnotesExtension ext : getEnabledLoadedExtensions()) {
            List<Action> extActions = ext.getTaskPaneActions(taskPaneName);
            if (extActions != null && !extActions.isEmpty()) {
                actions.addAll(extActions);
            }
        }
        return actions;
    }

    /**
     * Returns the icon provided by the first enabled extension
     * that has one for the given task pane name.
     */
    public ImageIcon getTaskPaneIcon(String taskPaneName) {
        for (SnotesExtension ext : getEnabledLoadedExtensions()) {
            ImageIcon icon = ext.getExtraTaskPaneIcon(taskPaneName);
            if (icon != null) {
                return icon; // return the first one we find for this task pane name
            }
        }
        return null;
    }
}
