package ca.corbett.snotes;

import java.io.File;

/**
 * Constants concerning the application name and version information.
 * TODO update this for newer style
 *      musicplayer is the reference app
 *      basically we pass in env vars for APPLICATION_HOME and EXTENSIONS_DIR and we don't care about the install dir
 *
 * @author scorbett
 */
public final class Version {

    /**
     * Irrelevant constructor.
     */
    private Version() {
    }

    /**
     * The major version. *
     */
    public static final int VERSION_MAJOR = 1;

    /**
     * The minor (patch) version. *
     */
    public static final int VERSION_MINOR = 0;

    /**
     * A user-friendly version string in the form "MAJOR.MINOR" (example: "1.0"). *
     */
    public static final String VERSION = VERSION_MAJOR + "." + VERSION_MINOR;

    /**
     * The user-friendly name of this application. *
     */
    public static final String APPLICATION_NAME = "Snotes";

    /**
     * The directory where this application is installed.
     * This property is set by the launcher script into a Java property called
     * ${APPLICATION_NAME}_HOME - if the jar is launched through some other means, the
     * system property won't be set, and so this value will be set to the user home dir.
     */
    public static final File APPLICATION_DIR;

    /**
     * The fully qualified directory where user-specific settings and files are stored.
     * This is typically ${user.home}/.${APPLICATION_NAME}/ or similar.
     */
    public static final File USER_SETTINGS_DIR;

    /**
     * Static initializer to populate APPLICATION_HOME and USER_HOME properties.
     */
    static {
        File homeDir = new File(System.getProperty("user.home"));
        USER_SETTINGS_DIR = new File(homeDir, "." + APPLICATION_NAME);
        if (!USER_SETTINGS_DIR.exists()) {
            USER_SETTINGS_DIR.mkdirs();
        }

        File appDir;
        String home = System.getProperty(APPLICATION_NAME + "_HOME"); // Set by launcher script
        if (home != null) {
            appDir = new File(home);
        }
        else {
            appDir = USER_SETTINGS_DIR; // fallback, but something's wrong and this isn't ideal now
        }
        if (!appDir.exists()) {
            APPLICATION_DIR = new File("/"); // fallback fallback, we're fubar now
        }
        else {
            APPLICATION_DIR = appDir;
        }

    }

}
