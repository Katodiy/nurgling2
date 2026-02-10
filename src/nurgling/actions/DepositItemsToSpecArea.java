package nurgling.actions;

import haven.Gob;
import haven.WItem;
import nurgling.NGameUI;
import nurgling.NInventory;
import nurgling.areas.NArea;
import nurgling.areas.NContext;
import nurgling.tools.Container;
import nurgling.tools.Context;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.widgets.Specialisation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Deposits items to containers in a specialized area.
 * Uses Container.ItemCount updater to track specific item counts per container.
 *
 * Algorithm:
 * 1. Scan all containers, calculate total needed items using ItemCount updater
 * 2. Fetch-Distribute Loop (makes multiple trips until done):
 *    a. Calculate how many items still needed across all containers
 *    b. Fetch what fits in inventory from source
 *    c. Distribute to containers until inventory empty
 *    d. Repeat until all containers full OR source exhausted
 */
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
        gui.msg("DepositItems: Starting. Item=" + itemAlias + ", maxPerContainer=" + maxPerContainer);
        
        // Get the destination area
        NArea area = context.getSpecArea(destinationSpec);
        if (area == null) return Results.ERROR("Destination spec area not found!");

        // Get all containers in this area (cupboards, troughs, etc)
        ArrayList<Gob> gobs = Finder.findGobs(area, new NAlias(new ArrayList<>(Context.contcaps.keySet())));
        ArrayList<Container> containers = new ArrayList<>();
        for (Gob gob : gobs) {
            Container c = new Container(gob, Context.contcaps.get(gob.ngob.name), area);
            // Initialize ItemCount updater with our target item and max count
            c.initItemCount(itemAlias, maxPerContainer);
            // Also initialize Space for free space tracking
            c.initattr(Container.Space.class);
            containers.add(c);
        }
        if (containers.isEmpty()) return Results.ERROR("No containers in target area!");
        
        gui.msg("DepositItems: Found " + containers.size() + " containers in area");

        // Step 1: Scan all containers and calculate total needed items
        int totalNeeded = 0;
        ArrayList<Container> containersNeedingItems = new ArrayList<>();
        int containerIndex = 0;

        for (Container container : containers) {
            containerIndex++;
            new PathFinder(Finder.findGob(container.gobid)).run(gui);
            new OpenTargetContainer(container).run(gui);

            // Update the ItemCount updater (counts specific items)
            Container.ItemCount itemCount = container.getattr(Container.ItemCount.class);
            itemCount.update();
            
            // Also update Space for free space tracking
            Container.Space space = container.getattr(Container.Space.class);
            space.update();

            int currentCount = itemCount.getCurrentCount();
            int needed = itemCount.getNeeded();
            int freeSpace = space.getFreeSpace();
            
            gui.msg("DepositItems: Container #" + containerIndex + " [gob=" + container.gobid + "]: current=" + currentCount + ", target=" + maxPerContainer + ", needed=" + needed + ", freeSpace=" + freeSpace);
            
            // Store free space for external access
            containerFreeSpaceMap.put(container.gobid, freeSpace);

            // Only add if we need items AND have space
            if (needed > 0 && freeSpace > 0) {
                // Limit by available free space
                int canAdd = Math.min(needed, freeSpace);
                totalNeeded += canAdd;
                containersNeedingItems.add(container);
                gui.msg("DepositItems: Container #" + containerIndex + " NEEDS " + canAdd + " items (added to fill list)");
            } else {
                gui.msg("DepositItems: Container #" + containerIndex + " SKIPPED (needed=" + needed + ", freeSpace=" + freeSpace + ")");
            }

            new CloseTargetContainer(container).run(gui);
        }

        gui.msg("DepositItems: TOTAL NEEDED = " + totalNeeded + ", containers to fill = " + containersNeedingItems.size());

        if (totalNeeded == 0) {
            gui.msg("DepositItems: All containers are full, nothing to do");
            return Results.SUCCESS(); // All containers are already filled
        }

        // Step 2: Register items in context
        for (String key : this.itemAlias.getKeys()) {
            context.addInItem(key, null);
        }

        // Step 3-4: Fetch-Distribute Loop (multiple trips until done or source empty)
        int tripNumber = 0;

        while (!containersNeedingItems.isEmpty()) {
            tripNumber++;

            // Calculate how many items still needed across all containers
            int totalStillNeeded = 0;
            for (Container container : containersNeedingItems) {
                Container.ItemCount itemCount = container.getattr(Container.ItemCount.class);
                Container.Space space = container.getattr(Container.Space.class);
                int needed = itemCount.getNeeded();
                int freeSpace = space.getFreeSpace();
                totalStillNeeded += Math.min(needed, freeSpace);
            }

            if (totalStillNeeded == 0) {
                gui.msg("DepositItems: All containers are now full");
                break;
            }

            gui.msg("DepositItems: Trip #" + tripNumber + " - fetching up to " + totalStillNeeded + " items from source...");

            // Fetch items from source
            for (String key : this.itemAlias.getKeys()) {
                int currentInInventory = gui.getInventory().getItems(itemAlias).size();
                int stillNeeded = totalStillNeeded - currentInInventory;
                if (stillNeeded <= 0) break;

                gui.msg("DepositItems: Taking " + stillNeeded + " of '" + key + "'");

                if (this.originSpec != null) {
                    new TakeItems2(context, key, stillNeeded, originSpec, NInventory.QualityType.High).run(gui);
                } else {
                    new TakeItems2(context, key, stillNeeded, NInventory.QualityType.High).run(gui);
                }
            }

            int itemsFetched = gui.getInventory().getItems(itemAlias).size();
            gui.msg("DepositItems: Trip #" + tripNumber + " - fetched " + itemsFetched + " items");

            if (itemsFetched == 0) {
                gui.msg("DepositItems: No items available from source, stopping");
                break;
            }

            // Distribute items to containers
            gui.msg("DepositItems: Trip #" + tripNumber + " - distributing to " + containersNeedingItems.size() + " containers...");
            ArrayList<Container> stillNeedingItems = new ArrayList<>();
            int fillIndex = 0;

            for (Container container : containersNeedingItems) {
                fillIndex++;
                ArrayList<WItem> itemsInInventory = gui.getInventory().getItems(itemAlias);

                if (itemsInInventory.isEmpty()) {
                    // No more items in inventory, but this container still needs - add to next trip
                    stillNeedingItems.add(container);
                    gui.msg("DepositItems: Container #" + fillIndex + " [gob=" + container.gobid + "] - no items left, will try next trip");
                    continue;
                }

                gui.msg("DepositItems: Fill container #" + fillIndex + " [gob=" + container.gobid + "], items in inventory=" + itemsInInventory.size());

                // Refresh the area context
                context.getSpecArea(destinationSpec);

                // Transfer items to this container
                // TransferToContainer will open container, call ItemCount.update(), and limit transfer to getNeeded()
                new TransferToContainer(container, itemAlias).run(gui);

                // Update container info after transfer (container should still be open from TransferToContainer)
                Container.ItemCount itemCount = container.getattr(Container.ItemCount.class);
                Container.Space space = container.getattr(Container.Space.class);
                if (space.isReady()) {
                    space.update();
                    containerFreeSpaceMap.put(container.gobid, space.getFreeSpace());
                }

                // ItemCount was updated by TransferToContainer, get current values
                int afterCount = itemCount.getCurrentCount();
                int afterNeeded = itemCount.getNeeded();
                int afterFreeSpace = space.getFreeSpace();
                gui.msg("DepositItems: Container #" + fillIndex + " after transfer: count=" + afterCount + ", stillNeeded=" + afterNeeded + ", freeSpace=" + afterFreeSpace);

                // Check if container still needs more items
                boolean isFull = itemCount.isFull() || afterFreeSpace == 0;
                if (isFull) {
                    gui.msg("DepositItems: Container #" + fillIndex + " is now FULL");
                } else if (afterNeeded > 0) {
                    // Container still needs items - add to next trip
                    stillNeedingItems.add(container);
                    gui.msg("DepositItems: Container #" + fillIndex + " still needs " + afterNeeded + " items, will try next trip");
                }

                new CloseTargetContainer(container).run(gui);
            }

            // Update list for next iteration
            containersNeedingItems = stillNeedingItems;

            gui.msg("DepositItems: Trip #" + tripNumber + " complete. Containers still needing items: " + containersNeedingItems.size());
        }

        int remainingItems = gui.getInventory().getItems(itemAlias).size();
        gui.msg("DepositItems: DONE after " + tripNumber + " trip(s). Remaining items in inventory: " + remainingItems);

        return Results.SUCCESS();
    }

    /**
     * Getter method to access container free space mapping.
     * Used by SilkProductionBot to calculate how many silkworms can fit.
     */
    public Map<Long, Integer> getContainerFreeSpaceMap() {
        return containerFreeSpaceMap;
    }
}
