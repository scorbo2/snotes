package ca.corbett.snotes.ui;

import ca.corbett.extras.image.ImageUtil;
import ca.corbett.extras.properties.FileBasedProperties;
import ca.corbett.extras.properties.Properties;
import ca.corbett.snotes.AppConfig;
import ca.corbett.snotes.Resources;
import ca.corbett.snotes.extensions.SnotesExtensionManager;
import ca.corbett.snotes.ui.actions.AboutAction;
import ca.corbett.snotes.ui.actions.ExtensionManagerAction;
import ca.corbett.snotes.ui.actions.LogConsoleAction;
import ca.corbett.snotes.ui.actions.PrefsAction;
import org.jdesktop.swingx.JXTaskPane;

import javax.swing.Action;
import javax.swing.ImageIcon;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.logging.Logger;

/**
 * A utility class to build and return the various task panes used in the application.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 * @since Snotes 2.0
 */
public class TaskPaneBuilder {

    private static final Logger logger = Logger.getLogger(TaskPaneBuilder.class.getName());

    public static final int ICON_SIZE = 24;

    private TaskPaneBuilder() {
    }

    public enum StandardTaskPanes {
        READ("Read"),
        WRITE("Write"),
        OPTIONS("Options");

        private final String label;

        StandardTaskPanes(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }

        static StandardTaskPanes fromString(String label) {
            for (StandardTaskPanes pane : StandardTaskPanes.values()) {
                if (pane.label.equalsIgnoreCase(label)) {
                    return pane;
                }
            }
            return null;
        }
    }

    /**
     * Builds and returns the Read task pane.
     */
    public static JXTaskPane buildReadTaskPane() {
        final String collapsedProp = "taskPane.read.isCollapsed";
        Properties props = AppConfig.getInstance().getPropertiesManager().getPropertiesInstance();

        JXTaskPane taskPane = new JXTaskPane();
        taskPane.setCollapsed(props.getBoolean(collapsedProp, false));
        taskPane.setFocusable(false);
        taskPane.addPropertyChangeListener(new TaskPaneExpandListener(props, collapsedProp));
        taskPane.setIcon(Resources.getIconRead());
        taskPane.setTitle(StandardTaskPanes.READ.toString());

        rebuildReadTaskPane(taskPane);
        return taskPane;
    }

    /**
     * Clears the given task pane and adds all "read"-related actions.
     * This can be invoked after initial creation to refresh the contents
     * of the task pane, if saved queries have been modified, or if extensions
     * have been added/removed/enabled/disabled.
     */
    public static void rebuildReadTaskPane(JXTaskPane taskPane) {
        taskPane.removeAll();

        // Add all the actions that come baked into the application:
        //taskPane.add(new SearchAction());
        //taskPane.add(new TagFrameAction());
        //taskPane.add(new PasswordManagementAction());

        // Load all stored queries from preferences:
        // TODO implement this
//        for (String name : AppPreferences.getInstalledQueryNames()) {
//            Query query = new Query(name);
//            query.loadFromProps(AppPreferences.getQueryProperties(name), "");
//            taskPane.add(new QueryLaunchAction(query));
//        }

        // Now ask extensions to contribute any read-related actions:
        for (Action action : SnotesExtensionManager.getInstance().getTaskPaneActions(StandardTaskPanes.READ.toString())) {
            taskPane.add(action);
        }
    }

    /**
     * Builds and returns the Write task pane.
     */
    public static JXTaskPane buildWriteTaskPane() {
        final String collapsedProp = "taskPane.write.isCollapsed";
        Properties props = AppConfig.getInstance().getPropertiesManager().getPropertiesInstance();

        JXTaskPane taskPane = new JXTaskPane();
        taskPane.setCollapsed(props.getBoolean(collapsedProp, false));
        taskPane.setFocusable(false);
        taskPane.addPropertyChangeListener(new TaskPaneExpandListener(props, collapsedProp));
        taskPane.setIcon(Resources.getIconWrite());
        taskPane.setTitle("Write");

        rebuildWriteTaskPane(taskPane);
        return taskPane;
    }

    /**
     * Clears the given task pane and adds all "write"-related actions.
     * This can be invoked after initial creation to refresh the contents
     * of the task pane, if saved queries have been modified, or if extensions
     * have been added/removed/enabled/disabled.
     */
    public static void rebuildWriteTaskPane(JXTaskPane taskPane) {
        taskPane.removeAll();

        // Add all the write actions that come baked into the application:
        //taskPane.add(new NewSnoteAction());
        //taskPane.add(new NewPasswordAction());

        // Add all write templates from preferences:
        // TODO implement this
//        for (String name : AppPreferences.getInstalledTemplateNames()) {
//            Template template = new Template(name);
//            template.loadFromProps(AppPreferences.getTemplateProperties(name), "");
//            taskPane.add(new TemplateLaunchAction(template));
//        }

        // Now ask extensions to contribute any write-related actions:
        for (Action action : SnotesExtensionManager.getInstance().getTaskPaneActions(StandardTaskPanes.WRITE.toString())) {
            taskPane.add(action);
        }
    }

    /**
     * Builds and returns the Options task pane.
     */
    public static JXTaskPane buildOptionsTaskPane() {
        final String collapsedProp = "taskPane.options.isCollapsed";
        Properties props = AppConfig.getInstance().getPropertiesManager().getPropertiesInstance();

        JXTaskPane taskPane = new JXTaskPane();
        taskPane.setCollapsed(props.getBoolean(collapsedProp, false));
        taskPane.setFocusable(false);
        taskPane.addPropertyChangeListener(new TaskPaneExpandListener(props, collapsedProp));
        taskPane.setIcon(Resources.getIconOptions());
        taskPane.setTitle("Options");

        rebuildOptionsTaskPane(taskPane);
        return taskPane;
    }

    /**
     * Clears the given task pane and adds all "options"-related actions.
     * This can be invoked after initial creation to refresh the contents
     * of the task pane, if extensions have been added/removed/enabled/disabled.
     */
    public static void rebuildOptionsTaskPane(JXTaskPane taskPane) {
        taskPane.removeAll();

        // Add all the options actions that come baked into the application:
        taskPane.add(new PrefsAction());
        taskPane.add(new ExtensionManagerAction());
        taskPane.add(new LogConsoleAction());
        taskPane.add(new AboutAction());

        // Now ask extensions to contribute any options-related actions:
        for (Action action : SnotesExtensionManager.getInstance().getTaskPaneActions(StandardTaskPanes.OPTIONS.toString())) {
            taskPane.add(action);
        }
    }

    /**
     * Returns a list of extra task panes supplied by extensions, if any.
     */
    public static List<JXTaskPane> buildExtensionTaskPanes() {
        SnotesExtensionManager extManager = SnotesExtensionManager.getInstance();
        List<JXTaskPane> panes = new java.util.ArrayList<>();

        for (String paneName : extManager.getExtraTaskPaneNames()) {

            // Skip any names that conflict with our standard panes:
            if (StandardTaskPanes.fromString(paneName) != null) {
                logger.warning("Ignoring extension task pane name that conflicts with standard pane: " + paneName);
                continue;
            }


            final String collapsedProp = "taskPane." + paneName.toLowerCase() + ".isCollapsed";
            Properties props = AppConfig.getInstance().getPropertiesManager().getPropertiesInstance();

            JXTaskPane taskPane = new JXTaskPane();
            taskPane.setCollapsed(props.getBoolean(collapsedProp, false));
            taskPane.setFocusable(false);
            taskPane.addPropertyChangeListener(new TaskPaneExpandListener(props, collapsedProp));
            taskPane.setIcon(getExtensionTaskPaneIcon(paneName));
            taskPane.setTitle(paneName);

            // Populate with actions from extensions:
            List<Action> actions = SnotesExtensionManager.getInstance().getTaskPaneActions(paneName);
            for (Action action : actions) {
                taskPane.add(action);
            }

            panes.add(taskPane);
        }

        return panes;
    }

    /**
     * Will attempt to get the icon for the given extension task pane name,
     * if any extension supplies one. A default will be provided if none is found.
     * All icons will be scaled to the standard task pane icon size.
     */
    private static ImageIcon getExtensionTaskPaneIcon(String taskPaneName) {
        ImageIcon icon = SnotesExtensionManager.getInstance().getTaskPaneIcon(taskPaneName);
        if (icon == null) {
            icon = Resources.getIconOptions(); // default icon
        }
        int width = icon.getImage().getWidth(null);
        int height = icon.getImage().getHeight(null);
        if (width != ICON_SIZE || height != ICON_SIZE) {
            BufferedImage buf = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            buf.getGraphics().drawImage(icon.getImage(), 0, 0, null);
            icon = new ImageIcon(ImageUtil.generateThumbnailWithTransparency(buf, ICON_SIZE, ICON_SIZE));
            buf.flush();
        }
        return icon;
    }

    /**
     * This PropertyChangeListener will listen for expand/collapse events
     * on a task pane, and will store the new state in the application
     * properties as a hidden property.
     */
    private static class TaskPaneExpandListener implements PropertyChangeListener {

        private final Properties props;
        private final String propertyName;

        public TaskPaneExpandListener(Properties props, String propertyName) {
            this.props = props;
            this.propertyName = propertyName;
        }

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            // We only care about changes to the collapsed state:
            if (!evt.getPropertyName().equals("collapsed")) {
                return;
            }

            // Do nothing if our property name is not set:
            if (propertyName == null || propertyName.trim().isEmpty()) {
                return;
            }

            // Update preferences with our new collapse state:
            props.setBoolean(propertyName, (Boolean)evt.getNewValue());
            if (props instanceof FileBasedProperties) {
                ((FileBasedProperties)props).saveWithoutException(); // save as we go
            }
        }
    }
}
