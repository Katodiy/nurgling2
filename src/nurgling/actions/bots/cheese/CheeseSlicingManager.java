package nurgling.actions.bots.cheese;

import haven.Coord;
import haven.WItem;
import nurgling.*;
import nurgling.actions.SelectFlowerAction;
import nurgling.areas.NContext;
import nurgling.tasks.NTask;
import nurgling.tasks.WaitItems;
import nurgling.tools.NAlias;

import java.util.ArrayList;

/**
 * Handles cheese slicing and empty tray management
 */
public class CheeseSlicingManager {
    
    /**
     * Slice cheese and return empty tray to empty tray area
     */
    /**
     * Slice cheese and return empty tray to storage area
     * This is the main public method that orchestrates the complete slicing process
     */
    public void sliceCheeseAndReturnEmptyTray(NGameUI gui, WItem tray) throws InterruptedException {
        String cheeseType = CheeseUtils.getContentName(tray);
        
        // 1. Slice the cheese
        sliceCheese(gui, tray);
        
        // 2. Store the sliced cheese products
        storeSlicedCheese(gui, cheeseType);
        
        // 3. Return empty tray to storage
        returnEmptyTrayToStorage(gui);
    }
    
    /**
     * Return empty trays to the empty cheese tray storage area
     */
    private void returnEmptyTrayToStorage(NGameUI gui) throws InterruptedException {
        ArrayList<WItem> emptyTrays = gui.getInventory().getItems(new NAlias(CheeseConstants.EMPTY_CHEESE_TRAY_NAME));
        
        if (emptyTrays.isEmpty()) {
            return; // No empty trays to return
        }
        
        gui.msg("Returning " + emptyTrays.size() + " empty tray(s) to storage area");
        
        // Find the area configured for empty cheese trays
        try {
            NContext returnContext = new NContext(gui);
            if (returnContext.addOutItem(CheeseConstants.EMPTY_CHEESE_TRAY_NAME, null, 1.0)) {
                // Successfully configured to return empty trays
                gui.msg("Empty trays will be moved to configured storage area");
            } else if (returnContext.addOutItem(CheeseConstants.CHEESE_TRAY_NAME, null, 1.0)) {
                // Fallback to generic cheese tray area
                gui.msg("Empty trays will be moved to generic cheese tray area");
            } else {
                gui.msg("No empty cheese tray area configured - keeping in inventory");
            }
        } catch (Exception e) {
            gui.msg("Error configuring empty tray return: " + e.getMessage());
        }
    }

    /**
     * Store sliced cheese products in appropriate storage areas
     */
    public void storeSlicedCheese(NGameUI gui, String cheeseType) throws InterruptedException {
        if (cheeseType == null || cheeseType.isEmpty()) {
            return;
        }
        
        NAlias cheeseAlias = new NAlias(cheeseType);
        ArrayList<WItem> cheeseItems = gui.getInventory().getItems(cheeseAlias);
        
        if (cheeseItems.isEmpty()) {
            return; // No cheese to store
        }
        
        gui.msg("Storing " + cheeseItems.size() + " pieces of " + cheeseType);
        
        try {
            NContext storeContext = new NContext(gui);
            if (storeContext.addOutItem(cheeseType, null, 1.0)) {
                gui.msg("Sliced " + cheeseType + " will be moved to configured storage area");
            } else {
                gui.msg("No storage area configured for " + cheeseType + " - keeping in inventory");
            }
        } catch (Exception e) {
            gui.msg("Error configuring cheese storage: " + e.getMessage());
        }
    }

    /**
     * Slice a cheese tray using the "Slice up" action
     * Waits for cheese pieces and empty tray to appear in inventory
     */
    public void sliceCheese(NGameUI gui, WItem tray) throws InterruptedException {
        if (tray == null) {
            gui.msg("Cannot slice: tray is null");
            return;
        }
        
        String cheeseType = CheeseUtils.getContentName(tray);
        if (cheeseType == null || cheeseType.isEmpty()) {
            gui.msg("Cannot slice: unable to determine cheese type");
            return;
        }
        
        gui.msg("Slicing " + cheeseType + " cheese tray...");
        
        // Check inventory space before slicing
        // Slicing typically produces 4-5 cheese pieces + 1 empty tray
        if (gui.getInventory().getFreeSpace() < CheeseConstants.SLICING_INVENTORY_REQUIREMENT) {
            gui.msg("Not enough inventory space for slicing (need " + CheeseConstants.SLICING_INVENTORY_REQUIREMENT + " slots)");
            return;
        }
        
        // Count items before slicing to detect changes
        int initialCheeseCount = getCheeseCount(gui, cheeseType);
        int initialEmptyTrayCount = getEmptyTrayCount(gui);
        
        // Right-click the tray to open flower menu and select "Slice up"
        new SelectFlowerAction("Slice up", tray).run(gui);
        
        // Wait for slicing to complete - check for cheese pieces OR empty tray to appear
        NUtils.addTask(new NTask() {
            @Override
            public boolean check() {
                try {
                    int currentCheeseCount = getCheeseCount(gui, cheeseType);
                    int currentEmptyTrayCount = getEmptyTrayCount(gui);
                    
                    // Success if we got more cheese pieces OR more empty trays
                    return currentCheeseCount > initialCheeseCount || currentEmptyTrayCount > initialEmptyTrayCount;
                } catch (InterruptedException e) {
                    return false;
                }
            }
        });
        
        gui.msg("Successfully sliced " + cheeseType + " - produced " + 
                (getCheeseCount(gui, cheeseType) - initialCheeseCount) + " cheese pieces");
    }
    
    /**
     * Get count of specific cheese type pieces in inventory
     */
    private int getCheeseCount(NGameUI gui, String cheeseType) throws InterruptedException {
        NAlias cheeseAlias = new NAlias(cheeseType);
        return gui.getInventory().getItems(cheeseAlias).size();
    }
    
    /**
     * Get count of empty cheese trays in inventory
     */
    private int getEmptyTrayCount(NGameUI gui) throws InterruptedException {
        return gui.getInventory().getItems(new NAlias(CheeseConstants.EMPTY_CHEESE_TRAY_NAME)).size();
    }
}