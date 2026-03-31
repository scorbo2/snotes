package ca.corbett.snotes;

import ca.corbett.snotes.ui.MainWindow;
import ca.corbett.updates.UpdateManager;
import ca.corbett.updates.UpdateSources;

/**
 * Issue <a href="https://github.com/scorbo2/swing-extras/issues/433">#433</a> in the swing-extras
 * library describes a problem that can occur if an application registers a shutdown hook
 * that needs to do UI cleanup. In our case, if our user tries to close the application with
 * unsaved changes in any internal frame, we show a popup dialog asking to save or discard changes.
 * This is a problem, because the current implementation of UpdateManager blocks the EDT during
 * application restarts. This leads to deadlocks when Snotes tries to restart itself.
 * <p>
 * Until the bug is fixed upstream, here is our workaround: extend the UpdateManager
 * class and override the restartApplication() method to do our UI cleanup BEFORE calling
 * the superclass method. Then, we avoid registering a shutdown hook at all, and we
 * avoid deadlocks, while retaining the benefit of being able to restart as needed.
 * When the bug is patched upstream, we can simply remove this class.
 * </p>
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 * @since Snotes 2.0
 */
public class SnotesUpdateManager extends UpdateManager {

    public SnotesUpdateManager(UpdateSources sources) {
        super(sources);

        // Register a dummy shutdown hook to avoid the "you have no shutdown hooks registered!" warning.
        registerShutdownHook(() -> {
            // Deliberate no-op. We'll handle cleanup ourselves.
        });
    }

    @Override
    public void restartApplication() {
        // Because this method is invoked on the EDT, we can safely invoke
        // our cleanup method here. We can prompt the user, close all internal
        // frames, and whatever else we need to do.
        MainWindow.getInstance().cleanup();

        // Then, once we're cleaned up, we can let UpdateManager handle the restart:
        super.restartApplication();
    }
}
