package ca.corbett.snotes.ui.actions;

import ca.corbett.extras.EnhancedAction;
import ca.corbett.snotes.AppConfig;
import ca.corbett.snotes.Resources;
import ca.corbett.snotes.extensions.SnotesExtensionManager;
import ca.corbett.snotes.io.DataManager;
import ca.corbett.snotes.model.Query;
import ca.corbett.snotes.ui.MainWindow;

import javax.swing.Icon;
import java.util.ArrayList;
import java.util.List;

/**
 * A simple wrapper class for a group of related actions with an optional icon.
 * The application supplies some built-in action groups, and extensions can
 * supply additional groups. These are displayed in the main ActionPanel
 * on the main window.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 * @since Snotes 2.0
 */
public class ActionGroup {

    // Our three built-in ActionGroups:
    public static final String READ = "Read";
    public static final String WRITE = "Write";
    public static final String OPTIONS = "Options";

    private final String name;
    private final List<EnhancedAction> actions;
    private final Icon icon;

    /**
     * Creates a named ActionGroup with no icon.
     */
    public ActionGroup(String name) {
        this(name, null);
    }

    /**
     * Creates a named ActionGroup with the given icon.
     * The icon can be null, but the name cannot be null or blank.
     */
    public ActionGroup(String name, Icon icon) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name cannot be null or blank");
        }
        this.name = name;
        this.icon = icon;
        this.actions = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public Icon getIcon() {
        return icon;
    }

    public int size() {
        return actions.size();
    }

    public boolean isEmpty() {
        return actions.isEmpty();
    }

    public void clear() {
        actions.clear();
    }

    /**
     * Adds the given action to this ActionGroup. The action cannot be null.
     */
    public ActionGroup addAction(EnhancedAction action) {
        if (action == null) {
            throw new IllegalArgumentException("action cannot be null");
        }
        actions.add(action);
        return this;
    }

    /**
     * Returns a copy of the list of actions in this ActionGroup.
     * Modifying the returned list will not affect the contents of this ActionGroup.
     */
    public List<EnhancedAction> getActions() {
        return new ArrayList<>(actions);
    }

    public static ActionGroup buildReadGroup() {
        List<EnhancedAction> queryActions = new ArrayList<>();

        // Query CRUD actions:
        queryActions.add(new NewQueryAction());
        queryActions.add(new ManageQueriesAction());

        // Show all Query instances as action links. Clicking one executes the Query!
        DataManager dataManager = MainWindow.getInstance().getDataManager();
        List<Query> savedQueries = dataManager.getQueries();
        for (Query query : savedQueries) {
            queryActions.add(new ExecuteQueryAction(query));
        }

        return buildGroup(ActionGroup.READ, Resources.getIconRead(), queryActions);
    }

    public static ActionGroup buildWriteGroup() {
        List<EnhancedAction> writeActions = new ArrayList<>();
        writeActions.add(AppConfig.getInstance().getNewNoteAction());
        // TODO Template actions go here.
        return buildGroup(WRITE, Resources.getIconWrite(), writeActions);
    }

    public static ActionGroup buildOptionsGroup() {
        List<EnhancedAction> optionActions = new ArrayList<>();
        optionActions.add(AppConfig.getInstance().getLogConsoleAction());
        optionActions.add(AppConfig.getInstance().getPreferencesAction());
        optionActions.add(AppConfig.getInstance().getExtensionManagerAction());
        ActionGroup group = buildGroup(OPTIONS, Resources.getIconOptions(), optionActions);
        group.addAction(AppConfig.getInstance().getAboutAction()); // This one always goes at the end
        return group;
    }

    private static ActionGroup buildGroup(String name, Icon icon, List<EnhancedAction> actions) {
        ActionGroup group = new ActionGroup(name, icon);
        for (EnhancedAction action : actions) {
            group.addAction(action);
        }

        // Query extensions for additional actions for this group:
        List<EnhancedAction> extraReadActions = SnotesExtensionManager.getInstance().getExtraActions(name);
        if (extraReadActions != null && !extraReadActions.isEmpty()) {
            for (EnhancedAction action : extraReadActions) {
                group.addAction(action);
            }
        }

        return group;
    }
}
