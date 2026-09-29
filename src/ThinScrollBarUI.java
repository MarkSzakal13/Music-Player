import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.plaf.basic.BasicScrollBarUI;

/**
 * A slim scroll bar with a rounded thumb and no arrow buttons.
 */
public final class ThinScrollBarUI extends BasicScrollBarUI {

    /**
     * Transparency of the thumb.
     */
    private static final int THUMB_ALPHA = 50;

    /**
     * Creates a button with no size, used to hide the arrow buttons.
     *
     * @return the hidden button.
     */
    private static JButton hiddenButton() {
        JButton button = new JButton();
        button.setPreferredSize(new Dimension(0, 0));
        return button;
    }

    @Override
    protected JButton createDecreaseButton(int orientation) {
        return hiddenButton();
    }

    @Override
    protected JButton createIncreaseButton(int orientation) {
        return hiddenButton();
    }

    @Override
    protected void paintTrack(Graphics g, JComponent c, Rectangle bounds) {
        // The track is left empty so only the thumb shows.
    }

    @Override
    protected void paintThumb(Graphics graphics, JComponent c,
            Rectangle bounds) {
        Graphics2D g = Theme.smooth(graphics);
        g.setColor(Theme.withAlpha(Color.WHITE, THUMB_ALPHA));
        g.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height,
                bounds.width, bounds.width);
        g.dispose();
    }
}
