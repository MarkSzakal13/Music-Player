import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

import javax.swing.JComponent;

/**
 * Draws the waveform of the current song and works as the seek bar. Clicking
 * or dragging jumps to that part of the song.
 */
public final class WaveformBar extends JComponent {

    /**
     * Serialization id.
     */
    private static final long serialVersionUID = 1L;
    /**
     * Preferred width.
     */
    private static final int WIDTH = 420;
    /**
     * Preferred height.
     */
    private static final int HEIGHT = 56;
    /**
     * How much of each slot a bar fills.
     */
    private static final double BAR_FILL = 0.55;
    /**
     * Thinnest a bar can be.
     */
    private static final double MIN_BAR_WIDTH = 1.5;
    /**
     * Shortest a bar can be, so quiet parts are still visible.
     */
    private static final double MIN_BAR_HEIGHT = 0.04;
    /**
     * Space left above and below the bars.
     */
    private static final int VERTICAL_PADDING = 6;
    /**
     * Transparency of bars that have not played yet.
     */
    private static final int UNPLAYED_ALPHA = 55;
    /**
     * Transparency of bars under the mouse.
     */
    private static final int HOVER_ALPHA = 110;
    /**
     * Gap above and below the playhead line.
     */
    private static final int PLAYHEAD_INSET = 2;
    /**
     * Transparency of the time tooltip background.
     */
    private static final int TOOLTIP_ALPHA = 170;
    /**
     * Font size of the time tooltip.
     */
    private static final float TOOLTIP_FONT = 11f;
    /**
     * Padding inside the time tooltip.
     */
    private static final int TOOLTIP_PADDING = 5;
    /**
     * Corner size of the time tooltip.
     */
    private static final int TOOLTIP_CORNER = 8;

    /**
     * The song being shown, or null if nothing is loaded.
     */
    private AudioTrack track;
    /**
     * The current accent color.
     */
    private Color accent = Theme.IDLE_ACCENT;
    /**
     * The x position of the mouse, or -1 if the mouse is not over the bar.
     */
    private int mouseX = -1;

    /**
     * Creates the waveform bar.
     */
    public WaveformBar() {
        this.setPreferredSize(new Dimension(WIDTH, HEIGHT));
        this.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        MouseAdapter mouse = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                WaveformBar.this.seekTo(e.getX());
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                WaveformBar.this.mouseX = e.getX();
                WaveformBar.this.seekTo(e.getX());
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                WaveformBar.this.mouseX = e.getX();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                WaveformBar.this.mouseX = -1;
            }
        };
        this.addMouseListener(mouse);
        this.addMouseMotionListener(mouse);
    }

    /**
     * Sets the song to show.
     *
     * @param track
     *            The song, or null if nothing is loaded.
     */
    public void setTrack(AudioTrack track) {
        this.track = track;
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

    /**
     * Jumps to the part of the song under the mouse.
     *
     * @param x
     *            The x position of the mouse.
     */
    private void seekTo(int x) {
        if (this.track != null) {
            this.track.seek((double) x / Math.max(1, this.getWidth()));
        }
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = Theme.smooth(graphics);
        int width = this.getWidth();
        int height = this.getHeight();
        double middle = height / 2.0;
        double slot = (double) width / AudioTrack.WAVEFORM_BARS;
        double barWidth = Math.max(MIN_BAR_WIDTH, slot * BAR_FILL);

        double playedX = 0;
        if (this.track != null) {
            playedX = this.track.progress() * width;
        }

        for (int i = 0; i < AudioTrack.WAVEFORM_BARS; i++) {
            double x = i * slot + (slot - barWidth) / 2;
            double level = MIN_BAR_HEIGHT;
            if (this.track != null) {
                level = Math.max(MIN_BAR_HEIGHT, this.track.waveform()[i]);
            }
            double barHeight = level * (height - VERTICAL_PADDING);

            if (x + barWidth / 2 <= playedX) {
                g.setColor(this.accent);
            } else if (this.mouseX >= 0 && x <= this.mouseX) {
                g.setColor(Theme.withAlpha(Color.WHITE, HOVER_ALPHA));
            } else {
                g.setColor(Theme.withAlpha(Color.WHITE, UNPLAYED_ALPHA));
            }
            g.fill(new RoundRectangle2D.Double(x, middle - barHeight / 2,
                    barWidth, barHeight, barWidth, barWidth));
        }

        if (this.track != null) {
            g.setColor(Theme.TEXT);
            g.fill(new RoundRectangle2D.Double(playedX - 1, PLAYHEAD_INSET, 2,
                    height - 2 * PLAYHEAD_INSET, 2, 2));

            if (this.mouseX >= 0) {
                this.drawTimeTooltip(g, width);
            }
        }
        g.dispose();
    }

    /**
     * Draws the time under the mouse in a small box.
     *
     * @param g
     *            The graphics to draw with.
     * @param width
     *            The width of the bar.
     */
    private void drawTimeTooltip(Graphics2D g, int width) {
        double seconds = (double) this.mouseX / width * this.track.seconds();
        String time = TrackInfo.formatTime(seconds);

        g.setFont(Theme.font(Theme.SANS, java.awt.Font.BOLD, TOOLTIP_FONT));
        FontMetrics metrics = g.getFontMetrics();
        int boxWidth = metrics.stringWidth(time) + 2 * TOOLTIP_PADDING;
        int boxX = this.mouseX - boxWidth / 2;
        boxX = Math.max(0, Math.min(width - boxWidth, boxX));

        g.setColor(new Color(0, 0, 0, TOOLTIP_ALPHA));
        g.fillRoundRect(boxX, 0, boxWidth, metrics.getHeight(), TOOLTIP_CORNER,
                TOOLTIP_CORNER);
        g.setColor(Theme.TEXT);
        g.drawString(time, boxX + TOOLTIP_PADDING, metrics.getAscent());
    }
}
