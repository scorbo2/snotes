package ca.corbett.snotes;

import ca.corbett.extensions.AppProperties;
import ca.corbett.extras.CustomizableDesktopPane;
import ca.corbett.extras.EnhancedAction;
import ca.corbett.extras.gradient.ColorSelectionType;
import ca.corbett.extras.gradient.Gradient;
import ca.corbett.extras.io.KeyStrokeManager;
import ca.corbett.extras.properties.AbstractProperty;
import ca.corbett.extras.properties.BooleanProperty;
import ca.corbett.extras.properties.ColorProperty;
import ca.corbett.extras.properties.DecimalProperty;
import ca.corbett.extras.properties.EnumProperty;
import ca.corbett.extras.properties.KeyStrokeProperty;
import ca.corbett.extras.properties.LookAndFeelProperty;
import ca.corbett.snotes.extensions.SnotesExtension;
import ca.corbett.snotes.extensions.SnotesExtensionManager;
import ca.corbett.snotes.ui.actions.AboutAction;
import ca.corbett.snotes.ui.actions.ExitAction;
import ca.corbett.snotes.ui.actions.ExtensionManagerAction;
import ca.corbett.snotes.ui.actions.LogConsoleAction;
import ca.corbett.snotes.ui.actions.NewNoteAction;
import ca.corbett.snotes.ui.actions.PrefsAction;
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

    /**
     * Extensions can use this prefix when defining their own keystroke properties,
     * so that they show up on the same properties dialog tab as the other ones.
     * This is optional! Extensions can opt to keep all of their properties
     * on their own separate tab if they prefer.
     * <p>
     * Suggested format: KEYSTROKE_PREFIX + ExtensionUserFriendlyName + "." + ActionName
     * </p>
     */
    public static final String KEYSTROKE_PREFIX = "Keystrokes.";

    public static final String KEY_ABOUT = KEYSTROKE_PREFIX + "General.about";
    public static final String KEY_EXT_MANAGER = KEYSTROKE_PREFIX + "General.extensionManager";
    public static final String KEY_LOG_CONSOLE = KEYSTROKE_PREFIX + "General.logConsole";
    public static final String KEY_NEW_NOTE = KEYSTROKE_PREFIX + "General.newNote";
    public static final String KEY_PREFERENCES = KEYSTROKE_PREFIX + "General.preferences";
    public static final String KEY_EXIT = KEYSTROKE_PREFIX + "General.exit";

    // We centralize these here so that KeyStrokeManager can handle updating
    // their keyboard accelerators when the user changes them in the properties dialog:
    private EnhancedAction aboutAction;
    private EnhancedAction extensionManagerAction;
    private EnhancedAction logConsoleAction;
    private EnhancedAction newNoteAction;
    private EnhancedAction preferencesAction;
    private EnhancedAction exitAction;

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

    public EnhancedAction getAboutAction() {
        return aboutAction;
    }

    public EnhancedAction getExtensionManagerAction() {
        return extensionManagerAction;
    }

    public EnhancedAction getLogConsoleAction() {
        return logConsoleAction;
    }

    public EnhancedAction getNewNoteAction() {
        return newNoteAction;
    }

    public EnhancedAction getPreferencesAction() {
        return preferencesAction;
    }

    public EnhancedAction getExitAction() {
        return exitAction;
    }

    /**
     * Returns all KeyStrokeProperty instances defined in the application config,
     * or offered by any currently-enabled extension.
     */
    public List<KeyStrokeProperty> getKeyStrokeProperties() {
        List<KeyStrokeProperty> keyProps = new ArrayList<>();

        // Add the ones we control:
        keyProps.add((KeyStrokeProperty)getPropertiesManager().getProperty(KEY_ABOUT));
        keyProps.add((KeyStrokeProperty)getPropertiesManager().getProperty(KEY_EXT_MANAGER));
        keyProps.add((KeyStrokeProperty)getPropertiesManager().getProperty(KEY_LOG_CONSOLE));
        keyProps.add((KeyStrokeProperty)getPropertiesManager().getProperty(KEY_NEW_NOTE));
        keyProps.add((KeyStrokeProperty)getPropertiesManager().getProperty(KEY_PREFERENCES));
        keyProps.add((KeyStrokeProperty)getPropertiesManager().getProperty(KEY_EXIT));

        // And now ask our extension manager:
        keyProps.addAll(SnotesExtensionManager.getInstance().getKeyStrokeProperties());

        return keyProps;
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
        props.addAll(createKeystrokeProperties());

        return props;
    }

    private List<AbstractProperty> createKeystrokeProperties() {
        aboutAction = new AboutAction();
        extensionManagerAction = new ExtensionManagerAction();
        logConsoleAction = new LogConsoleAction();
        newNoteAction = new NewNoteAction();
        preferencesAction = new PrefsAction();
        exitAction = new ExitAction();

        List<AbstractProperty> props = new ArrayList<>();

        props.add(new KeyStrokeProperty(KEY_ABOUT, "About dialog:",
                                        KeyStrokeManager.parseKeyStroke("Ctrl+A"), aboutAction)
                      .setAllowBlank(true));
        props.add(new KeyStrokeProperty(KEY_EXT_MANAGER, "Extension Manager:",
                                        KeyStrokeManager.parseKeyStroke("Ctrl+E"), extensionManagerAction)
                      .setAllowBlank(true));
        props.add(new KeyStrokeProperty(KEY_LOG_CONSOLE, "Log Console:",
                                        KeyStrokeManager.parseKeyStroke("Ctrl+L"), logConsoleAction)
                      .setAllowBlank(true));
        props.add(new KeyStrokeProperty(KEY_NEW_NOTE, "New note:",
                                        KeyStrokeManager.parseKeyStroke("Ctrl+N"), newNoteAction)
                      .setAllowBlank(true));
        props.add(new KeyStrokeProperty(KEY_PREFERENCES, "Preferences:",
                                        KeyStrokeManager.parseKeyStroke("Ctrl+P"), preferencesAction)
                      .setAllowBlank(true));
        props.add(new KeyStrokeProperty(KEY_EXIT, "Exit:",
                                        KeyStrokeManager.parseKeyStroke("Ctrl+Q"), exitAction)
                      .setAllowBlank(true));

        return props;
    }
}
