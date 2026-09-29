import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;

import javax.swing.JButton;

/**
 * A round button that draws its own icon. The icons are drawn on a 24 by 24
 * grid (the same way SVG icons work) and then scaled to the button size.
 */
public final class IconButton extends JButton {

    /**
     * The icons a button can show.
     */
    public enum Icon {
        /**
         * Play triangle.
         */
        PLAY,
        /**
         * Two pause bars.
         */
        PAUSE,
        /**
         * Skip forward.
         */
        NEXT,
        /**
         * Skip back.
         */
        PREVIOUS,
        /**
         * Crossed shuffle arrows.
         */
        SHUFFLE,
        /**
         * Repeat loop.
         */
        REPEAT,
        /**
         * Repeat loop with a 1 inside.
         */
        REPEAT_ONE,
        /**
         * Plus sign.
         */
        ADD,
        /**
         * Folder.
         */
        OPEN,
        /**
         * Download arrow.
         */
        SAVE,
        /**
         * Speaker.
         */
        VOLUME
    }

    /**
     * Serialization id.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Size of the grid the icons are drawn on.
     */
    private static final double GRID = 24;
    /**
     * Width of icon lines on the grid.
     */
    private static final float LINE_WIDTH = 2f;
    /**
     * Icon size compared to the button for the main button.
     */
    private static final double MAIN_ICON_SCALE = 0.45;
    /**
     * Icon size compared to the button for other buttons.
     */
    private static final double ICON_SCALE = 0.6;
    /**
     * Size of the filled circle compared to the main button.
     */
    private static final double MAIN_CIRCLE_SCALE = 0.84;
    /**
     * How much the main button shrinks while pressed.
     */
    private static final double PRESSED_SCALE = 0.94;
    /**
     * Transparency of the glow around the main button.
     */
    private static final int GLOW_ALPHA = 60;
    /**
     * Transparency of the circle shown when hovering.
     */
    private static final int HOVER_ALPHA = 22;
    /**
     * How much lighter the main button gets when hovering.
     */
    private static final double HOVER_LIGHTEN = 0.2;
    /**
     * Color of the icon on the main button.
     */
    private static final Color MAIN_ICON_COLOR = new Color(0x1A1022);
    /**
     * Color of icons that are not active.
     */
    private static final Color ICON_COLOR = new Color(0xC9C2D2);
    /**
     * Radius of the dot under an active toggle button.
     */
    private static final double DOT_RADIUS = 1.2;
    /**
     * How far below the center the active dot is, on the grid.
     */
    private static final double DOT_OFFSET = 11;

    /**
     * Play triangle.
     */
    private static final Shape PLAY_SHAPE = shape(true, 8, 5, 19, 12, 8, 19);
    /**
     * Left pause bar.
     */
    private static final Shape PAUSE_LEFT = new RoundRectangle2D.Double(6, 5,
            4, 14, 1, 1);
    /**
     * Right pause bar.
     */
    private static final Shape PAUSE_RIGHT = new RoundRectangle2D.Double(14,
            5, 4, 14, 1, 1);
    /**
     * Next triangle.
     */
    private static final Shape NEXT_SHAPE = shape(true, 6, 6, 15, 12, 6, 18);
    /**
     * Next bar.
     */
    private static final Shape NEXT_BAR = new RoundRectangle2D.Double(16, 6,
            2.5, 12, 1, 1);
    /**
     * Previous triangle.
     */
    private static final Shape PREVIOUS_SHAPE = shape(true, 18, 6, 9, 12, 18,
            18);
    /**
     * Previous bar.
     */
    private static final Shape PREVIOUS_BAR = new RoundRectangle2D.Double(5.5,
            6, 2.5, 12, 1, 1);
    /**
     * Shuffle line going down.
     */
    private static final Shape SHUFFLE_DOWN = shape(false, 3, 7, 7, 7, 17, 17,
            21, 17);
    /**
     * Shuffle line going up.
     */
    private static final Shape SHUFFLE_UP = shape(false, 3, 17, 7, 17, 17, 7,
            21, 7);
    /**
     * Top shuffle arrow head.
     */
    private static final Shape SHUFFLE_TOP_ARROW = shape(false, 18, 4, 21, 7,
            18, 10);
    /**
     * Bottom shuffle arrow head.
     */
    private static final Shape SHUFFLE_BOTTOM_ARROW = shape(false, 18, 14, 21,
            17, 18, 20);
    /**
     * Repeat loop.
     */
    private static final Shape REPEAT_LOOP = new RoundRectangle2D.Double(4, 7,
            16, 10, 6, 6);
    /**
     * Repeat arrow head.
     */
    private static final Shape REPEAT_ARROW = shape(false, 12, 4, 15, 7, 12,
            10);
    /**
     * Font size of the 1 in the repeat one icon.
     */
    private static final float REPEAT_ONE_FONT = 9f;
    /**
     * Baseline of the 1 in the repeat one icon.
     */
    private static final float REPEAT_ONE_BASELINE = 15.5f;
    /**
     * Vertical line of the plus sign.
     */
    private static final Shape ADD_VERTICAL = shape(false, 12, 6, 12, 18);
    /**
     * Horizontal line of the plus sign.
     */
    private static final Shape ADD_HORIZONTAL = shape(false, 6, 12, 18, 12);
    /**
     * Folder outline.
     */
    private static final Shape FOLDER = shape(true, 3, 6, 9, 6, 11, 8, 21, 8,
            21, 18, 3, 18);
    /**
     * Save arrow shaft.
     */
    private static final Shape SAVE_SHAFT = shape(false, 12, 4, 12, 14);
    /**
     * Save arrow head.
     */
    private static final Shape SAVE_ARROW = shape(false, 8, 10, 12, 14, 16,
            10);
    /**
     * Save tray.
     */
    private static final Shape SAVE_TRAY = shape(false, 4, 15, 4, 19, 20, 19,
            20, 15);
    /**
     * Speaker body.
     */
    private static final Shape SPEAKER = shape(true, 4, 9, 8, 9, 13, 5, 13,
            19, 8, 15, 4, 15);
    /**
     * Sound wave coming out of the speaker.
     */
    private static final Shape SOUND_WAVE = new Arc2D.Double(11, 7, 8, 10,
            -60, 120, Arc2D.OPEN);

    /**
     * The icon being shown.
     */
    private Icon icon;
    /**
     * True if this is the big play button, which is filled with the accent
     * color.
     */
    private final boolean isMain;
    /**
     * True if a toggle button (like repeat) is turned on.
     */
    private boolean isActive;
    /**
     * True while the mouse is over the button.
     */
    private boolean isHovered;
    /**
     * The current accent color.
     */
    private Color accent = Theme.IDLE_ACCENT;

    /**
     * Creates an icon button.
     *
     * @param icon
     *            The icon to show.
     * @param size
     *            The width and height of the button.
     * @param isMain
     *            True for the big play button.
     */
    public IconButton(Icon icon, int size, boolean isMain) {
        this.icon = icon;
        this.isMain = isMain;
        this.setPreferredSize(new Dimension(size, size));
        this.setContentAreaFilled(false);
        this.setBorderPainted(false);
        this.setFocusPainted(false);
        this.setFocusable(false);
        this.setOpaque(false);
        this.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        this.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                IconButton.this.isHovered = true;
                IconButton.this.repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                IconButton.this.isHovered = false;
                IconButton.this.repaint();
            }
        });
    }

    /**
     * Changes the icon.
     *
     * @param icon
     *            The new icon.
     */
    public void setIcon(Icon icon) {
        if (this.icon != icon) {
            this.icon = icon;
            this.repaint();
        }
    }

    /**
     * Turns the active highlight on or off.
     *
     * @param isActive
     *            True to highlight the button.
     */
    public void setActive(boolean isActive) {
        this.isActive = isActive;
        this.repaint();
    }

    /**
     * Sets the accent color.
     *
     * @param accent
     *            The new accent color.
     */
    public void setAccent(Color accent) {
        this.accent = accent;
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = Theme.smooth(graphics);
        double size = Math.min(this.getWidth(), this.getHeight());
        double radius = size / 2;
        Color iconColor;

        if (this.isMain) {
            double circle = radius * MAIN_CIRCLE_SCALE;
            if (this.getModel().isPressed()) {
                circle = circle * PRESSED_SCALE;
            }
            Color fill = this.accent;
            if (this.isHovered) {
                fill = Theme.blend(this.accent, Color.WHITE, HOVER_LIGHTEN);
            }

            g.setColor(Theme.withAlpha(this.accent, GLOW_ALPHA));
            g.fill(circle(radius, radius, radius));
            g.setColor(fill);
            g.fill(circle(radius, radius, circle));
            iconColor = MAIN_ICON_COLOR;
        } else {
            if (this.isHovered && this.isEnabled()) {
                g.setColor(Theme.withAlpha(Color.WHITE, HOVER_ALPHA));
                g.fill(circle(radius, radius, radius));
            }

            if (!this.isEnabled()) {
                iconColor = Theme.MUTED_TEXT;
            } else if (this.isActive) {
                iconColor = this.accent;
            } else if (this.isHovered) {
                iconColor = Theme.TEXT;
            } else {
                iconColor = ICON_COLOR;
            }
        }

        double iconSize = size * ICON_SCALE;
        if (this.isMain) {
            iconSize = size * MAIN_ICON_SCALE;
        }
        double offset = (size - iconSize) / 2;
        g.translate(offset, offset);
        g.scale(iconSize / GRID, iconSize / GRID);
        g.setColor(iconColor);
        g.setStroke(new BasicStroke(LINE_WIDTH, BasicStroke.CAP_ROUND,
                BasicStroke.JOIN_ROUND));

        this.drawIcon(g);

        if (this.isActive) {
            g.fill(circle(GRID / 2, GRID / 2 + DOT_OFFSET, DOT_RADIUS));
        }
        g.dispose();
    }

    /**
     * Draws the current icon on the 24 by 24 grid.
     *
     * @param g
     *            The graphics, already scaled to the grid.
     */
    private void drawIcon(Graphics2D g) {
        switch (this.icon) {
            case PLAY:
                g.fill(PLAY_SHAPE);
                break;
            case PAUSE:
                g.fill(PAUSE_LEFT);
                g.fill(PAUSE_RIGHT);
                break;
            case NEXT:
                g.fill(NEXT_SHAPE);
                g.fill(NEXT_BAR);
                break;
            case PREVIOUS:
                g.fill(PREVIOUS_SHAPE);
                g.fill(PREVIOUS_BAR);
                break;
            case SHUFFLE:
                g.draw(SHUFFLE_DOWN);
                g.draw(SHUFFLE_UP);
                g.draw(SHUFFLE_TOP_ARROW);
                g.draw(SHUFFLE_BOTTOM_ARROW);
                break;
            case REPEAT:
            case REPEAT_ONE:
                this.drawRepeat(g);
                break;
            case ADD:
                g.draw(ADD_VERTICAL);
                g.draw(ADD_HORIZONTAL);
                break;
            case OPEN:
                g.draw(FOLDER);
                break;
            case SAVE:
                g.draw(SAVE_SHAFT);
                g.draw(SAVE_ARROW);
                g.draw(SAVE_TRAY);
                break;
            case VOLUME:
                g.fill(SPEAKER);
                g.draw(SOUND_WAVE);
                break;
            default:
                break;
        }
    }

    /**
     * Draws the repeat icon, with a 1 inside for repeat one.
     *
     * @param g
     *            The graphics, already scaled to the grid.
     */
    private void drawRepeat(Graphics2D g) {
        g.draw(REPEAT_LOOP);
        g.draw(REPEAT_ARROW);

        if (this.icon == Icon.REPEAT_ONE) {
            g.setFont(Theme.font(Theme.SANS, java.awt.Font.BOLD,
                    REPEAT_ONE_FONT));
            FontMetrics metrics = g.getFontMetrics();
            float x = (float) (GRID - metrics.stringWidth("1")) / 2;
            g.drawString("1", x, REPEAT_ONE_BASELINE);
        }
    }

    /**
     * Builds a shape from a list of points on the grid.
     *
     * @param closed
     *            True to connect the last point back to the first.
     * @param points
     *            The points as x1, y1, x2, y2, and so on.
     * @return the shape.
     */
    private static Shape shape(boolean closed, double... points) {
        Path2D path = new Path2D.Double();
        path.moveTo(points[0], points[1]);
        for (int i = 2; i < points.length; i += 2) {
            path.lineTo(points[i], points[i + 1]);
        }
        if (closed) {
            path.closePath();
        }
        return path;
    }

    /**
     * Builds a circle.
     *
     * @param centerX
     *            The x of the center.
     * @param centerY
     *            The y of the center.
     * @param radius
     *            The radius.
     * @return the circle.
     */
    private static Ellipse2D circle(double centerX, double centerY,
            double radius) {
        return new Ellipse2D.Double(centerX - radius, centerY - radius,
                2 * radius, 2 * radius);
    }
}
