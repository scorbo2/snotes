package ca.corbett.snotes.ui;

import ca.corbett.extras.CustomizableDesktopPane;
import ca.corbett.extras.logging.LogConsole;
import ca.corbett.snotes.AppConfig;
import ca.corbett.snotes.Resources;
import ca.corbett.snotes.Version;
import ca.corbett.snotes.ui.actions.UIReloadAction;
import org.jdesktop.swingx.JXTaskPane;
import org.jdesktop.swingx.JXTaskPaneContainer;

import javax.swing.JFrame;
import javax.swing.JSplitPane;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * The main window for the Snotes application.
 * TODO this needs a lot of work...
 *
 * @author scorbo2
 */
public class MainWindow extends JFrame implements UIReloadable {

    private static final Logger logger = Logger.getLogger(MainWindow.class.getName());
    private static final MainWindow instance = new MainWindow();

    private CustomizableDesktopPane desktopPane;
    private JXTaskPane readTaskPane;
    private JXTaskPane writeTaskPane;
    private JXTaskPane optionsTaskPane;

    private MainWindow() {
        super(Version.FULL_NAME);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setMinimumSize(new Dimension(500, 400));
        initComponents();
    }

    public static MainWindow getInstance() {
        return instance;
    }

    /**
     * Overridden here so we can do some initialization when the
     * window is first shown.
     */
    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (visible) {
            instance.setIconImage(Resources.getLogoIcon());
            LogConsole.getInstance().setIconImage(Resources.getLogoIcon());
            UIReloadAction.getInstance().registerReloadable(this);
        }
    }

    public void processStartArgs(java.util.List<String> args) {
        // TODO implement argument processing
    }

    private void initComponents() {
        desktopPane = new CustomizableDesktopPane(Resources.getLogoWide(),
                                                  AppConfig.getInstance().getDesktopLogoPlacement(),
                                                  AppConfig.getInstance().getDesktopLogoAlpha(),
                                                  AppConfig.getInstance().getDesktopGradient());

        JSplitPane splitPane = new JSplitPane();
        splitPane.setOneTouchExpandable(false); // Sadly, this does not play well with some look and feels
        splitPane.setLeftComponent(buildTaskPaneContainer());
        splitPane.setRightComponent(desktopPane);
        splitPane.setDividerLocation(0.25);

        setLayout(new BorderLayout());
        add(splitPane, BorderLayout.CENTER);
    }

    /**
     * There are three built-in task panes defined by the application:
     * Read, Write, and Options. In addition, any enabled extensions
     * may provide extra task panes to be included here.
     */
    private JXTaskPaneContainer buildTaskPaneContainer() {
        readTaskPane = TaskPaneBuilder.buildReadTaskPane();
        writeTaskPane = TaskPaneBuilder.buildWriteTaskPane();
        optionsTaskPane = TaskPaneBuilder.buildOptionsTaskPane();

        JXTaskPaneContainer container = new JXTaskPaneContainer();
        container.add(readTaskPane);
        container.add(writeTaskPane);
        for (JXTaskPane extraTaskPane : TaskPaneBuilder.buildExtensionTaskPanes()) {
            container.add(extraTaskPane);
        }
        container.add(optionsTaskPane);

        return container;
    }

    /**
     * Invoked when the application preferences dialog or the extension manager dialog
     * have been okayed, and we need to reload the UI to reflect any changes.
     */
    @Override
    public void reloadUI() {
        // Update our desktop pane with new settings:
        desktopPane.setLogoImageTransparency(AppConfig.getInstance().getDesktopLogoAlpha());
        desktopPane.setGradientConfig(AppConfig.getInstance().getDesktopGradient());
        desktopPane.setLogoImagePlacement(AppConfig.getInstance().getDesktopLogoPlacement());
        desktopPane.repaint();

        // Rebuild our built-in tasks panes, as their content may change:
        TaskPaneBuilder.rebuildReadTaskPane(readTaskPane);
        TaskPaneBuilder.rebuildWriteTaskPane(writeTaskPane);
        TaskPaneBuilder.rebuildOptionsTaskPane(optionsTaskPane);

        // Remove any extension task panes:
        // (these may have changed too drastically to surgically rebuild them, so just nuke and pave)
        List<JXTaskPane> toRemove = new ArrayList<>();
        for (Component c : desktopPane.getComponents()) {
            if (c instanceof JXTaskPaneContainer) {
                JXTaskPane pane = (JXTaskPane)c;
                TaskPaneBuilder.StandardTaskPanes paneType = TaskPaneBuilder.StandardTaskPanes.fromString(
                    pane.getTitle());
                if (paneType != null) {
                    toRemove.add(pane);
                }
            }
        }
        for (JXTaskPane pane : toRemove) {
            desktopPane.remove(pane);
        }

        // Now build any new extension task panes:
        // Be sure to add them before the Options pane, which is always last:
        for (JXTaskPane extraTaskPane : TaskPaneBuilder.buildExtensionTaskPanes()) {
            desktopPane.add(extraTaskPane, desktopPane.getComponentCount() - 1);
        }
    }
}
