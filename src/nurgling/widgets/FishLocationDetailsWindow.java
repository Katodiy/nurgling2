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
        int lineHeight = UI.scale(20);

        // Fish name (title)
        Text.Foundry titleFont = new Text.Foundry(Text.dfont, 14).aa(true);
        content.add(new Label(fishLocation.getFishName(), titleFont), 0, y);
        y += lineHeight + UI.scale(5);

        // Percentage
        Text.Foundry normalFont = new Text.Foundry(Text.dfont, 10).aa(true);
        content.add(new Label("Catch Rate: " + fishLocation.getPercentage(), normalFont), 0, y);
        y += lineHeight;

        // Separator
        y += UI.scale(5);

        // Equipment section header
        Text.Foundry headerFont = new Text.Foundry(Text.dfont, 12).aa(true);
        content.add(new Label("Equipment Used:", headerFont), 0, y);
        y += lineHeight;

        // Equipment details
        content.add(new Label("Rod: " + fishLocation.getFishingRod(), normalFont), UI.scale(10), y);
        y += lineHeight;

        content.add(new Label("Hook: " + fishLocation.getHook(), normalFont), UI.scale(10), y);
        y += lineHeight;

        content.add(new Label("Line: " + fishLocation.getLine(), normalFont), UI.scale(10), y);
        y += lineHeight;

        content.add(new Label("Bait: " + fishLocation.getBait(), normalFont), UI.scale(10), y);
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
}
