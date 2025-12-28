package ca.corbett.snotes.extensions;

import ca.corbett.extensions.AppExtension;

import javax.swing.Action;
import javax.swing.ImageIcon;
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
     * If the extension wishes to supply one or more additional task panes,
     * this method should return a list of names of those task panes.
     * Returning null or an empty list is fine, if there are no extra task panes.
     * <p>
     * <b>Note:</b> The names returned here will be used to identify the task panes
     * in other extension methods, such as getExtraTaskPaneIcon() and
     * getTaskPaneActions(). The supplied name may not conflict with one
     * of the built-in task pane names defined in TaskPaneBuilder.StandardTaskPanes.
     * If your extra task pane name conflicts with a built-in name, your
     * extension's task pane will be ignored.
     * </p>
     */
    public List<String> getExtraTaskPaneNames() {
        return List.of();
    }

    /**
     * If an extension supplies an extra task pane, it can also supply an icon to go
     * with that task pane by implementing this method. The given taskPaneName
     * will be one of the names returned by getExtraTaskPaneNames().
     * Returning null is fine, but your task pane will be assigned a default icon.
     */
    public ImageIcon getExtraTaskPaneIcon(String taskPaneName) {
        return null;
    }

    /**
     * Extensions can supply actions to be added to specific task panes
     * by implementing this method. The given taskPaneName will be either
     * one of the built-in task panes defined in TaskPaneBuilder.StandardTaskPanes,
     * or one of the names returned by getExtraTaskPaneNames().
     * Returning null or an empty list is fine.
     */
    public List<Action> getTaskPaneActions(String taskPaneName) {
        return List.of();
    }
}
