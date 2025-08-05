package nurgling.actions.bots.cheese;

import haven.WItem;
import nurgling.*;
import nurgling.actions.SelectFlowerAction;
import nurgling.tasks.NTask;
import nurgling.tools.NAlias;

/**
 * Handles cheese slicing and empty tray management
 */
public class CheeseSlicingManager {
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
        
        // Check inventory space before slicing
        // Slicing typically produces 4-5 cheese pieces + 1 empty tray
        if (gui.getInventory().getFreeSpace() < CheeseConstants.SLICING_INVENTORY_REQUIREMENT) {
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