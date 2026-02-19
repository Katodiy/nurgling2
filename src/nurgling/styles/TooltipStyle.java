package nurgling.styles;

import haven.Text;
import haven.UI;
import nurgling.NConfig;
import nurgling.conf.FontSettings;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Centralized style constants and utilities for tooltips.
 * All spacing values are in logical pixels and should be scaled with UI.scale() when used.
 */
public final class TooltipStyle {
    private TooltipStyle() {} // Prevent instantiation

    // ============ FONT UTILITIES ============

    public static Font getOpenSansRegular() {
        FontSettings fontSettings = (FontSettings) NConfig.get(NConfig.Key.fonts);
        return fontSettings != null ? fontSettings.getFont("Open Sans") : null;
    }

    public static Font getOpenSansSemibold() {
        FontSettings fontSettings = (FontSettings) NConfig.get(NConfig.Key.fonts);
        return fontSettings != null ? fontSettings.getFont("Open Sans Semibold") : null;
    }

    /**
     * Create a Text.Foundry with the specified font settings.
     * @param semibold true for Open Sans Semibold, false for Open Sans Regular
     * @param fontSize font size in logical pixels (will be scaled)
     * @param color default text color
     */
    public static Text.Foundry createFoundry(boolean semibold, int fontSize, Color color) {
        Font font = semibold ? getOpenSansSemibold() : getOpenSansRegular();
        int size = UI.scale(fontSize);
        if (font == null) {
            font = new Font("SansSerif", semibold ? Font.BOLD : Font.PLAIN, size);
        } else {
            font = font.deriveFont(Font.PLAIN, (float) size);
        }
        return new Text.Foundry(font, color).aa(true);
    }

    /**
     * Get font descent for a given font size (used for baseline-relative spacing).
     */
    public static int getFontDescent(int fontSize) {
        Font font = getOpenSansRegular();
        int size = UI.scale(fontSize);
        if (font == null) {
            font = new Font("SansSerif", Font.PLAIN, size);
        } else {
            font = font.deriveFont(Font.PLAIN, (float) size);
        }
        BufferedImage tmp = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        FontMetrics fm = tmp.getGraphics().getFontMetrics(font);
        return fm.getDescent();
    }

    /**
     * Crop top of image to first visible pixel, but keep bottom at original position.
     * This ensures baseline-relative spacing.
     */
    public static BufferedImage cropTopOnly(BufferedImage img) {
        if (img == null) {
            return null;
        }
        int width = img.getWidth();
        int height = img.getHeight();
        int alphaThreshold = 128; // Ignore anti-aliased pixels

        // Find top-most row with visible pixels
        int top = 0;
        topSearch:
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int alpha = (img.getRGB(x, y) >> 24) & 0xFF;
                if (alpha > alphaThreshold) {
                    top = y;
                    break topSearch;
                }
            }
        }

        // If no top cropping needed, return original
        if (top == 0) {
            return img;
        }

        // Crop only from the top, keep the bottom at original position
        int newHeight = height - top;
        BufferedImage cropped = new BufferedImage(width, newHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics g = cropped.getGraphics();
        g.drawImage(img, 0, 0, width, newHeight, 0, top, width, height, null);
        g.dispose();

        return cropped;
    }

    // ============ COLORS ============

    /** LP (Learning Points) value color - purple */
    public static final Color COLOR_LP = new Color(210, 178, 255);  // #D2B2FF

    /** LP/H and LP/H/W value color - cyan */
    public static final Color COLOR_LPH = new Color(0, 238, 255);  // #00EEFF

    /** Study time value color - green */
    public static final Color COLOR_STUDY_TIME = new Color(153, 255, 132);  // #99FF84

    /** Mental weight value color - pink */
    public static final Color COLOR_MENTAL_WEIGHT = new Color(255, 148, 232);  // #FF94E8

    /** EXP cost value color - yellow */
    public static final Color COLOR_EXP_COST = new Color(255, 255, 130);  // #FFFF82

    /** Resource path text color - gray */
    public static final Color COLOR_RESOURCE_PATH = new Color(128, 128, 128);

    // ============ FOOD TOOLTIP COLORS ============

    /** Energy value color - green */
    public static final Color COLOR_ENERGY = new Color(100, 255, 100);

    /** Hunger value color - orange */
    public static final Color COLOR_HUNGER = new Color(255, 192, 128);

    /** FEP Sum value color - bright green */
    public static final Color COLOR_FEP_SUM = new Color(128, 255, 0);

    /** FEP/Hunger value color - light green */
    public static final Color COLOR_FEP_HUNGER = new Color(128, 255, 128);

    /** Expected FEP value color - bright green */
    public static final Color COLOR_EXPECTED_FEP = new Color(128, 255, 0);

    /** Delta value color - cyan */
    public static final Color COLOR_DELTA = new Color(0, 196, 255);

    /** Percentage text color - gray */
    public static final Color COLOR_PERCENTAGE = new Color(128, 128, 128);

    /** Tooltip background color - dark green-gray at 90% opacity */
    public static final Color COLOR_TOOLTIP_BG = new Color(37, 43, 41, 230);  // #252B29 @ 90%

    /** Tooltip border color - yellow */
    public static final Color COLOR_TOOLTIP_BORDER = new Color(244, 247, 21, 192);

    // ============ SPACING (logical pixels - use UI.scale()) ============

    /** Spacing between major sections (Name, LP Info group, Resource) */
    public static final int SECTION_SPACING = 10;

    /** Spacing within LP Info group (LP line, Study time, Mental weight) */
    public static final int INTERNAL_SPACING = 7;

    /** Horizontal spacing between elements on the same line */
    public static final int HORIZONTAL_SPACING = 7;

    /** Spacing between quality icon and quality number */
    public static final int ICON_TO_TEXT_SPACING = 3;

    /** Outer padding around tooltip content (top, left, right) */
    public static final int OUTER_PADDING = 10;

    /** Outer padding for bottom of tooltip content */
    public static final int OUTER_PADDING_BOTTOM = 7;

    /** GLPanel background margin beyond tooltip content */
    public static final int GLPANEL_MARGIN = 2;

    // ============ FONT SIZES (logical pixels - use UI.scale()) ============

    /** Font size for item name and quality */
    public static final int FONT_SIZE_NAME = 12;

    /** Font size for curio stats (LP, study time, etc.) */
    public static final int FONT_SIZE_BODY = 11;

    /** Font size for resource path */
    public static final int FONT_SIZE_RESOURCE = 9;
}
