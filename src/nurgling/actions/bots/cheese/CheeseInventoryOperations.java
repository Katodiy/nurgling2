package nurgling.actions.bots.cheese;

import haven.WItem;
import nurgling.NGameUI;
import nurgling.tools.Container;

import java.util.ArrayList;

/**
 * Centralizes cheese inventory operations
 * Eliminates repeated inventory query patterns (12+ occurrences)
 */
public class CheeseInventoryOperations {
    
    /**
     * Get all cheese trays from player inventory
     * Replaces: gui.getInventory().getItems(new NAlias("Cheese Tray"))
     * 
     * @param gui The game UI
     * @return List of cheese trays in inventory
     */
    public static ArrayList<WItem> getCheeseTrays(NGameUI gui) throws InterruptedException {
        return gui.getInventory().getItems(CheeseConstants.CHEESE_TRAY_ALIAS);
    }
    
    /**
     * Get all cheese trays from a specific container
     * 
     * @param gui The game UI
     * @param containerType The container type ("Rack", etc.)
     * @return List of cheese trays in the container
     */
    public static ArrayList<WItem> getCheeseTraysFromContainer(NGameUI gui, String containerType) throws InterruptedException {
        return gui.getInventory(containerType).getItems(CheeseConstants.CHEESE_TRAY_ALIAS);
    }
    
    /**
     * Get all cheese trays from a specific container using Container object
     * 
     * @param gui The game UI
     * @param container The container object
     * @return List of cheese trays in the container
     */
    public static ArrayList<WItem> getCheeseTraysFromContainer(NGameUI gui, Container container) throws InterruptedException {
        return gui.getInventory(container.cap).getItems(CheeseConstants.CHEESE_TRAY_ALIAS);
    }
    
    /**
     * Check if inventory has space for cheese trays
     * 
     * @param gui The game UI
     * @return Number of cheese trays that can fit in inventory
     */
    public static int getAvailableCheeseTraySlotsInInventory(NGameUI gui) throws InterruptedException {
        return gui.getInventory().getNumberFreeCoord(CheeseConstants.CHEESE_TRAY_SIZE);
    }
    
    /**
     * Check if inventory has space for slicing operations
     * Slicing requires space for tray + up to 5 cheese pieces (7 single slots total)
     * 
     * @param gui The game UI
     * @return true if inventory has enough space for slicing
     */
    public static boolean hasSpaceForSlicing(NGameUI gui) throws InterruptedException {
        int availableSlots = gui.getInventory().getNumberFreeCoord(CheeseConstants.SINGLE_SLOT_SIZE);
        return availableSlots >= CheeseConstants.SLICING_INVENTORY_REQUIREMENT;
    }
}