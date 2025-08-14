package nurgling.actions;

import haven.WItem;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.tasks.ISRemoved;
import nurgling.tools.Container;
import nurgling.tools.Finder;

import java.util.List;

/**
 * Transfer specific WItem objects to a container (not by name matching)
 */
public class TransferWItemsToContainer implements Action {
    private final Container container;
    private final List<WItem> itemsToTransfer;
    
    public TransferWItemsToContainer(Container container, List<WItem> itemsToTransfer) {
        this.container = container;
        this.itemsToTransfer = itemsToTransfer;
    }
    
    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        if (itemsToTransfer.isEmpty()) {
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
        
        // Transfer each specific item
        for (WItem item : itemsToTransfer) {
                // Check if there's space in the container
                if (container.getattr(Container.Space.class) != null &&
                        gui.getInventory(container.cap).getNumberFreeCoord(item) == 0) {
                    break;
                }
                
                // Transfer this specific item
                item.item.wdgmsg("transfer", haven.Coord.z);
                NUtils.addTask(new ISRemoved(item.item.wdgid()));
                
                // Update container state
                container.update();
        }
        
        // Close container
        new CloseTargetContainer(container).run(gui);

        return Results.SUCCESS();
    }
}