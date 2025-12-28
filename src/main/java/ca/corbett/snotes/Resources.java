package ca.corbett.snotes;

import ca.corbett.extras.image.ImageUtil;

import javax.swing.ImageIcon;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.logging.Logger;

/**
 * A utility class for managing access to images and other resources.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 * @since Snotes 2.0
 */
public class Resources {

    private static final Logger logger = Logger.getLogger(Resources.class.getName());

    private static Image logoIcon;
    private static BufferedImage logoWide;
    private static ImageIcon iconRead;
    private static ImageIcon iconWrite;
    private static ImageIcon iconOptions;

    private Resources() {
    }

    public static boolean loadAll() {
        // If we've already loaded, don't do it again:
        if (logoIcon != null && logoWide != null) {
            return true;
        }

        // Make sure all our expected URLs resolve:
        URL logoWideUrl = Resources.class.getResource("/ca/corbett/snotes/images/logo_wide.jpg");
        URL logoIconUrl = Resources.class.getResource("/ca/corbett/snotes/images/logo.png");
        URL iconReadUrl = Resources.class.getResource("/ca/corbett/snotes/images/icon-read-24.png");
        URL iconWriteUrl = Resources.class.getResource("/ca/corbett/snotes/images/icon-write-24.png");
        URL iconOptionsUrl = Resources.class.getResource("/ca/corbett/snotes/images/icon-options-24.png");
        if (logoWideUrl == null || logoIconUrl == null || iconReadUrl == null
            || iconWriteUrl == null || iconOptionsUrl == null) {
            logger.severe("Failed to load one or more resource images.");
            return false;
        }

        // Now load them:
        try {
            logoIcon = Toolkit.getDefaultToolkit().createImage(logoIconUrl);
            logoWide = ImageUtil.loadImage(logoWideUrl);
            iconRead = new ImageIcon(iconReadUrl);
            iconWrite = new ImageIcon(iconWriteUrl);
            iconOptions = new ImageIcon(iconOptionsUrl);
        }
        catch (Exception e) {
            logger.severe("Failed to load one or more resource images: " + e.getMessage());
            return false;
        }

        return true;
    }

    /**
     * Returns a small, square icon version of the Snotes logo, suitable for window icons or such.
     */
    public static Image getLogoIcon() {
        return logoIcon;
    }

    /**
     * Returns a wider image with the full application name, suitable for banners or such.
     */
    public static BufferedImage getLogoWide() {
        return logoWide;
    }

    /**
     * Returns the "read" icon.
     */
    public static ImageIcon getIconRead() {
        return iconRead;
    }

    /**
     * Returns the "write" icon.
     */
    public static ImageIcon getIconWrite() {
        return iconWrite;
    }

    /**
     * Returns the "options" icon.
     */
    public static ImageIcon getIconOptions() {
        return iconOptions;
    }
}
