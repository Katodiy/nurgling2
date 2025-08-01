package nurgling.actions.bots.cheese;

import haven.WItem;
import nurgling.NGameUI;
import nurgling.areas.NContext;
import nurgling.actions.CheeseAreaMatcher;

/**
 * Handles cheese slicing and empty tray management
 */
public class CheeseSlicingManager {
    
    /**
     * Slice cheese and return empty tray to empty tray area
     */
    public void sliceCheeseAndReturnEmptyTray(NGameUI gui, WItem tray) throws InterruptedException {
        // TODO: Implement actual slicing action
        // This would involve:
        // 1. Right-click tray and select "Slice" option
        // 2. Wait for slicing to complete
        // 3. Collect sliced cheese to appropriate storage
        // 4. Handle the now-empty tray
        
        gui.msg("Slicing cheese from tray...");
        
        // After slicing, the tray becomes empty and needs to go back to empty tray area
        returnEmptyTrayToStorage(gui, tray);
    }
    
    /**
     * Return empty tray to the empty cheese tray storage area
     */
    private void returnEmptyTrayToStorage(NGameUI gui, WItem emptyTray) throws InterruptedException {
        gui.msg("Returning empty tray to storage area");
        
        // Find the area configured for empty cheese trays
        try {
            NContext returnContext = new NContext(gui);
            if (returnContext.addOutItem("Empty Cheese Tray", null, 1.0)) {
                // Use the custom area matcher to send empty tray to correct area
                new CheeseAreaMatcher.TransferCheeseTraysToCorrectAreas().run(gui);
            } else if (returnContext.addOutItem("Cheese Tray", null, 1.0)) {
                // Fallback to generic cheese tray area
                new CheeseAreaMatcher.TransferCheeseTraysToCorrectAreas().run(gui);
            } else {
                gui.msg("No empty cheese tray area configured - keeping in inventory");
            }
        } catch (Exception e) {
            gui.msg("Error returning empty tray: " + e.getMessage());
        }
    }
    
    /**
     * Store sliced cheese products in appropriate storage
     */
    public void storeSlicedCheese(NGameUI gui, String cheeseType) throws InterruptedException {
        // TODO: Implement storage of sliced cheese products
        // This would find the appropriate storage area for the cheese type
        // and transfer the sliced cheese there
        gui.msg("Storing sliced " + cheeseType + " cheese (implementation needed)");
    }
}