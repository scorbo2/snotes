package ca.corbett.snotes;

import ca.corbett.extensions.AppProperties;
import ca.corbett.extras.CustomizableDesktopPane;
import ca.corbett.extras.gradient.ColorSelectionType;
import ca.corbett.extras.gradient.Gradient;
import ca.corbett.extras.properties.AbstractProperty;
import ca.corbett.extras.properties.BooleanProperty;
import ca.corbett.extras.properties.ColorProperty;
import ca.corbett.extras.properties.DecimalProperty;
import ca.corbett.extras.properties.EnumProperty;
import ca.corbett.extras.properties.LookAndFeelProperty;
import ca.corbett.snotes.extensions.SnotesExtension;
import ca.corbett.snotes.extensions.SnotesExtensionManager;
import com.formdev.flatlaf.FlatLightLaf;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages application configuration properties for Snotes.
 * <p>
 * This class provides access to all configuration properties so
 * that code throughout the application can always retrieve the
 * latest settings. We also expose the PropertiesDialog and the
 * ExtensionManagerDialog through this class.
 * </p>
 * <p>
 * All configuration is stored in a file named "Snotes.props"
 * which lives in ${SETTINGS_DIR}. The settings directory lives
 * in the user's home directory by default, but can be overridden
 * by specifying the SETTINGS_DIR system property when launching
 * the application, as shown in this example:
 * </p>
 * <pre>java -DSETTINGS_DIR=/path/to/settings/dir -jar snotes.jar</pre>
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 * @since Snotes 2.0
 */
public class AppConfig extends AppProperties<SnotesExtension> {

    private static final String PROPS_FILE_NAME = "Snotes.props";
    public static final File PROPS_FILE = new File(Version.SETTINGS_DIR, PROPS_FILE_NAME);
    private static AppConfig instance = null;

    /**
     * Property name for enabling/disabling single-instance mode.
     * We expose this one because it's referenced elsewhere in the code.
     */
    public static final String SINGLE_INSTANCE_PROP = "UI.General.singleInstance";

    private BooleanProperty enableSingleInstance;
    private LookAndFeelProperty lookAndFeelProp;
    private DecimalProperty desktopLogoAlphaProp;
    private ColorProperty desktopGradientProp;
    private EnumProperty<CustomizableDesktopPane.LogoPlacement> desktopLogoPlacementProp;

    private AppConfig() {
        super(Version.FULL_NAME, PROPS_FILE, SnotesExtensionManager.getInstance());
    }

    public static AppConfig getInstance() {
        if (instance == null) {
            instance = new AppConfig();
        }
        return instance;
    }

    /**
     * We'll add a convenience wrapper around the static peek() method so that
     * our callers don't have to specify our props file each time.
     */
    public static String peek(String propName) {
        return AppProperties.peek(PROPS_FILE, propName);
    }

    public String getLookAndFeelClassName() {
        return lookAndFeelProp.getSelectedLafClass();
    }

    public float getDesktopLogoAlpha() {
        return (float)desktopLogoAlphaProp.getValue();
    }

    public void setDesktopLogoAlpha(float alpha) {
        desktopLogoAlphaProp.setValue(alpha);
    }

    public Gradient getDesktopGradient() {
        return desktopGradientProp.getGradient();
    }

    public void setDesktopGradient(Gradient gradient) {
        desktopGradientProp.setGradient(gradient);
    }

    public CustomizableDesktopPane.LogoPlacement getDesktopLogoPlacement() {
        return desktopLogoPlacementProp.getSelectedItem();
    }

    public void setDesktopLogoPlacement(CustomizableDesktopPane.LogoPlacement placement) {
        desktopLogoPlacementProp.setSelectedItem(placement);
    }

    @Override
    protected List<AbstractProperty> createInternalProperties() {
        List<AbstractProperty> props = new ArrayList<>();

        // We'll create a property to allow enabling/disabling single-instance mode:
        enableSingleInstance = new BooleanProperty(SINGLE_INSTANCE_PROP,
                                                   "Allow only a single instance of the application",
                                                   true);
        props.add(enableSingleInstance);

        // Look and feel stuff:
        lookAndFeelProp = new LookAndFeelProperty("UI.Look and Feel.Look and Feel", "Look and Feel:",
                                                  FlatLightLaf.class.getName());
        props.add(lookAndFeelProp);

        // Customizable desktop properties:
        desktopLogoAlphaProp = new DecimalProperty("UI.Desktop.Logo Alpha", "Desktop Logo Alpha:",
                                                   1.0, 0.1, 1.0, 0.1);
        desktopGradientProp = new ColorProperty("UI.Desktop.Gradient Color", "Desktop Gradient Color:",
                                                ColorSelectionType.GRADIENT)
            .setGradient(Gradient.createDefault()); // boring default... user can change it
        desktopLogoPlacementProp = new EnumProperty<>("UI.Desktop.Logo Placement", "Desktop Logo Placement:",
                                                            CustomizableDesktopPane.LogoPlacement.CENTER);
        props.add(desktopLogoAlphaProp);
        props.add(desktopGradientProp);
        props.add(desktopLogoPlacementProp);

        return props;
    }
}
