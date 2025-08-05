package nurgling.actions;

import haven.WItem;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.tasks.ISRemoved;
import nurgling.tools.Container;
import nurgling.tools.Finder;

import java.util.List;

/**
 * Take specific WItem objects from a container to player inventory
 */
public class TakeWItemsFromContainer implements Action {
    private final Container container;
    private final List<WItem> itemsToTake;
    
    public TakeWItemsFromContainer(Container container, List<WItem> itemsToTake) {
        this.container = container;
        this.itemsToTake = itemsToTake;
    }
    
    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        if (itemsToTake.isEmpty()) {
            gui.msg("No items to take from container");
            return Results.SUCCESS();
        }
        
        gui.msg("Taking " + itemsToTake.size() + " items from container");
        
        // Navigate to container
        if (container.gobid != -1) {
            new PathFinder(Finder.findGob(container.gobid)).run(gui);
        }
        
        // Open container if needed
        if (container.cap != null) {
            gui.msg("Opening container: " + container.cap);
            Results openResult = new OpenTargetContainer(container.cap, Finder.findGob(container.gobid)).run(gui);
            if (!openResult.IsSuccess()) {
                gui.msg("Failed to open container");
                return openResult;
            }
            gui.msg("Container opened successfully");
        }
        
        int taken = 0;
        
        // Take each specific item from container to inventory
        for (WItem item : itemsToTake) {
                // Check if there's space in player inventory
                if (gui.getInventory().getNumberFreeCoord(item) == 0) {
                    gui.msg("Player inventory is full, stopping take operation");
                    break;
                }
                
                gui.msg("Attempting to transfer item: " + item.item.getres().name + " (ID: " + item.item.wdgid() + ")");
                
                try {
                    // Take this specific item to inventory
                    item.item.wdgmsg("transfer", haven.Coord.z);
                    
                    // Wait for the item to be removed from the container (this is the proper way)
                    NUtils.addTask(new ISRemoved(item.item.wdgid()));
                    
                    // If we get here, the ISRemoved task completed, meaning the transfer succeeded
                    taken++;
                    gui.msg("Successfully transferred item " + item.item.getres().name);
                    
                } catch (Exception e) {
                    gui.msg("Transfer failed for item " + item.item.getres().name + ": " + e.getMessage());
                }
                
                // Update container state
                container.update();
        }
        
        // Close container
        new CloseTargetContainer(container).run(gui);
        
        gui.msg("Took " + taken + " specific items from container to inventory");
        
        // Return failure if we were supposed to take items but didn't take any
        if (taken == 0 && !itemsToTake.isEmpty()) {
            return Results.FAIL();
        }
        
        return Results.SUCCESS();
    }
}