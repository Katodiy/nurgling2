package nurgling.widgets;

import haven.*;
import nurgling.NGameUI;
import nurgling.NUtils;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

/**
 * Extension for adding fish location saving functionality to the "This is bait" fishing window
 */
public class FishingWindowExtension {

    /**
     * Adds individual "Save" buttons next to each fish in the "This is bait" window
     * @param window The fishing window to extend
     * @param gui The game UI reference
     */
    public static void addSaveFishButton(Window window, NGameUI gui) {
        if (window == null || gui == null || gui.fishLocationService == null) return;

        // Check if buttons already exist in THIS window by looking for our save buttons
        for (Widget tc = window.lchild; tc != null; tc = tc.prev) {
            if (tc instanceof IButton) {
                // Buttons already added to this window
                return;
            }
        }

        // Collect all widgets in normal order
        java.util.List<Widget> widgetList = new java.util.ArrayList<>();
        for (Widget tc = window.lchild; tc != null; tc = tc.prev) {
            widgetList.add(tc);
        }
        // Reverse to get normal order
        java.util.Collections.reverse(widgetList);

        // Build mapping of fish names to percentages
        Map<String, FishEntry> fishEntries = new HashMap<>();

        for (int i = 0; i < widgetList.size(); i++) {
            Widget w = widgetList.get(i);
            if (w instanceof Label) {
                String text = ((Label)w).text();
                // Check if this is a fish name label
                if (text != null && text.endsWith(":") && !text.equals("This is bait:") && !text.equals("There are fish around:")) {
                    String fishName = text.substring(0, text.length() - 1).trim();

                    // Find the LAST percentage label (there are 3 per fish, we want the final one)
                    String percentage = "Unknown";
                    for (int j = i + 1; j < widgetList.size(); j++) {
                        Widget next = widgetList.get(j);
                        if (next instanceof Label) {
                            String nextText = ((Label)next).text();
                            if (nextText != null && nextText.contains("%")) {
                                percentage = nextText; // Keep updating to get the last one
                            }
                        } else if (next instanceof Button) {
                            // Stop when we hit a button (end of this fish's data)
                            break;
                        }
                    }

                    fishEntries.put(fishName, new FishEntry(fishName, percentage, (Label)w));
                }
            }
        }

        if (fishEntries.isEmpty()) return;

        // Add a small "Save" button next to each fish label
        for (Map.Entry<String, FishEntry> entry : fishEntries.entrySet()) {
            FishEntry fishEntry = entry.getValue();

            // Create a small save button for this fish with scaled down image (1.5x smaller)
            BufferedImage imgU = scaleImage(Resource.loadsimg("nurgling/hud/buttons/save/u"), 1.5);
            BufferedImage imgD = scaleImage(Resource.loadsimg("nurgling/hud/buttons/save/d"), 1.5);
            BufferedImage imgH = scaleImage(Resource.loadsimg("nurgling/hud/buttons/save/h"), 1.5);
            IButton saveBtn = new IButton(imgU, imgD, imgH) {
                @Override
                public void click() {
                    super.click();
                    Gob player = NUtils.player();
                    if (player != null && gui.fishLocationService != null) {
                        gui.fishLocationService.saveFishLocation(
                            fishEntry.fishName,
                            fishEntry.percentage,
                            player.rc
                        );
                    }
                }
            };
            saveBtn.settip("Save fish location");

            // Position the button at fixed X position to keep table layout stable
            int buttonX = UI.scale(185);
            // Center button vertically relative to label
            int buttonY = fishEntry.label.c.y + (fishEntry.label.sz.y - saveBtn.sz.y) / 2;
            Coord buttonPos = new Coord(buttonX, buttonY);
            window.add(saveBtn, buttonPos);
        }
    }

    /**
     * Scale down an image by the given factor
     */
    private static BufferedImage scaleImage(BufferedImage original, double factor) {
        int newWidth = (int)(original.getWidth() / factor);
        int newHeight = (int)(original.getHeight() / factor);
        BufferedImage scaled = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = scaled.createGraphics();
        g2d.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(original, 0, 0, newWidth, newHeight, null);
        g2d.dispose();
        return scaled;
    }

    /**
     * Helper class to hold fish entry information
     */
    private static class FishEntry {
        String fishName;
        String percentage;
        Label label;

        FishEntry(String fishName, String percentage, Label label) {
            this.fishName = fishName;
            this.percentage = percentage;
            this.label = label;
        }
    }

}
