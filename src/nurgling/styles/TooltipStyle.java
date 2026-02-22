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

    /** Negative stat value color - red */
    public static final Color COLOR_NEGATIVE_STAT = new Color(255, 100, 100);  // #FF6464

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

    // ============ FOOD TOOLTIP SPECIFIC COLORS ============

    /** Food energy value color - cyan */
    public static final Color COLOR_FOOD_ENERGY = new Color(0x00, 0xEE, 0xFF);  // #00EEFF

    /** Food hunger value color - yellow */
    public static final Color COLOR_FOOD_HUNGER = new Color(0xFF, 0xFF, 0x82);  // #FFFF82

    /** FEP Sum value color - bright green */
    public static final Color COLOR_FOOD_FEP_SUM = new Color(0x44, 0xFF, 0x1F);  // #44FF1F

    /** FEP/Hunger value color - light green */
    public static final Color COLOR_FOOD_FEP_HUNGER = new Color(0x99, 0xFF, 0x84);  // #99FF84

    /** Food type name color - light green */
    public static final Color COLOR_FOOD_TYPE = new Color(0x99, 0xFF, 0x84);  // #99FF84

    /** Vessel/drink name color - yellow */
    public static final Color COLOR_FOOD_VESSEL = new Color(0xFF, 0xFF, 0x82);  // #FFFF82

    /** Tooltip background color - dark green-gray at 90% opacity */
    public static final Color COLOR_TOOLTIP_BG = new Color(37, 43, 41, 230);  // #252B29 @ 90%

    /** Tooltip border color - yellow */
    public static final Color COLOR_TOOLTIP_BORDER = new Color(244, 247, 21, 192);

    // ============ SPACING (logical pixels - use UI.scale()) ============

    /** Spacing between major sections (Name, LP Info group, Resource) */
    public static final int SECTION_SPACING = 10;

    /** Spacing within LP Info group (LP line, Study time, Mental weight) */
    public static final int INTERNAL_SPACING = 7;

    /** Spacing within gilding sections (header to stats) */
    public static final int GILDING_INTERNAL_SPACING = 6;

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

    // ============ ICON SIZES (logical pixels - use UI.scale()) ============

    /** Icon size for tooltips (80% of standard 16px) */
    public static final int ICON_SIZE = 13;

    // ============ IMAGE COMPOSITION UTILITIES ============

    /**
     * Compose two images horizontally without gap.
     * Both images are vertically centered.
     */
    public static BufferedImage composePair(BufferedImage labelImg, BufferedImage valueImg) {
        int totalWidth = labelImg.getWidth() + valueImg.getWidth();
        int maxHeight = Math.max(labelImg.getHeight(), valueImg.getHeight());

        BufferedImage result = haven.TexI.mkbuf(new haven.Coord(totalWidth, maxHeight));
        java.awt.Graphics g = result.getGraphics();
        g.drawImage(labelImg, 0, (maxHeight - labelImg.getHeight()) / 2, null);
        g.drawImage(valueImg, labelImg.getWidth(), (maxHeight - valueImg.getHeight()) / 2, null);
        g.dispose();
        return result;
    }

    /**
     * Compose list of images horizontally with specified gap between each image.
     * All images are vertically centered.
     */
    public static BufferedImage composeHorizontalWithGap(java.util.List<BufferedImage> imgs, int gap) {
        if (imgs.isEmpty()) return null;

        int totalWidth = 0;
        int maxHeight = 0;
        for (BufferedImage img : imgs) {
            totalWidth += img.getWidth();
            maxHeight = Math.max(maxHeight, img.getHeight());
        }
        totalWidth += gap * (imgs.size() - 1);

        BufferedImage result = haven.TexI.mkbuf(new haven.Coord(totalWidth, maxHeight));
        java.awt.Graphics g = result.getGraphics();
        int x = 0;
        for (int i = 0; i < imgs.size(); i++) {
            BufferedImage img = imgs.get(i);
            g.drawImage(img, x, (maxHeight - img.getHeight()) / 2, null);
            x += img.getWidth();
            if (i < imgs.size() - 1) {
                x += gap;
            }
        }
        g.dispose();
        return result;
    }

    // ============ ICON-TEXT ALIGNMENT UTILITIES ============

    /**
     * Element for composing lines with mixed text and icons.
     * Distinguishes between text (which defines line height) and icons (which are centered).
     */
    public static class LineElement {
        public final BufferedImage image;
        public final boolean isIcon;

        private LineElement(BufferedImage image, boolean isIcon) {
            this.image = image;
            this.isIcon = isIcon;
        }

        public static LineElement text(BufferedImage img) {
            return new LineElement(img, false);
        }

        public static LineElement icon(BufferedImage img) {
            return new LineElement(img, true);
        }
    }

    /**
     * Result of composing elements - contains image and text positioning info for baseline-relative spacing.
     */
    public static class IconLineResult {
        public final BufferedImage image;
        public final int textTopOffset;     // Pixels from image top to text top
        public final int textBottomOffset;  // Pixels from text bottom to image bottom

        public IconLineResult(BufferedImage image, int textTopOffset, int textBottomOffset) {
            this.image = image;
            this.textTopOffset = textTopOffset;
            this.textBottomOffset = textBottomOffset;
        }
    }

    /**
     * Compose multiple elements (text and icons) horizontally with proper center-to-center alignment.
     * TEXT elements define the line height - icons are centered to the visual center of text.
     *
     * The key insight: text images include descent below the baseline, so the visual text center
     * is NOT at height/2. We shift text DOWN by descent/2 so its visual center aligns with icon center.
     *
     * @param gap Spacing between elements (in pixels, already scaled)
     * @param elements List of text and icon elements
     * @return IconLineResult with composed image and text positioning info
     */
    public static IconLineResult composeElements(int gap, java.util.List<LineElement> elements) {
        if (elements.isEmpty()) {
            return new IconLineResult(haven.TexI.mkbuf(new haven.Coord(1, 1)), 0, 0);
        }

        // Get font descent - text images include descent below baseline
        // We need to account for this when centering to align visual text center with icon center
        int descent = getFontDescent(FONT_SIZE_BODY);

        // First pass: find max text height (only from non-icon elements)
        int maxTextHeight = 0;
        for (LineElement elem : elements) {
            if (!elem.isIcon) {
                maxTextHeight = Math.max(maxTextHeight, elem.image.getHeight());
            }
        }

        // If no text elements, fall back to max of all heights
        if (maxTextHeight == 0) {
            for (LineElement elem : elements) {
                maxTextHeight = Math.max(maxTextHeight, elem.image.getHeight());
            }
        }

        // Find max icon height to determine total line height
        int maxIconHeight = 0;
        for (LineElement elem : elements) {
            if (elem.isIcon) {
                maxIconHeight = Math.max(maxIconHeight, elem.image.getHeight());
            }
        }

        // Total height: text height + any icon extension above/below
        int iconExtension = Math.max(0, (maxIconHeight - maxTextHeight) / 2);
        int totalHeight = maxTextHeight + iconExtension * 2;

        // Calculate actual text Y position (accounting for descent shift for visual alignment)
        // Text is centered then shifted down by descent/2
        int textTopOffset = (totalHeight - maxTextHeight) / 2 + descent / 2;
        // Text bottom offset is NOT the same due to descent shift
        int textBottomOffset = totalHeight - textTopOffset - maxTextHeight;

        // Calculate total width
        int totalWidth = 0;
        for (int i = 0; i < elements.size(); i++) {
            totalWidth += elements.get(i).image.getWidth();
            if (i > 0) totalWidth += gap;
        }

        // Create result image
        BufferedImage result = haven.TexI.mkbuf(new haven.Coord(totalWidth, totalHeight));
        java.awt.Graphics g = result.getGraphics();

        int x = 0;
        for (int i = 0; i < elements.size(); i++) {
            LineElement elem = elements.get(i);
            int y;
            if (elem.isIcon) {
                // Center icon vertically (simple centering)
                y = (totalHeight - elem.image.getHeight()) / 2;
            } else {
                // Center text, but adjust for descent so visual text center aligns with icon center
                // Text visual center is at (height - descent) / 2 from top, not height / 2
                // So we shift text DOWN by descent / 2 to compensate
                y = (totalHeight - elem.image.getHeight()) / 2 + descent / 2;
            }

            g.drawImage(elem.image, x, y, null);
            x += elem.image.getWidth();
            if (i < elements.size() - 1) x += gap;
        }

        g.dispose();

        return new IconLineResult(result, textTopOffset, textBottomOffset);
    }

    /**
     * Convenience method to compose a single icon with text.
     * Icon is centered to the visual center of the text (accounts for font descent).
     */
    public static IconLineResult composeIconText(BufferedImage icon, BufferedImage text, int gap) {
        java.util.List<LineElement> elements = new java.util.ArrayList<>();
        elements.add(LineElement.icon(icon));
        elements.add(LineElement.text(text));
        return composeElements(gap, elements);
    }
}
