package nurgling.widgets;

import haven.Resource;
import haven.UI;
import haven.res.lib.itemtex.ItemTex;
import org.json.JSONObject;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Generates custom 3-state icons (up/down/hover) by compositing
 * an in-game item icon onto a colored background.
 */
public class CustomIconGenerator {

    private static final int ICON_SIZE = 32;
    private static final int CORNER_RADIUS = 6;
    private static final int MARGIN = 3;
    private static final int BORDER_WIDTH = 1;

    /**
     * Generates a complete set of icons (up, down, hover) for a given color and item.
     *
     * @param baseColor The base background color
     * @param itemResource The VSpec JSONObject for the item (with "static" or "layer" key)
     * @return Array of 3 BufferedImages: [up, down, hover]
     */
    public static BufferedImage[] generateIconSet(Color baseColor, JSONObject itemResource) {
        BufferedImage itemIcon = null;
        if (itemResource != null) {
            try {
                itemIcon = ItemTex.create(itemResource);
            } catch (Exception e) {
                // Failed to load item icon, will generate without it
            }
        }

        return new BufferedImage[] {
            generateIcon(baseColor, itemIcon, "u"),
            generateIcon(baseColor, itemIcon, "d"),
            generateIcon(baseColor, itemIcon, "h")
        };
    }

    /**
     * Generates a single icon for a specific state.
     *
     * @param baseColor The base background color
     * @param itemIcon The item icon to composite (can be null)
     * @param state The button state: "u" (up), "d" (down), "h" (hover)
     * @return The generated icon
     */
    public static BufferedImage generateIcon(Color baseColor, BufferedImage itemIcon, String state) {
        int size = UI.scale(ICON_SIZE);
        BufferedImage icon = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = icon.createGraphics();

        // Enable high-quality rendering
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        // Calculate state-adjusted colors
        Color bgColor = adjustColorForState(baseColor, state);
        Color borderColor = adjustColorForState(darken(baseColor, 0.3f), state);

        int margin = UI.scale(2);
        int radius = UI.scale(CORNER_RADIUS);

        // Draw shadow for depth (only for up and hover states)
        if (!state.equals("d")) {
            g2d.setColor(new Color(0, 0, 0, 40));
            g2d.fillRoundRect(margin + 1, margin + 1, size - margin * 2, size - margin * 2, radius, radius);
        }

        // Draw background
        g2d.setColor(bgColor);
        g2d.fillRoundRect(margin, margin, size - margin * 2 - 1, size - margin * 2 - 1, radius, radius);

        // Draw border
        g2d.setColor(borderColor);
        g2d.setStroke(new BasicStroke(UI.scale(BORDER_WIDTH)));
        g2d.drawRoundRect(margin, margin, size - margin * 2 - 1, size - margin * 2 - 1, radius, radius);

        // Draw item icon if available
        if (itemIcon != null) {
            drawItemIcon(g2d, itemIcon, size, state);
        }

        g2d.dispose();
        return icon;
    }

    /**
     * Draws the item icon centered and scaled to fit within the button.
     */
    private static void drawItemIcon(Graphics2D g2d, BufferedImage itemIcon, int buttonSize, String state) {
        int margin = UI.scale(MARGIN + 1);
        int availableSize = buttonSize - margin * 2;

        int srcWidth = itemIcon.getWidth();
        int srcHeight = itemIcon.getHeight();

        // Calculate scale to fit while preserving aspect ratio
        double scale = Math.min(
            (double) availableSize / srcWidth,
            (double) availableSize / srcHeight
        );

        int destWidth = (int) (srcWidth * scale);
        int destHeight = (int) (srcHeight * scale);

        // Center the icon
        int x = (buttonSize - destWidth) / 2;
        int y = (buttonSize - destHeight) / 2;

        // For pressed state, offset slightly down-right for depth effect
        if (state.equals("d")) {
            x += UI.scale(1);
            y += UI.scale(1);
        }

        g2d.drawImage(itemIcon, x, y, destWidth, destHeight, null);
    }

    /**
     * Adjusts a color for the given button state using HSB color space.
     */
    public static Color adjustColorForState(Color base, String state) {
        float[] hsb = Color.RGBtoHSB(base.getRed(), base.getGreen(), base.getBlue(), null);

        switch (state) {
            case "d": // Down/pressed - darker
                hsb[2] = Math.max(0f, hsb[2] - 0.15f);
                break;
            case "h": // Hover - brighter
                hsb[2] = Math.min(1f, hsb[2] + 0.1f);
                hsb[1] = Math.max(0f, hsb[1] - 0.05f); // Slightly desaturate
                break;
            case "u": // Up/normal - as-is
            default:
                break;
        }

        return Color.getHSBColor(hsb[0], hsb[1], hsb[2]);
    }

    /**
     * Darkens a color by a factor.
     */
    public static Color darken(Color color, float factor) {
        float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
        hsb[2] = Math.max(0f, hsb[2] - factor);
        return Color.getHSBColor(hsb[0], hsb[1], hsb[2]);
    }

    /**
     * Lightens a color by a factor.
     */
    public static Color lighten(Color color, float factor) {
        float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
        hsb[2] = Math.min(1f, hsb[2] + factor);
        return Color.getHSBColor(hsb[0], hsb[1], hsb[2]);
    }

    /**
     * Predefined color palette that works well for bot icons.
     */
    public static final Color[] PRESET_COLORS = {
        // Blues
        new Color(70, 130, 180),   // Steel Blue
        new Color(100, 149, 237),  // Cornflower Blue
        new Color(65, 105, 225),   // Royal Blue

        // Greens
        new Color(60, 179, 113),   // Medium Sea Green
        new Color(46, 139, 87),    // Sea Green
        new Color(85, 107, 47),    // Dark Olive Green

        // Reds/Oranges
        new Color(205, 92, 92),    // Indian Red
        new Color(210, 105, 30),   // Chocolate
        new Color(184, 134, 11),   // Dark Goldenrod

        // Purples
        new Color(147, 112, 219),  // Medium Purple
        new Color(138, 43, 226),   // Blue Violet
        new Color(128, 0, 128),    // Purple

        // Neutrals
        new Color(112, 128, 144),  // Slate Gray
        new Color(119, 136, 153),  // Light Slate Gray
        new Color(105, 105, 105),  // Dim Gray

        // Earth tones
        new Color(139, 90, 43),    // Saddle Brown variant
        new Color(160, 82, 45),    // Sienna
        new Color(178, 134, 101),  // Tan variant
    };

    /**
     * Generates a preview icon (just the up state) for quick display.
     */
    public static BufferedImage generatePreview(Color baseColor, JSONObject itemResource) {
        BufferedImage itemIcon = null;
        if (itemResource != null) {
            try {
                itemIcon = ItemTex.create(itemResource);
            } catch (Exception e) {
                // Failed to load
            }
        }
        return generateIcon(baseColor, itemIcon, "u");
    }
}
