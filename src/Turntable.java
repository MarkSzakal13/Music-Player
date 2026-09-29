import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.RoundRectangle2D;

import javax.swing.JComponent;

/**
 * Draws a vinyl record that spins while music plays, with a tonearm that
 * moves onto the record when a song is playing.
 */
public final class Turntable extends JComponent {

    /**
     * Serialization id.
     */
    private static final long serialVersionUID = 1L;
    /**
     * Preferred width.
     */
    private static final int WIDTH = 460;
    /**
     * Preferred height.
     */
    private static final int HEIGHT = 380;
    /**
     * Smallest width.
     */
    private static final int MIN_WIDTH = 200;
    /**
     * Smallest height.
     */
    private static final int MIN_HEIGHT = 180;

    /**
     * Record radius compared to the component width.
     */
    private static final double RADIUS_OF_WIDTH = 0.4;
    /**
     * Record radius compared to the component height.
     */
    private static final double RADIUS_OF_HEIGHT = 0.47;
    /**
     * How far left of center the record sits, to leave room for the arm.
     */
    private static final double SHIFT_LEFT = 0.12;
    /**
     * Size of the shadow compared to the record.
     */
    private static final double SHADOW_SIZE = 1.12;
    /**
     * How far down the shadow is, compared to the record.
     */
    private static final double SHADOW_DROP = 0.06;
    /**
     * Where the shadow starts to fade.
     */
    private static final float SHADOW_FADE = 0.8f;
    /**
     * Darkness of the shadow.
     */
    private static final int SHADOW_ALPHA = 150;
    /**
     * Color at the center of the record.
     */
    private static final Color RECORD_CENTER = new Color(0x1E1B22);
    /**
     * Color at the edge of the record.
     */
    private static final Color RECORD_EDGE = new Color(0x0B0A0D);

    /**
     * Where the grooves start, compared to the record.
     */
    private static final double GROOVE_START = 0.37;
    /**
     * Where the grooves end, compared to the record.
     */
    private static final double GROOVE_END = 0.97;
    /**
     * Space between grooves, compared to the record.
     */
    private static final double GROOVE_SPACING = 0.018;
    /**
     * Faintest groove.
     */
    private static final int GROOVE_ALPHA = 10;
    /**
     * Number used to vary how bright each groove is.
     */
    private static final double GROOVE_VARIATION = 57;

    /**
     * Angles where light reflects off the record.
     */
    private static final int[] SHINE_ANGLES = { 28, 208 };
    /**
     * Width of each reflection, in degrees.
     */
    private static final int SHINE_WIDTH = 34;
    /**
     * Where the reflection fades in and out.
     */
    private static final float[] SHINE_STOPS = { 0.3f, 0.7f, 1f };
    /**
     * Brightness of the reflection.
     */
    private static final int SHINE_ALPHA = 34;

    /**
     * Size of the label compared to the record.
     */
    private static final double LABEL_SIZE = 0.33;
    /**
     * Dark color mixed into the label.
     */
    private static final Color LABEL_DARK = new Color(0x2A1030);
    /**
     * How much of the dark color is mixed in at the label's edge.
     */
    private static final double LABEL_DARKEN = 0.55;
    /**
     * Color of the text printed on the label.
     */
    private static final Color LABEL_INK = new Color(20, 12, 24, 210);
    /**
     * Most letters of the title printed on the label.
     */
    private static final int LABEL_MAX_LETTERS = 14;
    /**
     * Title font size compared to the label.
     */
    private static final double LABEL_TITLE_FONT = 0.17;
    /**
     * Small text font size compared to the label.
     */
    private static final double LABEL_SMALL_FONT = 0.11;
    /**
     * Where the small text sits below the center, compared to the label.
     */
    private static final double LABEL_SMALL_OFFSET = 0.5;
    /**
     * Smallest font on the label.
     */
    private static final float LABEL_MIN_FONT = 8f;
    /**
     * Size of the spindle compared to the record.
     */
    private static final double SPINDLE_SIZE = 0.022;
    /**
     * Color of metal parts.
     */
    private static final Color METAL = new Color(0xD8D2CC);

    /**
     * Where the arm pivot is, to the right of center, compared to the record.
     */
    private static final double PIVOT_RIGHT = 1.08;
    /**
     * Where the arm pivot is, above center, compared to the record.
     */
    private static final double PIVOT_UP = 0.78;
    /**
     * Length of the arm compared to the record.
     */
    private static final double ARM_LENGTH = 1.32;
    /**
     * Angle of the arm when it is resting.
     */
    private static final double ARM_REST_ANGLE = 95;
    /**
     * Angle of the arm when it is playing.
     */
    private static final double ARM_PLAY_ANGLE = 122;
    /**
     * Size of the arm base compared to the record.
     */
    private static final double BASE_SIZE = 0.13;
    /**
     * Size of the pivot cap compared to the record.
     */
    private static final double CAP_SIZE = 0.045;
    /**
     * Thickness of the arm compared to the record.
     */
    private static final double ARM_THICKNESS = 0.028;
    /**
     * Thinnest the arm can be.
     */
    private static final float MIN_ARM_THICKNESS = 3f;
    /**
     * Offset of the arm's shadow.
     */
    private static final int ARM_SHADOW_OFFSET = 3;
    /**
     * Darkness of the arm's shadow.
     */
    private static final int ARM_SHADOW_ALPHA = 90;
    /**
     * Light end of the arm.
     */
    private static final Color ARM_LIGHT = new Color(0xE9E4DE);
    /**
     * Dark end of the arm.
     */
    private static final Color ARM_DARK = new Color(0x9C96A0);
    /**
     * Light color of the arm base.
     */
    private static final Color BASE_LIGHT = new Color(0x55505C);
    /**
     * Extra turn of the head shell, in degrees.
     */
    private static final double HEAD_TURN = 20;
    /**
     * Length of the head shell compared to the record.
     */
    private static final double HEAD_LENGTH = 0.16;
    /**
     * Width of the head shell compared to the record.
     */
    private static final double HEAD_WIDTH = 0.075;
    /**
     * Color of the head shell.
     */
    private static final Color HEAD_COLOR = new Color(0xCFC8C2);
    /**
     * Where the colored tip starts along the head shell.
     */
    private static final double TIP_START = 0.5;
    /**
     * Length of the colored tip compared to the head shell.
     */
    private static final double TIP_LENGTH = 0.25;
    /**
     * Rounding of the head shell corners.
     */
    private static final double HEAD_ROUNDING = 0.6;
    /**
     * Rounding of the tip corners.
     */
    private static final double TIP_ROUNDING = 0.3;
    /**
     * Amount of the head shell behind the end of the arm.
     */
    private static final double HEAD_BEHIND = 0.2;

    /**
     * How far the record has turned, in degrees.
     */
    private double angle;
    /**
     * Arm position from 0 (resting) to 1 (on the record).
     */
    private double armPosition;
    /**
     * The current accent color.
     */
    private Color accent = Theme.IDLE_ACCENT;
    /**
     * Title printed on the record label.
     */
    private String title = "NOCTURNE";

    /**
     * Creates the turntable.
     */
    public Turntable() {
        this.setPreferredSize(new Dimension(WIDTH, HEIGHT));
        this.setMinimumSize(new Dimension(MIN_WIDTH, MIN_HEIGHT));
        this.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        this.setToolTipText("Click to play or pause");
    }

    /**
     * Updates what the turntable shows. Called every animation frame.
     *
     * @param angle
     *            How far the record has turned, in degrees.
     * @param armPosition
     *            Arm position from 0 (resting) to 1 (on the record).
     * @param accent
     *            The current accent color.
     * @param title
     *            Title to print on the label.
     */
    public void update(double angle, double armPosition, Color accent,
            String title) {
        this.angle = angle;
        this.armPosition = armPosition;
        this.accent = accent;
        this.title = title;
        this.repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = Theme.smooth(graphics);
        double radius = Math.min(this.getWidth() * RADIUS_OF_WIDTH,
                this.getHeight() * RADIUS_OF_HEIGHT);
        double centerX = this.getWidth() / 2.0 - radius * SHIFT_LEFT;
        double centerY = this.getHeight() / 2.0;

        this.drawRecord(g, centerX, centerY, radius);
        this.drawLabel(g, centerX, centerY, radius);
        this.drawArm(g, centerX, centerY, radius);
        g.dispose();
    }

    /**
     * Draws the black record with its shadow, grooves, and reflections.
     *
     * @param g
     *            The graphics to draw with.
     * @param centerX
     *            The x of the record's center.
     * @param centerY
     *            The y of the record's center.
     * @param radius
     *            The record's radius.
     */
    private void drawRecord(Graphics2D g, double centerX, double centerY,
            double radius) {
        double shadowY = centerY + radius * SHADOW_DROP;
        double shadowRadius = radius * SHADOW_SIZE;
        g.setPaint(new RadialGradientPaint((float) centerX, (float) shadowY,
                (float) shadowRadius, new float[] { SHADOW_FADE, 1f },
                new Color[] { new Color(0, 0, 0, SHADOW_ALPHA),
                    new Color(0, 0, 0, 0) }));
        g.fill(circle(centerX, shadowY, shadowRadius));

        g.setPaint(new RadialGradientPaint((float) centerX, (float) centerY,
                (float) radius, new float[] { 0f, 1f },
                new Color[] { RECORD_CENTER, RECORD_EDGE }));
        g.fill(circle(centerX, centerY, radius));

        g.setStroke(new BasicStroke(1f));
        for (double ring = GROOVE_START; ring < GROOVE_END;
                ring += GROOVE_SPACING) {
            // Vary the brightness a little so the grooves look real.
            double variation = Math.abs(Math.sin(ring * GROOVE_VARIATION));
            int alpha = (int) (GROOVE_ALPHA + GROOVE_ALPHA * variation);
            g.setColor(Theme.withAlpha(Color.WHITE, alpha));
            g.draw(circle(centerX, centerY, radius * ring));
        }

        Color clear = Theme.withAlpha(Color.WHITE, 0);
        Color shine = Theme.withAlpha(Color.WHITE, SHINE_ALPHA);
        for (int start : SHINE_ANGLES) {
            g.setPaint(new RadialGradientPaint((float) centerX,
                    (float) centerY, (float) radius, SHINE_STOPS,
                    new Color[] { clear, shine, clear }));
            g.fill(new Arc2D.Double(centerX - radius, centerY - radius,
                    2 * radius, 2 * radius, start, SHINE_WIDTH, Arc2D.PIE));
        }
    }

    /**
     * Draws the colored label in the middle of the record. The label turns
     * with the record.
     *
     * @param g
     *            The graphics to draw with.
     * @param centerX
     *            The x of the record's center.
     * @param centerY
     *            The y of the record's center.
     * @param radius
     *            The record's radius.
     */
    private void drawLabel(Graphics2D g, double centerX, double centerY,
            double radius) {
        Graphics2D label = (Graphics2D) g.create();
        label.rotate(Math.toRadians(this.angle), centerX, centerY);
        double labelRadius = radius * LABEL_SIZE;

        Color edge = Theme.blend(this.accent, LABEL_DARK, LABEL_DARKEN);
        label.setPaint(new GradientPaint((float) (centerX - labelRadius),
                (float) (centerY - labelRadius), this.accent,
                (float) (centerX + labelRadius),
                (float) (centerY + labelRadius), edge));
        label.fill(circle(centerX, centerY, labelRadius));

        String text = this.title.toUpperCase();
        if (text.length() > LABEL_MAX_LETTERS) {
            text = text.substring(0, LABEL_MAX_LETTERS - 1) + "...";
        }
        label.setColor(LABEL_INK);
        float titleSize = (float) Math.max(LABEL_MIN_FONT,
                labelRadius * LABEL_TITLE_FONT);
        label.setFont(Theme.font(Theme.SERIF, Font.BOLD, titleSize));
        drawCentered(label, text, centerX, centerY - labelRadius / 2);

        float smallSize = (float) Math.max(LABEL_MIN_FONT,
                labelRadius * LABEL_SMALL_FONT);
        label.setFont(Theme.font(Theme.SANS, Font.BOLD, smallSize));
        drawCentered(label, "SIDE A  33 1/3", centerX,
                centerY + labelRadius * LABEL_SMALL_OFFSET);
        label.dispose();

        g.setColor(METAL);
        g.fill(circle(centerX, centerY, radius * SPINDLE_SIZE));
    }

    /**
     * Draws the tonearm. It swings from its resting angle onto the record as
     * armPosition goes from 0 to 1.
     *
     * @param g
     *            The graphics to draw with.
     * @param centerX
     *            The x of the record's center.
     * @param centerY
     *            The y of the record's center.
     * @param radius
     *            The record's radius.
     */
    private void drawArm(Graphics2D g, double centerX, double centerY,
            double radius) {
        double pivotX = centerX + radius * PIVOT_RIGHT;
        double pivotY = centerY - radius * PIVOT_UP;
        double degrees = ARM_REST_ANGLE
                + (ARM_PLAY_ANGLE - ARM_REST_ANGLE) * this.armPosition;
        double armAngle = Math.toRadians(degrees);
        double endX = pivotX + Math.cos(armAngle) * radius * ARM_LENGTH;
        double endY = pivotY + Math.sin(armAngle) * radius * ARM_LENGTH;

        double baseRadius = radius * BASE_SIZE;
        g.setPaint(new RadialGradientPaint((float) pivotX, (float) pivotY,
                (float) baseRadius, new float[] { 0f, 1f },
                new Color[] { BASE_LIGHT, RECORD_CENTER }));
        g.fill(circle(pivotX, pivotY, baseRadius));
        g.setColor(Theme.LINE);
        g.draw(circle(pivotX, pivotY, baseRadius));

        float thickness = (float) Math.max(MIN_ARM_THICKNESS,
                radius * ARM_THICKNESS);
        g.setStroke(new BasicStroke(thickness, BasicStroke.CAP_ROUND,
                BasicStroke.JOIN_ROUND));
        g.setColor(new Color(0, 0, 0, ARM_SHADOW_ALPHA));
        g.draw(new Line2D.Double(pivotX + ARM_SHADOW_OFFSET,
                pivotY + ARM_SHADOW_OFFSET, endX + ARM_SHADOW_OFFSET,
                endY + ARM_SHADOW_OFFSET));
        g.setPaint(new GradientPaint((float) pivotX, (float) pivotY,
                ARM_LIGHT, (float) endX, (float) endY, ARM_DARK));
        g.draw(new Line2D.Double(pivotX, pivotY, endX, endY));

        this.drawHead(g, endX, endY, armAngle, radius);

        g.setColor(METAL);
        g.fill(circle(pivotX, pivotY, radius * CAP_SIZE));
    }

    /**
     * Draws the head shell at the end of the arm.
     *
     * @param g
     *            The graphics to draw with.
     * @param x
     *            The x of the end of the arm.
     * @param y
     *            The y of the end of the arm.
     * @param armAngle
     *            The angle of the arm in radians.
     * @param radius
     *            The record's radius.
     */
    private void drawHead(Graphics2D g, double x, double y, double armAngle,
            double radius) {
        Graphics2D head = (Graphics2D) g.create();
        head.translate(x, y);
        head.rotate(armAngle + Math.toRadians(HEAD_TURN));

        double length = radius * HEAD_LENGTH;
        double width = radius * HEAD_WIDTH;
        head.setColor(HEAD_COLOR);
        head.fill(new RoundRectangle2D.Double(-length * HEAD_BEHIND,
                -width / 2, length, width, width * HEAD_ROUNDING,
                width * HEAD_ROUNDING));
        head.setColor(this.accent);
        head.fill(new RoundRectangle2D.Double(length * TIP_START, -width / 2,
                length * TIP_LENGTH, width, width * TIP_ROUNDING,
                width * TIP_ROUNDING));
        head.dispose();
    }

    /**
     * Draws text centered on a point.
     *
     * @param g
     *            The graphics to draw with.
     * @param text
     *            The text.
     * @param x
     *            The x of the center.
     * @param y
     *            The y of the center.
     */
    private static void drawCentered(Graphics2D g, String text, double x,
            double y) {
        FontMetrics metrics = g.getFontMetrics();
        float left = (float) (x - metrics.stringWidth(text) / 2.0);
        float baseline = (float) (y + metrics.getAscent() / 2.0);
        g.drawString(text, left, baseline);
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
