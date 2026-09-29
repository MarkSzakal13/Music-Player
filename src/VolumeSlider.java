import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.util.function.DoubleConsumer;

import javax.swing.JComponent;

/**
 * A thin slider used for the volume. It can be dragged, clicked, or scrolled.
 */
public final class VolumeSlider extends JComponent {

    /**
     * Serialization id.
     */
    private static final long serialVersionUID = 1L;
    /**
     * Preferred width of the slider.
     */
    private static final int WIDTH = 160;
    /**
     * Preferred height of the slider.
     */
    private static final int HEIGHT = 22;
    /**
     * Thickness of the track.
     */
    private static final int TRACK_THICKNESS = 4;
    /**
     * Radius of the knob.
     */
    private static final double KNOB_RADIUS = 6;
    /**
     * Transparency of the empty part of the track.
     */
    private static final int TRACK_ALPHA = 40;
    /**
     * How much one scroll wheel click changes the value.
     */
    private static final double SCROLL_STEP = 0.05;

    /**
     * The current value, from 0 to 1.
     */
    private double value;
    /**
     * The current accent color.
     */
    private Color accent = Theme.IDLE_ACCENT;
    /**
     * Called when the user changes the value.
     */
    private final DoubleConsumer onChange;

    /**
     * Creates a volume slider.
     *
     * @param value
     *            The starting value, from 0 to 1.
     * @param onChange
     *            Called with the new value when the user moves the slider.
     */
    public VolumeSlider(double value, DoubleConsumer onChange) {
        this.value = value;
        this.onChange = onChange;
        this.setPreferredSize(new Dimension(WIDTH, HEIGHT));
        this.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        MouseAdapter mouse = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                VolumeSlider.this.moveTo(e.getX());
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                VolumeSlider.this.moveTo(e.getX());
            }
        };
        this.addMouseListener(mouse);
        this.addMouseMotionListener(mouse);
        this.addMouseWheelListener(e -> this.onChange
                .accept(this.value - e.getWheelRotation() * SCROLL_STEP));
    }

    /**
     * Changes the value to match where the user clicked.
     *
     * @param x
     *            The x position of the mouse.
     */
    private void moveTo(int x) {
        int padding = this.getHeight() / 2;
        int trackWidth = Math.max(1, this.getWidth() - 2 * padding);
        this.onChange.accept((double) (x - padding) / trackWidth);
    }

    /**
     * Updates the value shown by the slider.
     *
     * @param value
     *            The new value, from 0 to 1.
     */
    public void setValue(double value) {
        this.value = value;
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
        int padding = this.getHeight() / 2;
        int trackWidth = this.getWidth() - 2 * padding;
        int trackY = (this.getHeight() - TRACK_THICKNESS) / 2;
        int filled = (int) (trackWidth * this.value);

        g.setColor(Theme.withAlpha(Color.WHITE, TRACK_ALPHA));
        g.fillRoundRect(padding, trackY, trackWidth, TRACK_THICKNESS,
                TRACK_THICKNESS, TRACK_THICKNESS);
        g.setColor(this.accent);
        g.fillRoundRect(padding, trackY, filled, TRACK_THICKNESS,
                TRACK_THICKNESS, TRACK_THICKNESS);

        g.setColor(Theme.TEXT);
        double knobX = padding + filled - KNOB_RADIUS;
        double knobY = this.getHeight() / 2.0 - KNOB_RADIUS;
        g.fill(new Ellipse2D.Double(knobX, knobY, 2 * KNOB_RADIUS,
                2 * KNOB_RADIUS));
        g.dispose();
    }
}
