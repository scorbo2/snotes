package ca.corbett.snotes.extensions;

import ca.corbett.extensions.ExtensionManager;
import ca.corbett.snotes.Version;

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
        // TODO
    }

    public static SnotesExtensionManager getInstance() {
        return instance;
    }
}
