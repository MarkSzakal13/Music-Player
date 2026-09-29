import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;

/**
 * Colors, fonts, and drawing helpers shared by every part of the UI.
 */
public final class Theme {

    /**
     * Top color of the window background.
     */
    public static final Color BACKGROUND_TOP = new Color(0x14101F);
    /**
     * Bottom color of the window background.
     */
    public static final Color BACKGROUND_BOTTOM = new Color(0x0A0810);
    /**
     * Main text color.
     */
    public static final Color TEXT = new Color(0xF3EEE6);
    /**
     * Color for less important text.
     */
    public static final Color MUTED_TEXT = new Color(0x8E869C);
    /**
     * Color for error messages.
     */
    public static final Color ERROR_TEXT = new Color(0xFF8A80);
    /**
     * Faint line used for borders.
     */
    public static final Color LINE = new Color(255, 255, 255, 22);
    /**
     * Accent color used when no song is loaded.
     */
    public static final Color IDLE_ACCENT = new Color(0xE8A87C);
    /**
     * Font used for titles.
     */
    public static final String SERIF = pickFont("Georgia",
            "Palatino Linotype", "DejaVu Serif", Font.SERIF);
    /**
     * Font used for everything else.
     */
    public static final String SANS = pickFont("Segoe UI", "Helvetica Neue",
            "DejaVu Sans", Font.SANS_SERIF);

    /**
     * Saturation of the accent colors picked for songs.
     */
    private static final float ACCENT_SATURATION = 0.5f;
    /**
     * Brightness of the accent colors picked for songs.
     */
    private static final float ACCENT_BRIGHTNESS = 0.98f;
    /**
     * Number of different accent hues.
     */
    private static final int HUE_STEPS = 360;

    /**
     * Private constructor so this class is not instantiated.
     */
    private Theme() {
    }

    /**
     * Returns the first font in the list that is installed on this computer.
     *
     * @param names
     *            Font names in order of preference. The last one should be a
     *            font Java always has.
     * @return the name of the font to use.
     */
    private static String pickFont(String... names) {
        String[] installed = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getAvailableFontFamilyNames();

        for (String name : names) {
            for (String font : installed) {
                if (font.equals(name)) {
                    return name;
                }
            }
        }
        return names[names.length - 1];
    }

    /**
     * Creates a font from one of the theme's font families.
     *
     * @param family
     *            Either SERIF or SANS.
     * @param style
     *            Font.PLAIN or Font.BOLD.
     * @param size
     *            The font size.
     * @return the font.
     */
    public static Font font(String family, int style, float size) {
        return new Font(family, style, 1).deriveFont(size);
    }

    /**
     * Returns a copy of a color with a different transparency.
     *
     * @param color
     *            The original color.
     * @param alpha
     *            The transparency from 0 (invisible) to 255 (solid).
     * @return the new color.
     */
    public static Color withAlpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(),
                alpha);
    }

    /**
     * Mixes two colors together.
     *
     * @param from
     *            The first color.
     * @param to
     *            The second color.
     * @param amount
     *            How much of the second color to use, from 0 to 1.
     * @return the mixed color.
     */
    public static Color blend(Color from, Color to, double amount) {
        int red = (int) Math.round(
                from.getRed() + (to.getRed() - from.getRed()) * amount);
        int green = (int) Math.round(
                from.getGreen() + (to.getGreen() - from.getGreen()) * amount);
        int blue = (int) Math.round(
                from.getBlue() + (to.getBlue() - from.getBlue()) * amount);
        return new Color(red, green, blue);
    }

    /**
     * Picks an accent color for a song. The same song always gets the same
     * color.
     *
     * @param title
     *            The title of the song.
     * @return the accent color.
     */
    public static Color accentFor(String title) {
        int step = Math.floorMod(title.hashCode(), HUE_STEPS);
        float hue = (float) step / HUE_STEPS;
        return Color.getHSBColor(hue, ACCENT_SATURATION, ACCENT_BRIGHTNESS);
    }

    /**
     * Creates a copy of a Graphics object with smooth edges and text turned
     * on. The caller should dispose of it when done.
     *
     * @param graphics
     *            The graphics to copy.
     * @return the smooth graphics.
     */
    public static Graphics2D smooth(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,
                RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);
        return g;
    }
}
