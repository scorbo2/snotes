package ca.corbett.snotes.extensions.builtin;

import ca.corbett.extensions.AppExtensionInfo;
import ca.corbett.extras.EnhancedAction;
import ca.corbett.extras.properties.AbstractProperty;
import ca.corbett.extras.properties.BooleanProperty;
import ca.corbett.extras.properties.LabelProperty;
import ca.corbett.snotes.Version;
import ca.corbett.snotes.extensions.SnotesExtension;
import ca.corbett.snotes.ui.MainWindow;
import ca.corbett.snotes.ui.actions.ActionGroup;

import javax.swing.JOptionPane;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * This is a test extension used for development and testing purposes.
 * TODO remove or hide this extension before release.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 * @since Snotes 2.0
 */
public class TestExtension extends SnotesExtension {
    private final AppExtensionInfo extInfo;

    private static final String TASK_PANE_NAME = "Test1";

    public TestExtension() {
        extInfo = new AppExtensionInfo.Builder("Test extension")
            .setAuthor("Steve Corbett")
            .setTargetAppName(Version.NAME)
            .setTargetAppVersion(Version.VERSION)
            .setVersion(Version.VERSION)
            .setShortDescription("Test extension")
            .setLongDescription("Just a test of the Snotes extension system.")
            .build();
    }

    @Override
    public AppExtensionInfo getInfo() {
        return extInfo;
    }

    /**
     * We'll supply some config properties that don't actually do anything,
     * just to test out extension integration with the application properties dialog.
     */
    @Override
    protected List<AbstractProperty> createConfigProperties() {
        List<AbstractProperty> props = new ArrayList<>();
        props.add(new BooleanProperty("TestExtension.Options.checkbox1", "Enable feature 1", false));
        props.add(new BooleanProperty("TestExtension.Options.checkbox2", "Enable feature 2", true));
        props.add(new LabelProperty("TestExtension.Note.label1",
                                    "Testing properties integration - these properties do nothing."));
        return props;
    }

    @Override
    protected void loadJarResources() {
        // none
    }

    /**
     * We supply one extra action group with some dummy actions in it.
     */
    @Override
    public List<ActionGroup> getActionGroups() {
        ActionGroup group = new ActionGroup(TASK_PANE_NAME, null);
        for (EnhancedAction action : getExtraActions(TASK_PANE_NAME)) {
            group.addAction(action);
        }
        return List.of(group);
    }

    /**
     * We supply a couple of dummy actions for our extra task pane,
     * as well as some actions for the built-in task panes.
     */
    @Override
    public List<EnhancedAction> getExtraActions(String actionGroupName) {
        // Return some dummy action for our test task pane:
        if (TASK_PANE_NAME.equals(actionGroupName)) {
            return List.of(
                new DummyAction("Say hello", "Hello from the test extension!"),
                new DummyAction("Show version", "Snotes version: " + Version.VERSION)
            );
        }

        else if ("read".equalsIgnoreCase(actionGroupName)) {
            return List.of(
                new DummyAction("Read action", "This is a dummy action added to the Read task pane.")
            );
        }

        else if ("write".equalsIgnoreCase(actionGroupName)) {
            return List.of(
                new DummyAction("Write action", "This is a dummy action added to the Write task pane.")
            );
        }

        else if ("options".equalsIgnoreCase(actionGroupName)) {
            return List.of(
                new DummyAction("Options action", "This is a dummy action added to the Options task pane.")
            );
        }

        return List.of();
    }

    /**
     * Invoked internally to attach a simple action to anything that might
     * require an action. This action just shows a message dialog with
     * a supplied message when invoked.
     */
    private static class DummyAction extends EnhancedAction {

        private final String message;

        public DummyAction(String name, String message) {
            super(name);
            this.message = message;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            JOptionPane.showMessageDialog(MainWindow.getInstance(), message);
        }
    }
}
