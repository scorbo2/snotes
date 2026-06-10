package ca.corbett.snotes.ui.actions;

import ca.corbett.extras.EnhancedAction;
import ca.corbett.snotes.AppConfig;

import java.awt.event.ActionEvent;
import java.util.logging.Logger;

/**
 * An action that can be used to quickly adjust the font size in all edit panes up or down.
 * The idea was stolen from the CryptText application, but this is a slightly different implementation.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
public class FontSizeQuickChangeAction extends EnhancedAction {

    private static final Logger log = Logger.getLogger(FontSizeQuickChangeAction.class.getName());

    private final int increment;

    public FontSizeQuickChangeAction(int increment) {
        super(increment > 0 ? "Increase Font Size" : "Decrease Font Size");
        this.increment = increment;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // Handle the wonky case first:
        if (increment == 0) {
             log.warning("FontSizeQuickChangeAction created with increment of 0. This action will do nothing.");
             return;
        }

        // There are two font sizes: the tag font and the text font.
        // It would be nice to change them proportionally, as the tag font is usually just
        // slightly larger than the text font, and that difference would grow as the
        // font sizes increase. But for now, we will just change them both
        // by the same fixed increment. The difference between them is small enough
        // that this approach should be fine.
        AppConfig.getInstance().changeFontSizes(increment);

        // Trigger a UI reload to force all open edit panes to update their font sizes.
        UIReloadAction.getInstance().actionPerformed(null);
    }
}
