package ca.corbett.snotes.extensions.builtin;

import ca.corbett.extensions.AppExtensionInfo;
import ca.corbett.extras.properties.AbstractProperty;
import ca.corbett.snotes.Resources;
import ca.corbett.snotes.Version;
import ca.corbett.snotes.extensions.SnotesExtension;
import ca.corbett.snotes.ui.MainWindow;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
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

    @Override
    protected List<AbstractProperty> createConfigProperties() {
        return List.of(); // none yet
    }

    @Override
    protected void loadJarResources() {
        // none
    }

    public List<String> getExtraTaskPaneNames() {
        // We'll pretend we're supplying an extra task pane called "Test1":
        return List.of("Test1");
    }

    public ImageIcon getExtraTaskPaneIcon(String taskPaneName) {
        // Our icon should be used for our extra task pane, and it should be scaled to fit:
        return "Test1".equals(taskPaneName) ? new ImageIcon(Resources.getLogoIcon()) : null;
    }

    public List<Action> getTaskPaneActions(String taskPaneName) {
        // Return some dummy action for our test task pane:
        if ("Test1".equals(taskPaneName)) {
            Action dummyAction = new AbstractAction("Say hello") {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    JOptionPane.showMessageDialog(MainWindow.getInstance(), "Hello!");
                }
            };
            return List.of(dummyAction);
        }
        return List.of();
    }
}
