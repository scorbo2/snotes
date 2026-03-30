package ca.corbett.snotes.ui;

import java.awt.Color;

/**
 * Provides a few built-in editor themes that can be used if the
 * user decides to override the current Look and Feel.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 * @since Snotes 2.0
 */
public enum EditorTheme {

    /**
     * From the original V1 Snotes application.
     */
    PAPER("Paper",
          new Color(170, 170, 170),
          new Color(0, 0, 102),
          Color.BLACK),

    /**
     * Classic green on black.
     */
    MATRIX("Matrix",
           new Color(0, 0, 0),
           new Color(0, 255, 0),
           new Color(0, 255, 0)),

    /**
     * A bluesy theme.
     */
    GOT_THE_BLUES("Got the Blues",
                  new Color(0, 0, 128),
                  new Color(200, 255, 255),
                  new Color(215, 225, 255));

    private final String label;
    private final Color background;
    private final Color tagColor;
    private final Color textColor;

    EditorTheme(String label, Color bg, Color tag, Color text) {
        this.label = label;
        this.background = bg;
        this.tagColor = tag;
        this.textColor = text;
    }

    @Override
    public String toString() {
        return label;
    }

    public Color getBackground() {
        return background;
    }

    public Color getTagColor() {
        return tagColor;
    }

    public Color getTextColor() {
        return textColor;
    }
}
