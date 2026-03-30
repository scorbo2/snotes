package ca.corbett.snotes;

import ca.corbett.extensions.AppProperties;
import ca.corbett.extras.CustomizableDesktopPane;
import ca.corbett.extras.EnhancedAction;
import ca.corbett.extras.LookAndFeelManager;
import ca.corbett.extras.actionpanel.ColorTheme;
import ca.corbett.extras.gradient.ColorSelectionType;
import ca.corbett.extras.gradient.Gradient;
import ca.corbett.extras.io.KeyStrokeManager;
import ca.corbett.extras.properties.AbstractProperty;
import ca.corbett.extras.properties.BooleanProperty;
import ca.corbett.extras.properties.ColorProperty;
import ca.corbett.extras.properties.DecimalProperty;
import ca.corbett.extras.properties.DirectoryProperty;
import ca.corbett.extras.properties.EnumProperty;
import ca.corbett.extras.properties.FontProperty;
import ca.corbett.extras.properties.KeyStrokeProperty;
import ca.corbett.extras.properties.LabelProperty;
import ca.corbett.extras.properties.LookAndFeelProperty;
import ca.corbett.extras.properties.PropertyFormFieldValueChangedEvent;
import ca.corbett.extras.properties.ShortTextProperty;
import ca.corbett.forms.fields.CheckBoxField;
import ca.corbett.forms.fields.ColorField;
import ca.corbett.forms.fields.ComboField;
import ca.corbett.forms.fields.FormField;
import ca.corbett.snotes.extensions.SnotesExtension;
import ca.corbett.snotes.extensions.SnotesExtensionManager;
import ca.corbett.snotes.io.DataManager;
import ca.corbett.snotes.ui.EditorTheme;
import ca.corbett.snotes.ui.actions.AboutAction;
import ca.corbett.snotes.ui.actions.ExitAction;
import ca.corbett.snotes.ui.actions.ExtensionManagerAction;
import ca.corbett.snotes.ui.actions.LogConsoleAction;
import ca.corbett.snotes.ui.actions.NewNoteAction;
import ca.corbett.snotes.ui.actions.PrefsAction;
import ca.corbett.snotes.ui.actions.SaveAction;
import ca.corbett.snotes.ui.actions.SearchAction;
import com.formdev.flatlaf.FlatLightLaf;

import java.awt.Color;
import java.awt.Font;
import java.awt.Frame;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

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

    private static final Logger log = Logger.getLogger(AppConfig.class.getName());

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
    public static final String KEY_SAVE = KEYSTROKE_PREFIX + "General.save";
    public static final String KEY_EXT_MANAGER = KEYSTROKE_PREFIX + "General.extensionManager";
    public static final String KEY_LOG_CONSOLE = KEYSTROKE_PREFIX + "General.logConsole";
    public static final String KEY_NEW_NOTE = KEYSTROKE_PREFIX + "General.newNote";
    public static final String KEY_PREFERENCES = KEYSTROKE_PREFIX + "General.preferences";
    public static final String KEY_SEARCH = KEYSTROKE_PREFIX + "General.search";
    public static final String KEY_EXIT = KEYSTROKE_PREFIX + "General.exit";

    // We centralize these here so that KeyStrokeManager can handle updating
    // their keyboard accelerators when the user changes them in the properties dialog:
    private EnhancedAction aboutAction;
    private EnhancedAction saveAction;
    private EnhancedAction extensionManagerAction;
    private EnhancedAction logConsoleAction;
    private EnhancedAction newNoteAction;
    private EnhancedAction preferencesAction;
    private EnhancedAction searchAction;
    private EnhancedAction exitAction;

    private BooleanProperty enableSingleInstance;
    private BooleanProperty rememberSizePositionProp;
    private ShortTextProperty windowStateProp;
    private ShortTextProperty windowWidthProp;
    private ShortTextProperty windowHeightProp;
    private ShortTextProperty windowLeftProp;
    private ShortTextProperty windowTopProp;
    private LookAndFeelProperty lookAndFeelProp;
    private DecimalProperty desktopLogoAlphaProp;
    private ColorProperty desktopGradientProp;
    private EnumProperty<CustomizableDesktopPane.LogoPlacement> desktopLogoPlacementProp;
    private DirectoryProperty dataDirProp;

    private BooleanProperty overrideLafEditorProp;
    private EnumProperty<EditorTheme> editorThemeProp;
    private ColorProperty editorBgColorProp;
    private ColorProperty tagFontColorProp;
    private ColorProperty noteFontColorProp;
    private FontProperty tagFontProp;
    private FontProperty noteFontProp;

    private BooleanProperty overrideLafActionPanelProp;
    private EnumProperty<ColorTheme> actionPanelThemeProp;

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

    /**
     * Overridden so we can set the initial enabled/disabled state our properties.
     */
    @Override
    public boolean showPropertiesDialog(Frame owner) {
        boolean isCustomEditor = overrideLafEditorProp.getValue();
        editorThemeProp.setInitiallyEditable(isCustomEditor);
        editorBgColorProp.setInitiallyEditable(isCustomEditor);
        tagFontColorProp.setInitiallyEditable(isCustomEditor);
        noteFontColorProp.setInitiallyEditable(isCustomEditor);

        boolean isCustomActionPanel = overrideLafActionPanelProp.getValue();
        actionPanelThemeProp.setInitiallyEditable(isCustomActionPanel);

        return super.showPropertiesDialog(owner);
    }

    public boolean isSingleInstanceEnabled() {
        return enableSingleInstance.getValue();
    }

    public boolean isRememberSizeAndPositionEnabled() {
        return rememberSizePositionProp.getValue();
    }

    public int getWindowState() {
        try {
            return Integer.parseInt(windowStateProp.getValue());
        }
        catch (NumberFormatException ignored) {
            return VALUE_NOT_SET;
        }
    }

    public int getWindowWidth() {
        try {
            return Integer.parseInt(windowWidthProp.getValue());
        }
        catch (NumberFormatException ignored) {
            return VALUE_NOT_SET;
        }
    }

    public int getWindowHeight() {
        try {
            return Integer.parseInt(windowHeightProp.getValue());
        }
        catch (NumberFormatException ignored) {
            return VALUE_NOT_SET;
        }
    }

    public int getWindowLeft() {
        try {
            return Integer.parseInt(windowLeftProp.getValue());
        }
        catch (NumberFormatException ignored) {
            return VALUE_NOT_SET;
        }
    }

    public int getWindowTop() {
        try {
            return Integer.parseInt(windowTopProp.getValue());
        }
        catch (NumberFormatException ignored) {
            return VALUE_NOT_SET;
        }
    }

    public void setWindowProps(int state, int width, int height, int left, int top) {
        windowStateProp.setValue(Integer.toString(state));
        windowWidthProp.setValue(Integer.toString(width));
        windowHeightProp.setValue(Integer.toString(height));
        windowLeftProp.setValue(Integer.toString(left));
        windowTopProp.setValue(Integer.toString(top));
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

    public EnhancedAction getSaveAction() {
        return saveAction;
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

    public EnhancedAction getSearchAction() {
        return searchAction;
    }

    public EnhancedAction getPreferencesAction() {
        return preferencesAction;
    }

    public EnhancedAction getExitAction() {
        return exitAction;
    }

    public Font getTagFont() {
        return tagFontProp.getFont();
    }

    public Font getNoteFont() {
        return noteFontProp.getFont();
    }

    /**
     * Returns the currently configured editor background color, or the default
     * background color from the current Look and Feel, depending on whether
     * the user has opted to override the Look and Feel for the editor or not.
     */
    public Color getEditorBgColor() {
        if (overrideLafEditorProp.getValue()) {
            return editorBgColorProp.getSolidColor();
        }
        else {
            return LookAndFeelManager.getLafColor("TextPane.background", Color.WHITE);
        }
    }

    /**
     * Returns the currently configured tag font color, or the default foreground color from the current
     * Look and Feel, depending on whether the user has opted to override the Look and Feel for the editor or not.
     */
    public Color getTagFontColor() {
        if (overrideLafEditorProp.getValue()) {
            return tagFontColorProp.getSolidColor();
        }
        else {
            return LookAndFeelManager.getLafColor("TextPane.foreground", Color.BLUE);
        }
    }

    /**
     * Returns the currently configured note font color, or the default foreground color from the current
     * Look and Feel, depending on whether the user has opted to override the Look and Feel for the editor or not.
     */
    public Color getNoteFontColor() {
        if (overrideLafEditorProp.getValue()) {
            return noteFontColorProp.getSolidColor();
        }
        else {
            return LookAndFeelManager.getLafColor("TextPane.foreground", Color.BLACK);
        }
    }

    public boolean isOverrideLafForActionPanel() {
        return overrideLafActionPanelProp.getValue();
    }

    public ColorTheme getActionPanelColorTheme() {
        return actionPanelThemeProp.getSelectedItem();
    }

    /**
     * Returns all KeyStrokeProperty instances defined in the application config,
     * or offered by any currently-enabled extension.
     */
    public List<KeyStrokeProperty> getKeyStrokeProperties() {
        List<KeyStrokeProperty> keyProps = new ArrayList<>();

        // Add the ones we control:
        keyProps.add((KeyStrokeProperty)getPropertiesManager().getProperty(KEY_ABOUT));
        keyProps.add((KeyStrokeProperty)getPropertiesManager().getProperty(KEY_SAVE));
        keyProps.add((KeyStrokeProperty)getPropertiesManager().getProperty(KEY_EXT_MANAGER));
        keyProps.add((KeyStrokeProperty)getPropertiesManager().getProperty(KEY_LOG_CONSOLE));
        keyProps.add((KeyStrokeProperty)getPropertiesManager().getProperty(KEY_NEW_NOTE));
        keyProps.add((KeyStrokeProperty)getPropertiesManager().getProperty(KEY_PREFERENCES));
        keyProps.add((KeyStrokeProperty)getPropertiesManager().getProperty(KEY_SEARCH));
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
        lookAndFeelProp = new LookAndFeelProperty("UI.General.Look and Feel", "Look and Feel:",
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
        props.addAll(createEditorThemeProperties());
        props.addAll(createActionPanelThemeProperties());
        props.addAll(createKeystrokeProperties());
        props.addAll(createDataProperties());
        props.addAll(createWindowStateProperties());

        return props;
    }

    private List<AbstractProperty> createEditorThemeProperties() {
        List<AbstractProperty> props = new ArrayList<>();

        tagFontProp = new FontProperty("UI.Editor.tagFont", "Tag font:", new Font(Font.MONOSPACED, Font.BOLD, 16));
        noteFontProp = new FontProperty("UI.Editor.noteFont", "Note font:", new Font(Font.MONOSPACED, Font.PLAIN, 14));
        overrideLafEditorProp = new BooleanProperty("UI.Editor.overrideLaf", "Override Look and Feel", false);
        overrideLafEditorProp.addFormFieldChangeListener(this::updateEditorProps);

        editorThemeProp = new EnumProperty<>("UI.Editor.theme", "Set from theme:", EditorTheme.PAPER);
        editorThemeProp.addLeftPadding(12);
        editorThemeProp.addFormFieldChangeListener(evt -> setEditorTheme(evt));

        editorBgColorProp = new ColorProperty("UI.Editor.bgColor", "Background:",
                                              ColorSelectionType.SOLID)
            .setSolidColor(Color.WHITE);
        editorBgColorProp.addLeftPadding(12);

        tagFontColorProp = new ColorProperty("UI.Editor.tagFontColor", "Tag text:",
                                             ColorSelectionType.SOLID)
            .setSolidColor(Color.BLUE);
        tagFontColorProp.addLeftPadding(12);
        noteFontColorProp = new ColorProperty("UI.Editor.noteFontColor", "Note text:",
                                              ColorSelectionType.SOLID)
            .setSolidColor(Color.BLACK);
        noteFontColorProp.addLeftPadding(12);

        props.add(tagFontProp);
        props.add(noteFontProp);
        props.add(overrideLafEditorProp);
        props.add(editorThemeProp);
        props.add(editorBgColorProp);
        props.add(tagFontColorProp);
        props.add(noteFontColorProp);

        return props;
    }

    /**
     * Sets the enabled status of our editor color prop fields as the user
     * checks or unchecks the "Override Look and Feel" checkbox.
     */
    private void updateEditorProps(PropertyFormFieldValueChangedEvent evt) {
        if (!(evt.formField() instanceof CheckBoxField enabledField)) {
            log.warning("Unexpected field type for editor override property: " + evt.formField());
            return; // safety check
        }

        // Look up our generated form fields:
        FormField themeField = evt.formPanel().findFormField(editorThemeProp.getFullyQualifiedName());
        FormField bgField = evt.formPanel().findFormField(editorBgColorProp.getFullyQualifiedName());
        FormField tagField = evt.formPanel().findFormField(tagFontColorProp.getFullyQualifiedName());
        FormField noteField = evt.formPanel().findFormField(noteFontColorProp.getFullyQualifiedName());

        // Safety check:
        if (themeField == null || bgField == null || tagField == null || noteField == null) {
            log.warning("Could not find one or more editor theme fields in the properties form.");
            return;
        }
        themeField.setEnabled(enabledField.isChecked());
        bgField.setEnabled(enabledField.isChecked());
        tagField.setEnabled(enabledField.isChecked());
        noteField.setEnabled(enabledField.isChecked());
    }

    /**
     * Populates our editor color choosers based on the selected theme.
     */
    private void setEditorTheme(PropertyFormFieldValueChangedEvent evt) {
        if (!(evt.formField() instanceof ComboField<?> comboProp)) {
            log.warning("Unexpected field type for editor theme property: " + evt.formField());
            return; // safety check
        }
        EditorTheme selectedTheme = (EditorTheme)comboProp.getSelectedItem();

        // Look up our generated form fields:
        FormField bgField = evt.formPanel().findFormField(editorBgColorProp.getFullyQualifiedName());
        FormField tagField = evt.formPanel().findFormField(tagFontColorProp.getFullyQualifiedName());
        FormField noteField = evt.formPanel().findFormField(noteFontColorProp.getFullyQualifiedName());

        // Safety check:
        if (!(bgField instanceof ColorField bg)
            || !(tagField instanceof ColorField tag)
            || !(noteField instanceof ColorField note)) {
            log.warning("Could not find one or more editor theme fields in the properties form.");
            return;
        }
        bg.setColor(selectedTheme.getBackground());
        tag.setColor(selectedTheme.getTagColor());
        note.setColor(selectedTheme.getTextColor());
    }

    private List<AbstractProperty> createActionPanelThemeProperties() {
        List<AbstractProperty> props = new ArrayList<>();

        // We will override the LaF by default for this, because it looks much nicer in my opinion.
        // User can disable this override if they disagree, or choose a different ActionPanel theme.
        overrideLafActionPanelProp = new BooleanProperty("UI.ActionPanel.overrideLaf", "Override Look and Feel", true);
        overrideLafActionPanelProp.addFormFieldChangeListener(this::updateActionPanelProps);
        actionPanelThemeProp = new EnumProperty<>("UI.ActionPanel.theme", "Theme:", ColorTheme.DEFAULT);
        actionPanelThemeProp.addLeftPadding(12);

        props.add(overrideLafActionPanelProp);
        props.add(actionPanelThemeProp);

        return props;
    }

    /**
     * Enables or disables the generated ActionPanel theme chooser based on
     * whether the user has checked or unchecked the "Override Look and Feel" checkbox for the ActionPanel.
     */
    private void updateActionPanelProps(PropertyFormFieldValueChangedEvent evt) {
        if (!(evt.formField() instanceof CheckBoxField enabledField)) {
            log.warning("Unexpected field type for action panel override property: " + evt.formField());
            return; // safety check
        }

        // Look up our generated form field:
        FormField themeField = evt.formPanel().findFormField(actionPanelThemeProp.getFullyQualifiedName());

        // Safety check:
        if (themeField == null) {
            log.warning("Could not find action panel theme field in the properties form.");
            return;
        }
        themeField.setEnabled(enabledField.isChecked());
    }

    private List<AbstractProperty> createKeystrokeProperties() {
        aboutAction = new AboutAction();
        saveAction = new SaveAction();
        extensionManagerAction = new ExtensionManagerAction();
        logConsoleAction = new LogConsoleAction();
        newNoteAction = new NewNoteAction();
        preferencesAction = new PrefsAction();
        searchAction = new SearchAction();
        exitAction = new ExitAction();

        List<AbstractProperty> props = new ArrayList<>();

        props.add(new KeyStrokeProperty(KEY_ABOUT, "About dialog:",
                                        KeyStrokeManager.parseKeyStroke("Ctrl+A"), aboutAction)
                      .setAllowBlank(true));
        props.add(new KeyStrokeProperty(KEY_SAVE, "Save:",
                                        KeyStrokeManager.parseKeyStroke("Ctrl+S"), saveAction)
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
        props.add(new KeyStrokeProperty(KEY_SEARCH, "Search:",
                                        KeyStrokeManager.parseKeyStroke("Ctrl+F"), searchAction)
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

        // Technical note: we use ShortTextProperty for these instead of IntegerProperty,
        // because our sentinel value VALUE_NOT_SET is (deliberately) well outside
        // the min/max range of each of these properties. That will result in an
        // IllegalArgumentException when trying to generate SpinnerNumberModels.
        // So, since they're not user-visible anyway, screw it, we'll just
        // store them as String values in a text prop and convert to int as needed.

        windowStateProp = new ShortTextProperty("UI.Window.state", "Window state:", Integer.toString(VALUE_NOT_SET));
        windowStateProp.setExposed(false); // not visible to the user
        props.add(windowStateProp);

        windowWidthProp = new ShortTextProperty("UI.Window.width", "Window width:", Integer.toString(VALUE_NOT_SET));
        windowWidthProp.setExposed(false); // not visible to the user
        props.add(windowWidthProp);

        windowHeightProp = new ShortTextProperty("UI.Window.height", "Window height:", Integer.toString(VALUE_NOT_SET));
        windowHeightProp.setExposed(false); // not visible to the user
        props.add(windowHeightProp);

        windowLeftProp = new ShortTextProperty("UI.Window.left", "Window left:", Integer.toString(VALUE_NOT_SET));
        windowLeftProp.setExposed(false); // not visible to the user
        props.add(windowLeftProp);

        windowTopProp = new ShortTextProperty("UI.Window.top", "Window top:", Integer.toString(VALUE_NOT_SET));
        windowTopProp.setExposed(false); // not visible to the user
        props.add(windowTopProp);

        return props;
    }
}
