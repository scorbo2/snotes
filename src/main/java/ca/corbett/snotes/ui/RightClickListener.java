package ca.corbett.snotes.ui;

import javax.swing.JPopupMenu;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * A generic utility class that listens for right-click events and shows a given JPopupMenu.
 */
public class RightClickListener extends MouseAdapter {
    private final JPopupMenu popupMenu;

    public RightClickListener(JPopupMenu popupMenu) {
        this.popupMenu = popupMenu;
    }

    private void maybeShowPopup(MouseEvent e) {
        if (e.isPopupTrigger()) {
            popupMenu.show(e.getComponent(), e.getX(), e.getY());
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
        maybeShowPopup(e);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        maybeShowPopup(e);
    }
}
