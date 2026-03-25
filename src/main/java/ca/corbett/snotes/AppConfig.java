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
import ca.corbett.extras.properties.DirectoryProperty;
import ca.corbett.extras.properties.EnumProperty;
import ca.corbett.extras.properties.IntegerProperty;
import ca.corbett.extras.properties.KeyStrokeProperty;
import ca.corbett.extras.properties.LabelProperty;
import ca.corbett.extras.properties.LookAndFeelProperty;
import ca.corbett.snotes.extensions.SnotesExtension;
import ca.corbett.snotes.extensions.SnotesExtensionManager;
import ca.corbett.snotes.io.DataManager;
import ca.corbett.snotes.ui.actions.AboutAction;
import ca.corbett.snotes.ui.actions.ExitAction;
import ca.corbett.snotes.ui.actions.ExtensionManagerAction;
import ca.corbett.snotes.ui.actions.LogConsoleAction;
import ca.corbett.snotes.ui.actions.NewNoteAction;
import ca.corbett.snotes.ui.actions.PrefsAction;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.JFrame;
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
    public static final int VALUE_NOT_SET = -9999;

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
    private BooleanProperty rememberSizePositionProp;
    private IntegerProperty windowStateProp;
    private IntegerProperty windowWidthProp;
    private IntegerProperty windowHeightProp;
    private IntegerProperty windowLeftProp;
    private IntegerProperty windowTopProp;
    private LookAndFeelProperty lookAndFeelProp;
    private DecimalProperty desktopLogoAlphaProp;
    private ColorProperty desktopGradientProp;
    private EnumProperty<CustomizableDesktopPane.LogoPlacement> desktopLogoPlacementProp;
    private DirectoryProperty dataDirProp;

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

    public boolean isSingleInstanceEnabled() {
        return enableSingleInstance.getValue();
    }

    public boolean isRememberSizeAndPositionEnabled() {
        return rememberSizePositionProp.getValue();
    }

    public int getWindowState() {
        return windowStateProp.getValue();
    }

    public int getWindowWidth() {
        return windowWidthProp.getValue();
    }

    public int getWindowHeight() {
        return windowHeightProp.getValue();
    }

    public int getWindowLeft() {
        return windowLeftProp.getValue();
    }

    public int getWindowTop() {
        return windowTopProp.getValue();
    }

    public void setWindowProps(int state, int width, int height, int left, int top) {
        windowStateProp.setValue(state);
        windowWidthProp.setValue(width);
        windowHeightProp.setValue(height);
        windowLeftProp.setValue(left);
        windowTopProp.setValue(top);
        save(); // trigger an immediate save() to persist these.
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

    public File getDataDirectory() {
        return dataDirProp.getDirectory();
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

        rememberSizePositionProp = new BooleanProperty("UI.General.rememberSizePosition",
                                                       "Remember main window size and position between sessions",
                                                       true);
        props.add(rememberSizePositionProp);

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
        props.addAll(createDataProperties());
        props.addAll(createWindowStateProperties());

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

    private List<AbstractProperty> createDataProperties() {
        List<AbstractProperty> props = new ArrayList<>();

        // The main data directory is configurable:
        dataDirProp = new DirectoryProperty("Persistence.Directory.dataDirectory", "Data directory:", false,
                                            new File(Version.SETTINGS_DIR, "data"));
        dataDirProp.setColumns(16);
        dataDirProp.setHelpText("<html>The parent directory where all notes, queries, and templates are stored." +
                                    "<br>Changing this property requires an application restart.</html>");
        props.add(dataDirProp);

        // The metadata subdirectory is not configurable, but we can at least show it here:
        LabelProperty metaSubDirProp = new LabelProperty("Persistence.Directory.metadataSubdir",
                                                         DataManager.METADATA_DIR);
        metaSubDirProp.setFieldLabelText("Metadata:");
        metaSubDirProp.setExtraMargins(0, 0);
        metaSubDirProp.setHelpText("<html>The subdirectory where queries and templates are stored." +
                                       "<br>Not currently configurable.</html>");
        props.add(metaSubDirProp);

        // The static subdirectory is also not configurable:
        LabelProperty staticSubDirProp = new LabelProperty("Persistence.Directory.staticSubdir",
                                                           DataManager.STATIC_DIR);
        staticSubDirProp.setFieldLabelText("Undated notes:");
        staticSubDirProp.setExtraMargins(0, 0);
        staticSubDirProp.setHelpText("<html>The subdirectory where undated Notes are stored." +
                                         "<br>Not currently configurable.</html>");
        props.add(staticSubDirProp);

        // And finally, the scratch subdirectory is ALSO not configurable:
        LabelProperty scratchSubDirProp = new LabelProperty("Persistence.Directory.scratchSubdir",
                                                            DataManager.SCRATCH_DIR);
        scratchSubDirProp.setFieldLabelText("Scratch:");
        scratchSubDirProp.setExtraMargins(0, 0);
        scratchSubDirProp.setHelpText("<html>The subdirectory where temporary files are stored." +
                                          "<br>Not currently configurable.</html>");
        props.add(scratchSubDirProp);

        return props;
    }

    /**
     * Our window state properties are all hidden from direct user exposure, as we will manage
     * them internally. We use a special value of VALUE_NOT_SET for the default values for all
     * of these properties, to allow MainWindow to size and position itself on a first time run.
     * On all subsequent runs, MainWindow will commit its current state on a clean shutdown
     * (even if "remember size and position" is disabled). On startup, MainWindow will check
     * to see if there are valid values here, and use them if "remember size and position" is enabled,
     * or ignore them if not.
     */
    private List<AbstractProperty> createWindowStateProperties() {
        List<AbstractProperty> props = new ArrayList<>();

        windowStateProp = new IntegerProperty("UI.Window.state", "Main window state:",
                                              VALUE_NOT_SET, 0, Integer.MAX_VALUE, 1);
        windowStateProp.setExposed(false); // not visible to the user
        props.add(windowStateProp);

        windowWidthProp = new IntegerProperty("UI.Window.width", "Main window width:", VALUE_NOT_SET, 500, 10000, 1);
        windowWidthProp.setExposed(false); // not visible to the user
        props.add(windowWidthProp);

        windowHeightProp = new IntegerProperty("UI.Window.height", "Main window height:", VALUE_NOT_SET, 400, 10000, 1);
        windowHeightProp.setExposed(false); // not visible to the user
        props.add(windowHeightProp);

        windowLeftProp = new IntegerProperty("UI.Window.left", "Main window left position:", VALUE_NOT_SET, -10000,
                                             10000, 1);
        windowLeftProp.setExposed(false); // not visible to the user
        props.add(windowLeftProp);

        windowTopProp = new IntegerProperty("UI.Window.top", "Main window top position:", VALUE_NOT_SET, -10000, 10000,
                                            1);
        windowTopProp.setExposed(false); // not visible to the user
        props.add(windowTopProp);

        return props;
    }
}
