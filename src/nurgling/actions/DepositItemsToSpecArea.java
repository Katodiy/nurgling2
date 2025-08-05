package nurgling.actions;

import haven.Gob;
import nurgling.NGameUI;
import nurgling.areas.NArea;
import nurgling.areas.NContext;
import nurgling.tools.Container;
import nurgling.tools.Context;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.widgets.Specialisation;

import java.util.ArrayList;
import java.util.List;

public class DepositItemsToSpecArea implements Action {
    private final NContext context;
    private final String item; // item name (e.g. "Mulberry Leaf")
    private final Specialisation.SpecName specArea;
    private final int maxPerContainer; // e.g. 32

    public DepositItemsToSpecArea(NContext context, String item, Specialisation.SpecName specArea, int maxPerContainer) {
        this.context = context;
        this.item = item;
        this.specArea = specArea;
        this.maxPerContainer = maxPerContainer;
    }

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        NAlias itemAlias = new NAlias(item);

        // Get the destination area
        NArea area = context.getSpecArea(specArea);
        if (area == null) return Results.ERROR("Destination spec area not found!");

        // Get all containers in this area (cupboards, troughs, etc)
        ArrayList<Gob> gobs = Finder.findGobs(area, new NAlias(new ArrayList<>(Context.contcaps.keySet())));
        ArrayList<Container> containers = new ArrayList<>();
        for (Gob gob : gobs) {
            Container c = new Container(gob, Context.contcaps.get(gob.ngob.name));
            c.initattr(Container.Space.class);
            containers.add(c);
        }
        if (containers.isEmpty()) return Results.ERROR("No containers in target area!");

        // Main processing loop - process each container individually
        while (true) {
            // Step 1: Check all containers to find those that need items
            List<ContainerNeed> containerNeeds = new ArrayList<>();
            
            for (Container container : containers) {
                new PathFinder(Finder.findGob(container.gobid)).run(gui);
                new OpenTargetContainer(container).run(gui);
                
                int currentCount = gui.getInventory(container.cap).getItems(itemAlias).size();
                int needed = Math.max(0, maxPerContainer - currentCount);
                containerNeeds.add(new ContainerNeed(container, needed, currentCount));
                
                new CloseTargetContainer(container).run(gui);
            }
            
            // Step 2: Process each container that needs items
            boolean anyContainerProcessed = false;
            
            for (ContainerNeed containerNeed : containerNeeds) {
                if (containerNeed.needed > 0) {
                    // Fetch exactly what this container needs
                    context.addInItem(item, null);
                    new TakeItems2(context, item, containerNeed.needed).run(gui);
                    
                    int itemsInInventory = gui.getInventory().getItems(itemAlias).size();
                    if (itemsInInventory == 0) break; // No more items available from source
                    
                    // Transfer all items from inventory to this container
                    new PathFinder(Finder.findGob(containerNeed.container.gobid)).run(gui);
                    new OpenTargetContainer(containerNeed.container).run(gui);
                    
                    new TransferToContainer(containerNeed.container, itemAlias).run(gui);
                    
                    new CloseTargetContainer(containerNeed.container).run(gui);
                    
                    anyContainerProcessed = true;
                }
            }
            
            // If no containers were processed, we're done
            if (!anyContainerProcessed) break;
        }

        return Results.SUCCESS();
    }
    
    private static class ContainerNeed {
        final Container container;
        final int needed;
        final int current;
        
        ContainerNeed(Container container, int needed, int current) {
            this.container = container;
            this.needed = needed;
            this.current = current;
        }
    }
}
