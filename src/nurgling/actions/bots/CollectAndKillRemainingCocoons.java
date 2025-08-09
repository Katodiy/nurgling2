package nurgling.actions.bots;

import haven.Gob;
import haven.WItem;
import nurgling.NGameUI;
import nurgling.actions.*;
import nurgling.areas.NArea;
import nurgling.areas.NContext;
import nurgling.tools.Container;
import nurgling.tools.Context;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.widgets.Specialisation;

import java.util.ArrayList;

import static nurgling.areas.NContext.contcaps;

/**
 * Collects all remaining cocoons from silkworm feeding area after the initial
 * batch has been moved to breeding area, then uses FreeInventory2 to store them.
 */
public class CollectAndKillRemainingCocoons implements Action {
    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        NContext context = new NContext(gui);
        String cocoons = "Silkworm Cocoon";
        String moth = "Silkmoth";
        
        // Get silkworm feeding area
        NArea feedingArea = context.getSpecArea(Specialisation.SpecName.silkwormFeeding);
        if (feedingArea == null) {
            return Results.SUCCESS(); // No feeding area, nothing to do
        }
        
        // Get all containers in feeding area
        ArrayList<Gob> feedingGobs = Finder.findGobs(feedingArea, new NAlias(new ArrayList<>(Context.contcaps.keySet())));
        ArrayList<Container> feedingContainers = new ArrayList<>();
        
        for (Gob gob : feedingGobs) {
            Container container = new Container(gob, contcaps.get(gob.ngob.name));
            container.initattr(Container.Space.class);
            feedingContainers.add(container);
        }
        
        if (feedingContainers.isEmpty()) {
            return Results.SUCCESS();
        }
        
        // Collect all remaining cocoons from feeding containers
        NAlias cocoonAlias = new NAlias(cocoons, moth);
        boolean foundCocoons = false;
        
        for (Container container : feedingContainers) {
            new PathFinder(Finder.findGob(container.gobid)).run(gui);
            new OpenTargetContainer(container).run(gui);
            
            // Get all cocoons and moths from this container
            ArrayList<WItem> cocoonsAndMoths = gui.getInventory(container.cap).getItems(cocoonAlias);
            
            if (!cocoonsAndMoths.isEmpty()) {
                foundCocoons = true;
                
                // Take cocoons in batches based on inventory space
                while (!cocoonsAndMoths.isEmpty()) {
                    int inventorySpace = gui.getInventory().getFreeSpace();
                    if (inventorySpace == 0) {
                        // Inventory full - use FreeInventory2 to clear it
                        new CloseTargetContainer(container).run(gui);
                        new KillCocoons().run(gui);
                        new FreeInventory2(context).run(gui);
                        context.getSpecArea(Specialisation.SpecName.silkwormFeeding);
                        new PathFinder(Finder.findGob(container.gobid)).run(gui);
                        new OpenTargetContainer(container).run(gui);
                        // Refresh the item list after inventory was cleared
                        cocoonsAndMoths = gui.getInventory(container.cap).getItems(cocoonAlias);
                        continue;
                    }
                    
                    // Take what fits in inventory
                    int itemsToTake = Math.min(cocoonsAndMoths.size(), inventorySpace);
                    ArrayList<WItem> itemsToTakeBatch = new ArrayList<>();
                    
                    for (int i = 0; i < itemsToTake; i++) {
                        itemsToTakeBatch.add(cocoonsAndMoths.get(i));
                    }
                    
                    new TakeWItemsFromContainer(container, itemsToTakeBatch).run(gui);
                    
                    // Remove taken items from tracking list
                    for (int i = 0; i < itemsToTake; i++) {
                        cocoonsAndMoths.remove(0);
                    }
                }
            }
            
            new CloseTargetContainer(container).run(gui);
        }
        
        // Clear any remaining items in inventory
        if (foundCocoons || gui.getInventory().getItems(cocoonAlias).size() > 0) {
            new KillCocoons().run(gui);
            new FreeInventory2(context).run(gui);
        }
        
        return Results.SUCCESS();
    }
}