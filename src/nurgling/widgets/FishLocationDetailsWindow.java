package nurgling.widgets;

import haven.*;
import nurgling.FishLocation;
import nurgling.NGameUI;

/**
 * Window that displays detailed information about a saved fish location
 */
public class FishLocationDetailsWindow extends Window {
    private final FishLocation fishLocation;
    private final NGameUI gui;

    public FishLocationDetailsWindow(FishLocation fishLocation, NGameUI gui) {
        super(UI.scale(300, 250), "Fish Location Details", true);
        this.fishLocation = fishLocation;
        this.gui = gui;

        // Create content
        Widget content = add(new Widget(new Coord(UI.scale(280), UI.scale(200))), UI.scale(10), UI.scale(10));

        int y = 0;
        int lineHeight = UI.scale(18);

        // All text uses same simple font
        content.add(new Label(fishLocation.getFishName()), 0, y);
        y += lineHeight + UI.scale(3);

        content.add(new Label("Catch Rate: " + fishLocation.getPercentage()), 0, y);
        y += lineHeight + UI.scale(5);

        content.add(new Label("Equipment:"), 0, y);
        y += lineHeight;

        content.add(new Label("  Rod: " + fishLocation.getFishingRod()), 0, y);
        y += lineHeight;

        content.add(new Label("  Hook: " + fishLocation.getHook()), 0, y);
        y += lineHeight;

        content.add(new Label("  Line: " + fishLocation.getLine()), 0, y);
        y += lineHeight;

        content.add(new Label("  Bait: " + fishLocation.getBait()), 0, y);
        y += lineHeight + UI.scale(10);

        // Buttons at the bottom
        int buttonY = UI.scale(180);
        int buttonWidth = UI.scale(120);

        // Delete button
        Button deleteBtn = new Button(buttonWidth, "Delete") {
            @Override
            public void click() {
                if (gui != null && gui.fishLocationService != null) {
                    gui.fishLocationService.removeFishLocation(fishLocation.getLocationId());
                    gui.msg("Removed " + fishLocation.getFishName() + " location", java.awt.Color.YELLOW);
                }
                FishLocationDetailsWindow.this.destroy();
            }
        };
        add(deleteBtn, UI.scale(10), buttonY);

        // Close button
        Button closeBtn = new Button(buttonWidth, "Close") {
            @Override
            public void click() {
                FishLocationDetailsWindow.this.destroy();
            }
        };
        add(closeBtn, UI.scale(170), buttonY);

        pack();
    }

    @Override
    public void wdgmsg(Widget sender, String msg, Object... args) {
        if (msg.equals("close")) {
            destroy();
        } else {
            super.wdgmsg(sender, msg, args);
        }
    }
}
