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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DepositItemsToSpecArea implements Action {
    private final NContext context;
    private final NAlias itemAlias;
    private final Specialisation.SpecName destinationSpec;
    private final int maxPerContainer;
    private Specialisation.SpecName originSpec = null;

    private Map<Long, Integer> containerFreeSpaceMap = new HashMap<>();

    public DepositItemsToSpecArea(NContext context, NAlias itemAlias, Specialisation.SpecName destinationSpec, int maxPerContainer) {
        this.context = context;
        this.itemAlias = itemAlias;
        this.destinationSpec = destinationSpec;
        this.maxPerContainer = maxPerContainer;
    }

    public DepositItemsToSpecArea(NContext context, NAlias itemAlias, Specialisation.SpecName destinationSpec, Specialisation.SpecName originSpec, int maxPerContainer) {
        this.context = context;
        this.itemAlias = itemAlias;
        this.destinationSpec = destinationSpec;
        this.maxPerContainer = maxPerContainer;
        this.originSpec = originSpec;
    }

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        // Get the destination area
        NArea area = context.getSpecArea(destinationSpec);
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

                containerFreeSpaceMap.put(container.gobid, gui.getInventory(container.cap).getFreeSpace());
                
                new CloseTargetContainer(container).run(gui);
            }
            
            // Step 2: Process each container that needs items
            context.addInItem(this.itemAlias.getDefault(), null);

            boolean noMoreItemsAtSource = false;
            for (ContainerNeed containerNeed : containerNeeds) {
                if(noMoreItemsAtSource) {
                    break;
                }

                while (containerNeed.needed > 0) {
                    containerNeed.container.initattr(Container.Space.class);

                    // Fetch exactly what this container needs
                    if(this.originSpec != null) {
                        new TakeItems2(context, this.itemAlias.getDefault(), containerNeed.needed, originSpec).run(gui);
                    } else {
                        new TakeItems2(context, this.itemAlias.getDefault(), containerNeed.needed).run(gui);
                    }

                    context.getSpecArea(destinationSpec);
                    
                    int itemsInInventory = gui.getInventory().getItems(itemAlias).size();
                    if (itemsInInventory == 0) {
                        noMoreItemsAtSource = true;
                        break; // No more items available from source
                    }
                    
                    new TransferToContainer(containerNeed.container, itemAlias).run(gui);

                    if(gui.getInventory(containerNeed.container.cap).getFreeSpace() == 0) {
                        break;
                    }
                    
                    new CloseTargetContainer(containerNeed.container).run(gui);

                    containerNeed.setNeeded(containerNeed.needed - itemsInInventory);
                }
            }

            break;
        }

        return Results.SUCCESS();
    }
    
    // Getter method to access container free space mapping
    public Map<Long, Integer> getContainerFreeSpaceMap() {
        return containerFreeSpaceMap;
    }
    
    private static class ContainerNeed {
        final Container container;
        int needed;
        final int current;
        
        ContainerNeed(Container container, int needed, int current) {
            this.container = container;
            this.needed = needed;
            this.current = current;
        }

        void setNeeded(int needed) {
            this.needed = needed;
        }
    }
}
