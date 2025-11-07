package nurgling.actions.bots;

import haven.*;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.NGItem;
import nurgling.NISBox;
import nurgling.actions.Action;
import nurgling.actions.PathFinder;
import nurgling.actions.Results;
import nurgling.actions.TakeItemsFromContainer;
import nurgling.actions.TakeItemsFromPile;
import nurgling.actions.OpenTargetContainer;
import nurgling.actions.CloseTargetContainer;
import nurgling.areas.NArea;
import nurgling.areas.NContext;
import nurgling.tools.Container;
import nurgling.tools.Context;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.tools.StockpileUtils;
import nurgling.tasks.FilledTrough;
import nurgling.widgets.Specialisation;

import java.util.*;
import java.util.List;

/**
 * Collects swill-compatible items from containers in a selected area
 * and delivers them to appropriate feeding destinations.
 * Based on the FreeContainersInArea pattern with specialized swill handling.
 */
public class CollectSwillInArea implements Action {

    private final boolean includeLowValue;
    private final boolean includeSeeds;

    /**
     * Create swill collection action with default settings.
     */
    public CollectSwillInArea() {
        this(false, false);
    }

    /**
     * Create swill collection action with specific configuration.
     *
     * @param includeLowValue Include low-value items (leaves, wildflowers)
     * @param includeSeeds Include crop seeds
     */
    public CollectSwillInArea(boolean includeLowValue, boolean includeSeeds) {
        this.includeLowValue = includeLowValue;
        this.includeSeeds = includeSeeds;
    }

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        gui.msg("Starting swill collection...");

        try {
            // Initialize NContext for proper area management
            NContext context = new NContext(gui);

            // Let user select area using NContext.createArea()
            gui.msg("Please, select area for swill collection");
            String collectionAreaId = context.createArea("Please, select area for swill collection",
                Resource.loadsimg("baubles/inputArea"));

            if (collectionAreaId == null) {
                return Results.ERROR("No area selected");
            }

            gui.msg("Area registered in NContext with ID: " + collectionAreaId);

            // Get delivery areas first (we'll need them for the cycle)
            List<NArea> deliveryAreas = NContext.findSwillDeliveryAreas();
            if (deliveryAreas.isEmpty()) {
                return Results.ERROR("No swill or trough delivery areas found in NContext");
            }

            int totalItemsProcessed = 0;
            boolean hasMoreItems;
            int cycleCount = 0;
            int maxCycles = 20; // Safety limit to prevent infinite loops

            gui.msg("Starting collection/delivery cycle...");

            // Main collection/delivery cycle
            while (cycleCount < maxCycles) {
                cycleCount++;
                gui.msg("=== Starting new collection cycle " + cycleCount + " ===");

                // Force navigation to collection area by checking distance first
                Pair<Coord2d, Coord2d> area = context.getRCArea(collectionAreaId);
                if (area == null) {
                    gui.msg("Could not get collection area coordinates - ending cycle");
                    break;
                }

                // Check if we're actually close to the collection area
                Coord2d areaCenter = new Coord2d((area.a.x + area.b.x) / 2, (area.a.y + area.b.y) / 2);
                double distanceToArea = areaCenter.dist(NUtils.player().rc);

                if (distanceToArea > 500) {
                    gui.msg("Too far from collection area (" + (int)distanceToArea + " units), forcing route navigation...");

                    // Create a new NContext and force re-navigation
                    try {
                        NContext freshContext = new NContext(gui);
                        // Try to get the area again, which should force navigation
                        Pair<Coord2d, Coord2d> navigatedArea = freshContext.getRCArea(collectionAreaId);
                        if (navigatedArea != null) {
                            area = navigatedArea; // Update with fresh coordinates
                            gui.msg("Successfully navigated to collection area via route");
                        } else {
                            gui.msg("Route navigation failed");
                        }
                    } catch (Exception e) {
                        gui.msg("Navigation error: " + e.getMessage());
                    }
                } else {
                    gui.msg("Already near collection area (" + (int)distanceToArea + " units)");
                }

                // Check if there are still items to collect in this area (now that we're here)
                hasMoreItems = hasMoreSwillItemsInArea(area);
                if (!hasMoreItems) {
                    gui.msg("No more swill items found in collection area - stopping");
                    break;
                }

                // Phase 1: Collect from containers until inventory full
                int cycleItems = collectFromContainers(gui, area);
                totalItemsProcessed += cycleItems;

                // Phase 2: Collect from stockpiles until inventory full
                cycleItems += collectFromStockpilesUntilFull(gui, area);
                totalItemsProcessed += cycleItems;

                // Check if we have any swill items to deliver
                ArrayList<WItem> swillItems = getSwillItemsFromInventory(gui);
                if (swillItems.isEmpty()) {
                    gui.msg("No more swill items found in area");
                    break;
                }

                gui.msg("Collected " + swillItems.size() + " swill items this cycle");

                // Phase 3: Deliver all swill items to trough areas
                Results deliveryResult = deliverAllSwillItems(gui, deliveryAreas, swillItems);
                if (!deliveryResult.IsSuccess()) {
                    gui.msg("Warning: Some items could not be delivered");
                }

                gui.msg("Delivery completed");
                gui.msg("=== Cycle " + cycleCount + " completed ===");

                // Will check for more items at the beginning of next cycle after navigating back
            }

            if (cycleCount >= maxCycles) {
                gui.msg("Reached maximum cycle limit (" + maxCycles + ") - stopping to prevent infinite loop");
            }

            gui.msg("Swill collection completed: " + totalItemsProcessed + " total items processed in " + cycleCount + " cycles");
            return Results.SUCCESS();

        } catch (InterruptedException e) {
            throw e;
        } catch (Exception e) {
            return Results.ERROR("Swill collection failed: " + e.getMessage());
        }
    }

    /**
     * Collect swill items from an open container with enhanced filtering.
     */
    private int collectSwillFromContainer(NGameUI gui, Container container) throws InterruptedException {
        int itemsCollected = 0;
        HashSet<String> swillTargets = new HashSet<>();
        int totalItems = 0;
        int swillItemsFound = 0;

        // Get container inventory
        if (gui.getInventory(container.cap) != null) {
            // Enhanced item filtering with detailed logging
            for (WItem item : gui.getInventory(container.cap).getItems()) {
                totalItems++;
                String itemName = ((NGItem)item.item).name();

                if (itemName != null) {
                    if (isSwillItem(itemName)) {
                        swillTargets.add(itemName);
                        swillItemsFound++;
                        gui.msg("Found swill item: " + itemName);
                    } else {
                        // Debug: Log non-swill items for verification
                        gui.msg("Skipping non-swill item: " + itemName);
                    }
                } else {
                    gui.msg("Warning: Found item with null name");
                }
            }

            gui.msg("Container analysis: " + totalItems + " total items, " + swillItemsFound + " swill items");

            if (!swillTargets.isEmpty()) {
                // Check inventory space before taking items
                int freeSpace = gui.getInventory().getFreeSpace();
                gui.msg("Player inventory has " + freeSpace + " free slots");

                if (freeSpace < swillTargets.size()) {
                    gui.msg("Warning: Limited inventory space. May not collect all items.");
                }

                // Take swill items from container (following FreeContainers pattern)
                Results takeResult = new TakeItemsFromContainer(container, swillTargets, null).run(gui);
                if (takeResult.IsSuccess()) {
                    itemsCollected = swillTargets.size();
                    gui.msg("Successfully took " + itemsCollected + " types of swill items");
                } else {
                    gui.msg("Failed to take swill items from container");
                }
            } else {
                gui.msg("No swill items found in container (checked " + totalItems + " items)");
            }
        } else {
            gui.msg("Warning: Could not access container inventory");
        }

        return itemsCollected;
    }


    /**
     * Check if an item name matches swill criteria.
     */
    private boolean isSwillItem(String itemName) {
        return SwillItemRegistry.isSwillItem(itemName, includeLowValue, includeSeeds);
    }

    /**
     * Deliver swill items to a specific feeding container (trough/cistern) using proper transfer pattern.
     * Assumes we are already at the container location.
     */
    private Results deliverToContainer(NGameUI gui, Gob container, ArrayList<WItem> swillItems) throws InterruptedException {
        // Use exact TransferToTrough pattern: while loop with item refresh
            int itemsDelivered = 0;

            // Build dynamic NAlias based on actual swill items in inventory
            HashSet<String> swillItemNames = new HashSet<>();
            for (WItem item : swillItems) {
                String itemName = ((NGItem)item.item).name();
                if (itemName != null && isSwillItem(itemName)) {
                    swillItemNames.add(itemName);
                }
            }

            if (swillItemNames.isEmpty()) {
                gui.msg("No swill items in inventory to deliver");
                return Results.FAIL();
            }

            gui.msg("Creating alias for items: " + swillItemNames);
            NAlias swillAlias = new NAlias(swillItemNames.toArray(new String[0]));

            ArrayList<WItem> witems;
            // Follow TransferToTrough pattern exactly - while loop with refreshed item list
            while (!(witems = gui.getInventory().getItems(swillAlias)).isEmpty()) {
                try {
                    WItem item = witems.get(0); // Take first item like TransferToTrough
                    String itemName = ((NGItem)item.item).name();
                    gui.msg("Delivering " + itemName + " to " + container.ngob.name);

                    // Take item to hand (following TransferToTrough pattern exactly)
                    NUtils.takeItemToHand(item);

                    // Drop item into container (following TransferToTrough pattern exactly)
                    NUtils.dropsame(container);

                    // Use FilledTrough task like TransferToTrough (critical for proper timing)
                    NUtils.getUI().core.addTask(new FilledTrough(container, swillAlias));

                    itemsDelivered++;
                    gui.msg("Successfully delivered " + itemName);

                    // Don't need manual sleep - FilledTrough task handles timing

                } catch (Exception e) {
                    gui.msg("Failed to deliver item: " + e.getMessage());
                    break; // Stop trying if something goes wrong
                }
            }

            // Remove all delivered items from the tracking list by filtering
            swillItems.removeIf(item -> {
                String itemName = ((NGItem)item.item).name();
                return itemName != null && isSwillItem(itemName);
            });

            if (itemsDelivered > 0) {
                gui.msg("Successfully delivered " + itemsDelivered + " items to " + container.ngob.name);
                return Results.SUCCESS();
            } else {
                gui.msg("No items could be delivered to " + container.ngob.name);
                return Results.FAIL();
            }
    }

    /**
     * Collect swill items from containers in the area.
     */
    private int collectFromContainers(NGameUI gui, Pair<Coord2d, Coord2d> area) throws InterruptedException {
        int itemsCollected = 0;

        ArrayList<Container> containers = new ArrayList<>();
        for (Gob sm : Finder.findGobs(area, new NAlias(new ArrayList<>(NContext.contcaps.keySet())))) {
            Container cand = new Container(sm, NContext.contcaps.get(sm.ngob.name));
            containers.add(cand);
        }

        gui.msg("Found " + containers.size() + " containers");

        for (Container container : containers) {
            try {
                // Navigate to container
                Gob containerGob = Finder.findGob(container.gobid);
                Results pathResult = new PathFinder(containerGob).run(gui);
                if (!pathResult.IsSuccess()) {
                    gui.msg("Warning: Cannot reach container " + container.gobid);
                    continue;
                }

                // Open container
                Results openResult = new OpenTargetContainer(container).run(gui);
                if (!openResult.IsSuccess()) {
                    gui.msg("Warning: Cannot open container " + container.gobid);
                    continue;
                }

                // Check for swill items and take them
                int containerItems = collectSwillFromContainer(gui, container);
                itemsCollected += containerItems;

                // Close container
                new CloseTargetContainer(container).run(gui);

                if (containerItems > 0) {
                    gui.msg("Collected " + containerItems + " swill items from container");
                }

                // Check if inventory is getting full
                if (gui.getInventory().getFreeSpace() < 5) {
                    gui.msg("Inventory getting full, stopping container collection");
                    break;
                }

                Thread.sleep(100); // Small delay between containers

            } catch (InterruptedException e) {
                throw e;
            } catch (Exception e) {
                gui.msg("Error processing container " + container.gobid + ": " + e.getMessage());
            }
        }

        return itemsCollected;
    }

    /**
     * Collect swill items from stockpiles until inventory is full or no more stockpiles.
     */
    private int collectFromStockpilesUntilFull(NGameUI gui, Pair<Coord2d, Coord2d> area) throws InterruptedException {
        int totalSwillItems = 0;

        while (gui.getInventory().getFreeSpace() > 0) {
            ArrayList<Gob> stockpiles = Finder.findGobs(area, new NAlias("stockpile"));
            if (stockpiles.isEmpty()) {
                gui.msg("No more stockpiles in area");
                break;
            }

            boolean collectedFromAny = false;

            for (Gob pile : stockpiles) {
                if (gui.getInventory().getFreeSpace() == 0) {
                    gui.msg("Inventory full, stopping stockpile collection");
                    return totalSwillItems;
                }

                try {
                    if (!PathFinder.isAvailable(pile)) {
                        continue;
                    }

                    gui.msg("Processing stockpile " + pile.id);

                    // Navigate to stockpile
                    Results pathResult = new PathFinder(pile).run(gui);
                    if (!pathResult.IsSuccess()) {
                        continue;
                    }

                    // Open stockpile
                    Results openResult = new OpenTargetContainer("Stockpile", pile).run(gui);
                    if (!openResult.IsSuccess()) {
                        continue;
                    }

                    // Get stockpile interface
                    NISBox spbox = gui.getStockpile();
                    if (spbox == null) {
                        continue;
                    }

                    // Take items until stockpile is empty or inventory is full
                    while (spbox.calcCount() > 0 && gui.getInventory().getFreeSpace() > 0) {
                        Coord size = StockpileUtils.itemMaxSize.get(pile.ngob.name);
                        if (size == null) size = new Coord(1, 1);

                        int freeSpace = gui.getInventory().getNumberFreeCoord(size);
                        if (freeSpace == 0) break;

                        int target_size = Math.min(spbox.calcCount(), freeSpace);
                        if (target_size > 0) {
                            TakeItemsFromPile tifp = new TakeItemsFromPile(pile, spbox, target_size);
                            Results takeResult = tifp.run(gui);

                            if (takeResult.IsSuccess()) {
                                // Count swill items from what we took
                                for (NGItem item : tifp.newItems()) {
                                    if (item.name() != null && isSwillItem(item.name())) {
                                        totalSwillItems++;
                                        gui.msg("Collected swill item from stockpile: " + item.name());
                                    }
                                }
                                collectedFromAny = true;
                            } else {
                                break; // Failed to take items, move to next pile
                            }
                        } else {
                            break; // No space or items
                        }
                    }

                    if (spbox.calcCount() == 0) {
                        gui.msg("Stockpile " + pile.id + " fully collected");
                    }

                    Thread.sleep(200); // Small delay between stockpiles

                } catch (InterruptedException e) {
                    throw e;
                } catch (Exception e) {
                    gui.msg("Error processing stockpile " + pile.id + ": " + e.getMessage());
                }
            }

            if (!collectedFromAny) {
                break; // No more items could be collected
            }
        }

        return totalSwillItems;
    }

    /**
     * Get all swill items currently in player inventory.
     */
    private ArrayList<WItem> getSwillItemsFromInventory(NGameUI gui) throws InterruptedException {
        ArrayList<WItem> swillItems = new ArrayList<>();
        for (WItem item : gui.getInventory().getItems()) {
            String itemName = ((NGItem)item.item).name();
            if (itemName != null && isSwillItem(itemName)) {
                swillItems.add(item);
            }
        }
        return swillItems;
    }

    /**
     * Deliver all swill items to trough areas, ensuring complete delivery.
     * Uses proper NContext navigation for potentially distant areas.
     */
    private Results deliverAllSwillItems(NGameUI gui, List<NArea> deliveryAreas, ArrayList<WItem> swillItems) throws InterruptedException {
        NContext context = new NContext(gui);

        while (!swillItems.isEmpty()) {
            int itemsBefore = swillItems.size();

            for (NArea area : deliveryAreas) {
                if (swillItems.isEmpty()) break;

                try {
                    gui.msg("Attempting delivery to area: " + area.name);

                    // Navigate to the area using proper NContext routing (handles distant areas)
                    if (area.name.toLowerCase().contains("trough")) {
                        // Navigate to trough specialization area
                        NArea troughArea = context.getSpecArea(Specialisation.SpecName.trough);
                        if (troughArea == null) {
                            gui.msg("Could not find or navigate to trough area");
                            continue;
                        }
                    } else if (area.name.toLowerCase().contains("swill")) {
                        // Navigate to swill specialization area (if it gets re-enabled later)
                        gui.msg("Swill area navigation - assuming trough for now");
                        NArea troughArea = context.getSpecArea(Specialisation.SpecName.trough);
                        if (troughArea == null) {
                            gui.msg("Could not find or navigate to trough area");
                            continue;
                        }
                    }

                    gui.msg("Successfully navigated to delivery area: " + area.name);

                    // Now look for troughs and cisterns in the area (after proper navigation)
                    Pair<Coord2d, Coord2d> areaCoords = area.getRCArea();
                    if (areaCoords == null) {
                        gui.msg("Could not get area coordinates after navigation");
                        continue;
                    }

                    ArrayList<Gob> feedingContainers = Finder.findGobs(areaCoords,
                        new NAlias("Trough", "Cistern", "Stone Cistern"));

                    if (feedingContainers.isEmpty()) {
                        gui.msg("No feeding containers found in area after navigation");
                        continue;
                    }

                    gui.msg("Found " + feedingContainers.size() + " feeding containers in area");

                    // Try to feed each container (now we're already in the right area)
                    for (Gob container : feedingContainers) {
                        if (swillItems.isEmpty()) break;

                        try {
                            gui.msg("Navigating to " + container.ngob.name + " at " + container.rc);

                            // Short-distance navigation to the container (within area)
                            Results pathResult = new PathFinder(container).run(gui);
                            if (!pathResult.IsSuccess()) {
                                gui.msg("Could not reach " + container.ngob.name + " within area");
                                continue;
                            }

                            // Deliver items to this container
                            Results feedResult = deliverToContainer(gui, container, swillItems);
                            if (feedResult.IsSuccess()) {
                                gui.msg("Successfully delivered items to " + container.ngob.name);
                            }
                        } catch (Exception e) {
                            gui.msg("Error feeding container: " + e.getMessage());
                        }
                    }

                } catch (Exception e) {
                    gui.msg("Error processing delivery area " + area.name + ": " + e.getMessage());
                }
            }

            // Check if we made progress
            if (swillItems.size() == itemsBefore) {
                gui.msg("Could not deliver any more items - stopping delivery");
                break;
            }
        }

        return swillItems.isEmpty() ? Results.SUCCESS() : Results.FAIL();
    }

    /**
     * Check if there are more swill items to collect in the area.
     */
    private boolean hasMoreSwillItemsInArea(Pair<Coord2d, Coord2d> area) {
        try {
            // Check containers
            ArrayList<Container> containers = new ArrayList<>();
            for (Gob sm : Finder.findGobs(area, new NAlias(new ArrayList<>(NContext.contcaps.keySet())))) {
                Container cand = new Container(sm, NContext.contcaps.get(sm.ngob.name));
                containers.add(cand);
            }

            // Quick check for stockpiles
            ArrayList<Gob> stockpiles = Finder.findGobs(area, new NAlias("stockpile"));

            return !containers.isEmpty() || !stockpiles.isEmpty();
        } catch (Exception e) {
            return false; // Assume no more items if we can't check
        }
    }

}