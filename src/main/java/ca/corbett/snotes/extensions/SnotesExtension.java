package ca.corbett.snotes.extensions;

import ca.corbett.extensions.AppExtension;
import ca.corbett.extras.EnhancedAction;
import ca.corbett.snotes.ui.actions.ActionGroup;

import java.util.List;

/**
 * This is the starting point for extensions within the Snotes app.
 * Extend this class and implement whichever method(s) are relevant
 * to your app, then bundle your code up into a jar file and put it
 * in the extension directory and your extension will automatically
 * be picked up when Snotes is restarted.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 * @since Snotes 2.0
 */
public abstract class SnotesExtension extends AppExtension {

    /**
     * Extensions can optionally supply a list of ActionGroups to be added
     * to the main ActionPanel on the main window. Returning null or an empty list is fine.
     * The returned ActionGroups should be populated with whatever actions the
     * extension wants to add to the UI.
     */
    public List<ActionGroup> getActionGroups() {
        return List.of();
    }

    /**
     * Extensions can supply actions to be added to the built-in ActionGroups.
     * The given group name will be one of ActionGroup.READ, ActionGroup.WRITE,
     * or ActionGroup.OPTIONS. Extensions that have actions that don't belong
     * in one of the three built-in groups must supply their
     * own action group(s) via getActionGroups(). This method will ONLY
     * be invoked for the application's built-in action groups.
     * <p>
     * Returning null or an empty list here is fine.
     * </p>
     */
    public List<EnhancedAction> getExtraActions(String actionGroupName) {
        return List.of();
    }
}
