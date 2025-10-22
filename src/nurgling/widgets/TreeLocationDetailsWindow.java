package nurgling.widgets;

import haven.*;
import nurgling.TreeLocation;
import nurgling.NGameUI;

/**
 * Window that displays detailed information about a saved tree location
 * Simplified version of FishLocationDetailsWindow - no equipment, percentage, time, moon
 */
public class TreeLocationDetailsWindow extends Window {
    private final TreeLocation treeLocation;
    private final NGameUI gui;

    public TreeLocationDetailsWindow(TreeLocation treeLocation, NGameUI gui) {
        super(UI.scale(250, 120), "Tree Location Details", true);
        this.treeLocation = treeLocation;
        this.gui = gui;

        // Create content
        Widget content = add(new Widget(new Coord(UI.scale(230), UI.scale(60))), UI.scale(10), UI.scale(10));

        int y = 0;
        int lineHeight = UI.scale(18);

        // Tree name label
        content.add(new Label(treeLocation.getTreeName()), 0, y);
        y += lineHeight;

        // Quantity label
        content.add(new Label("Quantity: " + treeLocation.getQuantity()), 0, y);
        y += lineHeight + UI.scale(10);

        // Buttons at the bottom
        int buttonY = UI.scale(70);
        int buttonWidth = UI.scale(100);

        // Delete button
        Button deleteBtn = new Button(buttonWidth, "Delete") {
            @Override
            public void click() {
                if (gui != null && gui.treeLocationService != null) {
                    gui.treeLocationService.removeTreeLocation(treeLocation.getLocationId());
                    gui.msg("Removed " + treeLocation.getTreeName() + " location", java.awt.Color.YELLOW);
                }
                TreeLocationDetailsWindow.this.destroy();
            }
        };
        add(deleteBtn, UI.scale(10), buttonY);

        // Close button
        Button closeBtn = new Button(buttonWidth, "Close") {
            @Override
            public void click() {
                TreeLocationDetailsWindow.this.destroy();
            }
        };
        add(closeBtn, UI.scale(130), buttonY);

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
