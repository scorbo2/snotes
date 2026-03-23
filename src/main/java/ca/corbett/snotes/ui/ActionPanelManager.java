package ca.corbett.snotes.ui;

import ca.corbett.extras.EnhancedAction;
import ca.corbett.extras.actionpanel.ActionPanel;
import ca.corbett.extras.actionpanel.ColorTheme;
import ca.corbett.snotes.extensions.SnotesExtensionManager;
import ca.corbett.snotes.ui.actions.ActionGroup;

import java.util.ArrayList;
import java.util.List;

public class ActionPanelManager {
    private final ActionPanel actionPanel;

    /**
     * If we don't put a cap on this, the ActionPanel will grow horizontally
     * without bounds, which might break our UI. So, let's put some reasonable
     * upper limit on action and group name length.
     */
    public static final int NAME_MAX_LENGTH = 30;

    public ActionPanelManager() {
        this.actionPanel = new ActionPanel();

        // One-time customization of ActionPanel:
        actionPanel.getColorOptions().setFromTheme(ColorTheme.DEFAULT); // TODO should be a config option; fine for now
        actionPanel.getActionGroupMargins().setAll(8).setInternalSpacing(12).setTop(12);
        actionPanel.setHeaderIconSize(20);
        actionPanel.getExpandCollapseOptions().setAllowHeaderDoubleClick(true);
    }

    /**
     * Returns our enclosed ActionPanel.
     */
    public ActionPanel getActionPanel() {
        return actionPanel;
    }

    /**
     * Invoke this when the manager is no longer needed.
     */
    public void dispose() {
        actionPanel.dispose();
    }

    /**
     * Forces a complete clear and reload of all actions.
     * Useful if extensions have been enabled/disabled/installed/uninstalled.
     */
    public void reload() {
        actionPanel.setAutoRebuildEnabled(false); // don't rebuild on each change below
        try {
            actionPanel.clear(true);
            List<ActionGroup> groups = new ArrayList<>();
            groups.add(ActionGroup.buildReadGroup());
            groups.add(ActionGroup.buildWriteGroup());
            groups.addAll(SnotesExtensionManager.getInstance().getActionGroups());
            groups.add(ActionGroup.buildOptionsGroup()); // Add Options group last for consistency
            for (ActionGroup group : groups) {
                for (EnhancedAction action : group.getActions()) {
                    action.setName(truncateIfNecessary(action.getName()));
                    actionPanel.add(truncateIfNecessary(group.getName()), action);
                }
                if (group.getIcon() != null) {
                    actionPanel.setGroupIcon(truncateIfNecessary(group.getName()), group.getIcon());
                }
            }
        }
        finally {
            // Re-enabling this triggers an automatic rebuild once for all changes above:
            actionPanel.setAutoRebuildEnabled(true);
        }
    }

    private String truncateIfNecessary(String input) {
        if (input.length() > NAME_MAX_LENGTH) {
            return input.substring(0, NAME_MAX_LENGTH - 3) + "...";
        }
        return input;
    }
}
