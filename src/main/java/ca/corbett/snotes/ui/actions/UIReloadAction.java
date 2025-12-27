package ca.corbett.snotes.ui.actions;

import ca.corbett.extras.LookAndFeelManager;
import ca.corbett.snotes.AppConfig;
import ca.corbett.snotes.ui.UIReloadable;

import javax.swing.AbstractAction;
import java.awt.event.ActionEvent;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

public class UIReloadAction extends AbstractAction {
    private static final Logger logger = Logger.getLogger(UIReloadAction.class.getName());
    private static final UIReloadAction instance = new UIReloadAction();
    private final Set<UIReloadable> reloadables = new HashSet<>();

    private UIReloadAction() {
    }

    public static UIReloadAction getInstance() {
        return instance;
    }

    public void registerReloadable(UIReloadable reloadable) {
        reloadables.add(reloadable);
    }

    public void unregisterReloadable(UIReloadable reloadable) {
        reloadables.remove(reloadable);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        logger.info("Reloading UI");

        // Change the look and feel:
        LookAndFeelManager.switchLaf(AppConfig.getInstance().getLookAndFeelClassName());

        // Notify all listeners:
        for (UIReloadable reloadable : reloadables) {
            reloadable.reloadUI();
        }
    }
}
