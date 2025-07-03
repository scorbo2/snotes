package ca.corbett.snotes;

import ca.corbett.extras.image.ImageUtil;
import ca.corbett.extras.image.LogoConfig;
import ca.corbett.extras.progress.SimpleProgressAdapter;
import ca.corbett.extras.progress.SimpleProgressWorker;
import ca.corbett.extras.progress.SplashProgressWindow;
import ca.corbett.snotes.io.Scanner;
import ca.corbett.snotes.ui.MainWindow;

import javax.swing.JFrame;
import javax.swing.UIManager;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * Entry point for Snotes. There is no CLI.
 *
 * @author scorbo2
 */
public class Main {

    public static void main(String[] args) {
        // turn off the silly sideways popup menu title that JTattoo generates:
        // TODO this can be updated as swing-extras supports themes now
        Properties props = new Properties();
        props.put("logoString", "");
        com.jtattoo.plaf.graphite.GraphiteLookAndFeel.setCurrentTheme(props);

        // Set L&F to something other than the boring defaults:
        try {
            UIManager.setLookAndFeel("com.jtattoo.plaf.graphite.GraphiteLookAndFeel");
//      for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
//        if ("Nimbus".equals(info.getName())) {
//          javax.swing.UIManager.setLookAndFeel(info.getClassName());
//          break;
//        }
//      }
        }
        catch (ClassNotFoundException
               | InstantiationException
               | IllegalAccessException
               | javax.swing.UnsupportedLookAndFeelException cnfe) {
            System.out.println("Error setting look and feel: " + cnfe.getMessage());
        }

        // Set up logging based on logging properties, if any:
        // TODO approach to logging has changed, update this code
        LogManager manager = LogManager.getLogManager();
        try {
            File globalConfig = new File(Version.APPLICATION_DIR, "logging.properties");
            File userConfig = new File(Version.USER_SETTINGS_DIR, "logging.properties");

            // Use user config if it exists:
            if (userConfig.exists()) {
                manager.readConfiguration(new FileInputStream(userConfig));
            }
            else if (globalConfig.exists()) {
                manager.readConfiguration(new FileInputStream(globalConfig));
            }
        }
        catch (IOException | SecurityException ignored) {
        }

        final Logger logger = Logger.getLogger(Main.class.getName());
        logger.info(Version.APPLICATION_NAME + " " + Version.VERSION + " starting up...");
        URL url = Main.class.getResource("/snotes/images/logo_wide.jpg");
        BufferedImage splashImage = null;
        SplashProgressWindow splashWindow;
        try {
            if (url == null) {
                throw new IOException("Image not found.");
            }
            splashImage = ImageUtil.loadImage(url);
            splashWindow = new SplashProgressWindow(Color.GRAY, Color.BLACK, splashImage);
        }
        catch (IOException ioe) {
            logger.log(Level.SEVERE, "Unable to load logo image.", ioe);
            LogoConfig conf = new LogoConfig(Version.APPLICATION_NAME);
            conf.setLogoWidth(350);
            conf.setLogoHeight(80);
            conf.setTextColor(Color.BLACK);
            conf.setBgColor(Color.WHITE);
            conf.setBorderColor(Color.BLACK);
            conf.setBorderWidth(2);
            splashWindow = new SplashProgressWindow(Version.APPLICATION_NAME, conf);
        }

        // TODO migrate AppPreferences to a newer AppConfig implementation
        AppPreferences.load();
        MainWindow.setLogoImage(splashImage); // must be invoked before getInstance()/initComponents()
        final MainWindow window = MainWindow.getInstance();
        splashWindow.runWorker(new SimpleProgressWorker() {
            @Override
            public void run() {
                Scanner.findAll(AppPreferences.getDataDirectory(), new SimpleProgressAdapter() {
                    @Override
                    public void progressBegins(int stepCount) {
                        fireProgressBegins(stepCount);
                    }

                    @Override
                    public boolean progressUpdate(int step, String msg) {
                        return fireProgressUpdate(step, msg);
                    }

                    @Override
                    public void progressComplete() {
                        fireProgressComplete();

                        // Create and display the form
                        java.awt.EventQueue.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                                window.setVisible(true);
                            }

                        });
                    }

                });
            }

        });
    }

}
