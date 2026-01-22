package nurgling.widgets;

import haven.Resource;
import haven.res.lib.itemtex.ItemTex;
import org.json.JSONObject;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Generates custom 3-state icons (up/down/hover) by compositing
 * an in-game item icon onto a background image.
 */
public class CustomIconGenerator {

    private static final String BACKGROUND_BASE_PATH = "nurgling/background/";
    private static final int MARGIN = 5;

    /**
     * Represents a background icon resource.
     */
    public static class IconBackground {
        private final String id;
        private final String resourcePath;
        private BufferedImage cachedPreview;

        public IconBackground(String id) {
            this.id = id;
            this.resourcePath = BACKGROUND_BASE_PATH + id;
        }

        public String getId() {
            return id;
        }

        /**
         * Loads a specific state image for this background.
         * @param state "u", "d", or "h"
         */
        public BufferedImage loadState(String state) {
            return Resource.loadsimg(resourcePath + "/" + state);
        }

        /**
         * Gets a cached preview image (up state) for display in swatches.
         */
        public BufferedImage getPreview() {
            if (cachedPreview == null) {
                cachedPreview = loadState("u");
            }
            return cachedPreview;
        }
    }

    /**
     * Available background presets.
     * These correspond to resources in nurgling/background/
     */
    public static final List<IconBackground> PRESET_BACKGROUNDS;
    static {
        PRESET_BACKGROUNDS = new ArrayList<>();
        // Backgrounds 1-16
        for (int i = 1; i <= 16; i++) {
            PRESET_BACKGROUNDS.add(new IconBackground(String.valueOf(i)));
        }
    }

    /**
     * Generates a complete set of icons (up, down, hover) for a given background and item.
     *
     * @param background The background to use
     * @param itemResource The VSpec JSONObject for the item (with "static" or "layer" key)
     * @return Array of 3 BufferedImages: [up, down, hover]
     */
    public static BufferedImage[] generateIconSet(IconBackground background, JSONObject itemResource) {
        BufferedImage itemIcon = loadItemIcon(itemResource);

        return new BufferedImage[] {
            generateIcon(background.loadState("u"), itemIcon, "u"),
            generateIcon(background.loadState("d"), itemIcon, "d"),
            generateIcon(background.loadState("h"), itemIcon, "h")
        };
    }

    /**
     * Loads an item icon from a VSpec JSONObject.
     */
    private static BufferedImage loadItemIcon(JSONObject itemResource) {
        if (itemResource == null) {
            return null;
        }
        try {
            return ItemTex.create(itemResource);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Generates a single icon by compositing an item icon onto a background.
     *
     * @param background The background image
     * @param itemIcon The item icon to composite (can be null)
     * @param state The button state: "u" (up), "d" (down), "h" (hover)
     * @return The generated icon
     */
    public static BufferedImage generateIcon(BufferedImage background, BufferedImage itemIcon, String state) {
        int width = background.getWidth();
        int height = background.getHeight();

        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = result.createGraphics();

        // Enable high-quality rendering
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        // Draw background
        g2d.drawImage(background, 0, 0, null);

        // Draw item icon if available
        if (itemIcon != null) {
            drawItemIcon(g2d, itemIcon, width, height, state);
        }

        g2d.dispose();
        return result;
    }

    /**
     * Draws the item icon centered and scaled to fit within the button.
     */
    private static void drawItemIcon(Graphics2D g2d, BufferedImage itemIcon, int buttonWidth, int buttonHeight, String state) {
        int margin = MARGIN;
        int availableWidth = buttonWidth - margin * 2;
        int availableHeight = buttonHeight - margin * 2;

        int srcWidth = itemIcon.getWidth();
        int srcHeight = itemIcon.getHeight();

        // Calculate scale to fit while preserving aspect ratio
        double scale = Math.min(
            (double) availableWidth / srcWidth,
            (double) availableHeight / srcHeight
        );

        int destWidth = (int) (srcWidth * scale);
        int destHeight = (int) (srcHeight * scale);

        // Center the icon
        int x = (buttonWidth - destWidth) / 2;
        int y = (buttonHeight - destHeight) / 2;

        // For pressed state, offset slightly down-right for depth effect
        if (state.equals("d")) {
            x += 1;
            y += 1;
        }

        g2d.drawImage(itemIcon, x, y, destWidth, destHeight, null);
    }
}
