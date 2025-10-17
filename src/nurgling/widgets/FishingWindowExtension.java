package nurgling.widgets;

import haven.*;
import nurgling.NGameUI;
import nurgling.NUtils;

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
            if (tc instanceof Button && "Save".equals(((Button)tc).text.text)) {
                // Buttons already added to this window
                return;
            }
        }


        // Collect all labels with ":" first (we iterate backwards, so collect in reverse order)
        java.util.List<Label> colonLabels = new java.util.ArrayList<>();
        for (Widget tc = window.lchild; tc != null; tc = tc.prev) {
            if (tc instanceof Label) {
                String text = ((Label)tc).text();
                if (text != null && text.endsWith(":")) {
                    colonLabels.add((Label)tc);
                }
            }
        }

        // Skip the last one in the list (which is the first/top one in the window - the title)
        Map<String, Label> fishLabels = new HashMap<>();
        for (int i = 0; i < colonLabels.size() - 1; i++) {
            Label fishLabel = colonLabels.get(i);
            String text = fishLabel.text();
            // Remove the trailing ":"
            String fishName = text.substring(0, text.length() - 1).trim();
            if (!fishName.isEmpty()) {
                fishLabels.put(fishName, fishLabel);
            }
        }

        if (fishLabels.isEmpty()) return;

        // Add a small "Save" button next to each fish label
        for (Map.Entry<String, Label> entry : fishLabels.entrySet()) {
            String fishName = entry.getKey();
            Label fishLabel = entry.getValue();

            // Create a small save button for this fish
            Button saveBtn = new Button(UI.scale(40), "Save") {
                @Override
                public void click() {
                    Gob player = NUtils.player();
                    if (player != null && gui.fishLocationService != null) {
                        gui.fishLocationService.saveFishLocation(fishName, player.rc);
                    }
                }
            };

            // Position the button to the right of the fish label, vertically centered
            int buttonX = fishLabel.c.x + fishLabel.sz.x + UI.scale(5);
            // Center button vertically relative to label
            int buttonY = fishLabel.c.y + (fishLabel.sz.y - saveBtn.sz.y) / 2;
            Coord buttonPos = new Coord(buttonX, buttonY);
            window.add(saveBtn, buttonPos);
        }
    }

}
