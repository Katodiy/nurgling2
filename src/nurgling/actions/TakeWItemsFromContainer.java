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
            return Results.SUCCESS();
        }
        
        // Navigate to container
        if (container.gobid != -1) {
            new PathFinder(Finder.findGob(container.gobid)).run(gui);
        }
        
        // Open container if needed
        if (container.cap != null) {
            new OpenTargetContainer(container.cap, Finder.findGob(container.gobid)).run(gui);
        }
        
        int taken = 0;
        
        // Take each specific item from container to inventory
        for (WItem item : itemsToTake) {
            try {
                // Check if there's space in player inventory
                if (gui.getInventory().getFreeSpace() <= 0) {
                    gui.msg("Player inventory is full, stopping take operation");
                    break;
                }
                
                // Take this specific item to inventory
                item.item.wdgmsg("take", haven.Coord.z);
                NUtils.addTask(new ISRemoved(item.item.wdgid()));
                taken++;
                
                // Update container state
                container.update();
                
            } catch (Exception e) {
                gui.msg("Error taking item: " + e.getMessage());
                break;
            }
        }
        
        // Close container
        new CloseTargetContainer(container).run(gui);
        
        gui.msg("Took " + taken + " specific items from container to inventory");
        return Results.SUCCESS();
    }
}