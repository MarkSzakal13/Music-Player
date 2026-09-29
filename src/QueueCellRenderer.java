import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

/**
 * Draws one song in the queue: its number, title, artist, and length. The
 * first row is the song that is playing and is highlighted.
 */
public final class QueueCellRenderer extends JComponent
        implements ListCellRenderer<String> {

    /**
     * Serialization id.
     */
    private static final long serialVersionUID = 1L;
    /**
     * Height of each row.
     */
    public static final int ROW_HEIGHT = 54;
    /**
     * Space above and below the row highlight.
     */
    private static final int HIGHLIGHT_INSET = 3;
    /**
     * Corner size of the row highlight.
     */
    private static final int HIGHLIGHT_CORNER = 14;
    /**
     * Transparency of the highlight on the playing row.
     */
    private static final int PLAYING_ALPHA = 38;
    /**
     * Transparency of the highlight on a selected row.
     */
    private static final int SELECTED_ALPHA = 18;
    /**
     * Where the title and artist text starts.
     */
    private static final int TEXT_LEFT = 34;
    /**
     * Where the row number is drawn.
     */
    private static final int NUMBER_LEFT = 12;
    /**
     * Space to the right of the song length.
     */
    private static final int RIGHT_PADDING = 10;
    /**
     * Space between the title and the song length.
     */
    private static final int GAP = 12;
    /**
     * Font size of the title.
     */
    private static final float TITLE_FONT = 14f;
    /**
     * Font size of the other text.
     */
    private static final float SMALL_FONT = 12f;
    /**
     * How far the title is above the middle of the row.
     */
    private static final int TITLE_OFFSET = 2;
    /**
     * How far the artist is below the middle of the row.
     */
    private static final int ARTIST_OFFSET = 14;
    /**
     * How far the number and length are below the middle of the row.
     */
    private static final int NUMBER_OFFSET = 5;
    /**
     * Size of the dot shown on the current song while paused.
     */
    private static final int DOT_SIZE = 6;
    /**
     * Color of titles that are not playing.
     */
    private static final Color TITLE_COLOR = new Color(0xDAD3E2);

    /**
     * Number of bars in the playing animation.
     */
    private static final int EQUALIZER_BARS = 3;
    /**
     * Left edge of the playing animation.
     */
    private static final int EQUALIZER_LEFT = 10;
    /**
     * Space from one bar to the next.
     */
    private static final int EQUALIZER_STEP = 5;
    /**
     * Width of each bar.
     */
    private static final int EQUALIZER_BAR_WIDTH = 3;
    /**
     * Tallest a bar gets.
     */
    private static final int EQUALIZER_HEIGHT = 16;
    /**
     * How far the bottom of the bars is below the middle of the row.
     */
    private static final int EQUALIZER_BOTTOM = 8;
    /**
     * Shortest a bar gets, compared to its tallest.
     */
    private static final double EQUALIZER_MIN = 0.35;
    /**
     * Base speed of the bars.
     */
    private static final double EQUALIZER_SPEED = 3;
    /**
     * Extra speed for each bar so they don't move together.
     */
    private static final double EQUALIZER_SPEED_STEP = 1.7;
    /**
     * Nanoseconds in a second.
     */
    private static final double NANOSECONDS = 1e9;

    /**
     * Path of the song in the row being drawn.
     */
    private String path = "";
    /**
     * Index of the row being drawn.
     */
    private int index;
    /**
     * True if the row being drawn is selected.
     */
    private boolean isSelected;
    /**
     * True if music is playing.
     */
    private boolean isPlaying;
    /**
     * The current accent color.
     */
    private Color accent = Theme.IDLE_ACCENT;
    /**
     * Song lengths that were already read, so files are only read once.
     */
    private final Map<String, String> lengths = new HashMap<>();

    /**
     * Updates the accent color and playing state.
     *
     * @param accent
     *            The current accent color.
     * @param isPlaying
     *            True if music is playing.
     */
    public void update(Color accent, boolean isPlaying) {
        this.accent = accent;
        this.isPlaying = isPlaying;
    }

    @Override
    public Component getListCellRendererComponent(
            JList<? extends String> list, String value, int index,
            boolean isSelected, boolean cellHasFocus) {
        this.path = value;
        this.index = index;
        this.isSelected = isSelected;
        return this;
    }

    /**
     * Returns the length of a song as text, reading it from the file the
     * first time.
     *
     * @param song
     *            The path to the song.
     * @return the length like "3:07", or "" if it is unknown.
     */
    private String lengthOf(String song) {
        if (!this.lengths.containsKey(song)) {
            double seconds = AudioTrack.lengthOf(new File(song));
            String text = "";
            if (seconds >= 0) {
                text = TrackInfo.formatTime(seconds);
            }
            this.lengths.put(song, text);
        }
        return this.lengths.get(song);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = Theme.smooth(graphics);
        int width = this.getWidth();
        int middle = this.getHeight() / 2;
        boolean isCurrent = this.index == 0;
        int highlightHeight = this.getHeight() - 2 * HIGHLIGHT_INSET;

        if (isCurrent) {
            g.setColor(Theme.withAlpha(this.accent, PLAYING_ALPHA));
            g.fillRoundRect(0, HIGHLIGHT_INSET, width, highlightHeight,
                    HIGHLIGHT_CORNER, HIGHLIGHT_CORNER);
        } else if (this.isSelected) {
            g.setColor(Theme.withAlpha(Color.WHITE, SELECTED_ALPHA));
            g.fillRoundRect(0, HIGHLIGHT_INSET, width, highlightHeight,
                    HIGHLIGHT_CORNER, HIGHLIGHT_CORNER);
        }

        Font small = Theme.font(Theme.SANS, Font.PLAIN, SMALL_FONT);
        g.setFont(small);
        if (isCurrent && this.isPlaying) {
            this.drawEqualizer(g, middle);
        } else if (isCurrent) {
            g.setColor(this.accent);
            g.fillOval(NUMBER_LEFT, middle - DOT_SIZE / 2, DOT_SIZE, DOT_SIZE);
        } else {
            g.setColor(Theme.MUTED_TEXT);
            g.drawString(String.valueOf(this.index), NUMBER_LEFT,
                    middle + NUMBER_OFFSET);
        }

        FontMetrics smallMetrics = g.getFontMetrics();
        String length = this.lengthOf(this.path);
        int lengthWidth = smallMetrics.stringWidth(length);
        int textWidth = width - TEXT_LEFT - lengthWidth - RIGHT_PADDING - GAP;

        g.setColor(Theme.MUTED_TEXT);
        g.drawString(length, width - lengthWidth - RIGHT_PADDING,
                middle + NUMBER_OFFSET);
        g.drawString(shorten(TrackInfo.artist(this.path), smallMetrics,
                textWidth), TEXT_LEFT, middle + ARTIST_OFFSET);

        g.setFont(Theme.font(Theme.SANS, Font.BOLD, TITLE_FONT));
        if (isCurrent) {
            g.setColor(Theme.TEXT);
        } else {
            g.setColor(TITLE_COLOR);
        }
        g.drawString(shorten(TrackInfo.title(this.path), g.getFontMetrics(),
                textWidth), TEXT_LEFT, middle - TITLE_OFFSET);
        g.dispose();
    }

    /**
     * Draws three little bars that bounce while music plays.
     *
     * @param g
     *            The graphics to draw with.
     * @param middle
     *            The y of the middle of the row.
     */
    private void drawEqualizer(Graphics2D g, int middle) {
        double time = System.nanoTime() / NANOSECONDS;
        g.setColor(this.accent);

        for (int bar = 0; bar < EQUALIZER_BARS; bar++) {
            double speed = EQUALIZER_SPEED + bar * EQUALIZER_SPEED_STEP;
            double bounce = Math.abs(Math.sin(time * speed + bar));
            double level = EQUALIZER_MIN + (1 - EQUALIZER_MIN) * bounce;
            int height = (int) (EQUALIZER_HEIGHT * level);
            int x = EQUALIZER_LEFT + bar * EQUALIZER_STEP;
            int y = middle + EQUALIZER_BOTTOM - height;
            g.fillRoundRect(x, y, EQUALIZER_BAR_WIDTH, height, 2, 2);
        }
    }

    /**
     * Shortens text with "..." so it fits in a width.
     *
     * @param text
     *            The text.
     * @param metrics
     *            The font metrics used to measure the text.
     * @param maxWidth
     *            The most space the text can take.
     * @return the text, shortened if it was too long.
     */
    private static String shorten(String text, FontMetrics metrics,
            int maxWidth) {
        if (metrics.stringWidth(text) <= maxWidth) {
            return text;
        }
        String shorter = text;
        while (shorter.length() > 1
                && metrics.stringWidth(shorter + "...") > maxWidth) {
            shorter = shorter.substring(0, shorter.length() - 1);
        }
        return shorter + "...";
    }
}
