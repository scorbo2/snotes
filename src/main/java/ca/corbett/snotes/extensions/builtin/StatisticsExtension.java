package ca.corbett.snotes.extensions.builtin;

import ca.corbett.extensions.AppExtensionInfo;
import ca.corbett.extras.EnhancedAction;
import ca.corbett.extras.properties.AbstractProperty;
import ca.corbett.snotes.Resources;
import ca.corbett.snotes.Version;
import ca.corbett.snotes.extensions.SnotesExtension;
import ca.corbett.snotes.ui.actions.ActionGroup;

import java.awt.event.ActionEvent;
import java.util.List;

/**
 * An optional built-in extension for Snotes that can gather and display
 * statistics about the user's notes.
 * <ul>
 *     <li>Word frequency - what are the most frequently used words?</li>
 *     <li>Phrase frequency - what are the most frequently used phrases?</li>
 *     <li>Note length distribution - how long are the user's notes over time?</li>
 * </ul>
 * <p>
 *     All statistics above can be gathered either for a specific time period,
 *     or from all notes. Word and phrase frequency can be configured to ignore
 *     common words ("a", "the", "an", etc.)
 * </p>
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 * @since Snotes 2.0
 */
public class StatisticsExtension extends SnotesExtension {

    private final AppExtensionInfo extInfo;

    public StatisticsExtension() {
        extInfo = new AppExtensionInfo.Builder("Statistics")
            .setAuthor("Steve Corbett")
            .setTargetAppName(Version.NAME)
            .setTargetAppVersion(Version.VERSION)
            .setVersion(Version.VERSION)
            .setShortDescription("Gathers and displays statistics about your notes.")
            .setLongDescription("This extension gathers and displays various statistics about your notes, " +
                                    "such as word frequency, phrase frequency, and note length distribution.")
            .build();
    }

    @Override
    public AppExtensionInfo getInfo() {
        return extInfo;
    }

    @Override
    protected List<AbstractProperty> createConfigProperties() {
        // Currently none
        return List.of();
    }

    @Override
    protected void loadJarResources() {
        // Currently none
    }

    /**
     * We supply one extra action group with some dummy actions in it.
     */
    @Override
    public List<ActionGroup> getActionGroups() {
        ActionGroup group = new ActionGroup("Statistics", Resources.getIconStats());
        group.addAction(new StatsDialogAction());
        return List.of(group);
    }

    private static class StatsDialogAction extends EnhancedAction {

        public StatsDialogAction() {
            super("Statistics dialog");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            new StatisticsDialog().setVisible(true);
        }
    }
}
