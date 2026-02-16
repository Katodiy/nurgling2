package nurgling.actions;

import haven.Gob;
import haven.WItem;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.areas.NArea;
import nurgling.areas.NContext;
import nurgling.cheese.CheeseBranch;
import nurgling.actions.bots.cheese.CheeseUtils;
import nurgling.actions.bots.cheese.CheeseSlicingManager;
import nurgling.actions.bots.cheese.CheeseRackOverlayUtils;
import nurgling.actions.bots.cheese.CheeseConstants;
import nurgling.actions.bots.cheese.CheeseAreaManager;
import nurgling.actions.bots.cheese.CheeseInventoryOperations;
import nurgling.tasks.ISRemoved;
import nurgling.tasks.WaitForGobWithHash;
import nurgling.tasks.WaitMoreItems;
import nurgling.tools.Container;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.cheese.CheeseOrder;
import nurgling.cheese.CheeseOrdersManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Processes cheese from all buffer containers across all areas
 * Handles slicing ready cheese and moving cheese to next aging stage
 * Use getTraysMovedToAreas() to get capacity impact after running
 */
public class ProcessCheeseFromBufferContainers implements Action {
    private final CheeseSlicingManager slicingManager;
    private final Map<CheeseBranch.Place, Integer> traysMovedToAreas = new HashMap<>();
    private final CheeseOrdersManager ordersManager;
    private final Map<CheeseBranch.Place, Integer> recordedRackCapacity;
    private final Map<CheeseBranch.Place, Boolean> bufferEmptinessMap;
    private boolean ordersNeedSaving = false;

    public ProcessCheeseFromBufferContainers(CheeseOrdersManager ordersManager, Map<CheeseBranch.Place, Integer> rackCapacity, Map<CheeseBranch.Place, Boolean> bufferEmptinessMap) {
        this.slicingManager = new CheeseSlicingManager();
        this.ordersManager = ordersManager;
        this.recordedRackCapacity = rackCapacity != null ? rackCapacity : new HashMap<>();
        this.bufferEmptinessMap = bufferEmptinessMap != null ? bufferEmptinessMap : new HashMap<>();
    }

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        // Initialize tracking map
        traysMovedToAreas.clear();

        CheeseBranch.Place[] places = {
                CheeseBranch.Place.inside,
                CheeseBranch.Place.cellar,
                CheeseBranch.Place.outside,
                CheeseBranch.Place.mine
        };

        for (CheeseBranch.Place place : places) {
            // Skip areas that have all empty buffers (optimization from ClearRacksAndRecordCapacity)
            if (bufferEmptinessMap.containsKey(place) && bufferEmptinessMap.get(place)) {
                continue;
            }

            // Get ALL areas for this place type (supports multiple cellars, multiple inside areas, etc.)
            ArrayList<NArea> areasForPlace = CheeseAreaManager.getAllCheeseAreas(place);

            if (areasForPlace.isEmpty()) {
                gui.msg("No cheese area found for " + place);
                continue;
            }

            // Process each area of this place type
            for (NArea area : areasForPlace) {
                processBufferContainersForArea(gui, area, place);
            }
        }

        // Batch save all order updates at the end - much more efficient than writing after every tray
        if (ordersNeedSaving) {
            ordersManager.writeOrders();
            ordersNeedSaving = false;
        }

        return Results.SUCCESS();
    }

    /**
     * Get the number of trays moved to each area during buffer processing
     * This impacts rack capacity - these areas now have fewer available slots
     *
     * @return Map of area to number of trays moved to that area
     */
    public Map<CheeseBranch.Place, Integer> getTraysMovedToAreas() {
        return new HashMap<>(traysMovedToAreas);
    }

    /**
     * Process cheese from buffer containers in a specific area
     * 1. First pass: collect ready-to-slice cheese to inventory
     * 2. Free inventory when full (FreeInventory2)
     * 3. Second pass: move remaining cheese to next aging stage
     *
     * @param gui The game UI
     * @param area The specific area to process
     * @param place The place type (for context and navigation back)
     */
    private void processBufferContainersForArea(NGameUI gui, NArea area, CheeseBranch.Place place) throws InterruptedException {
        // Navigate to the area first
        NContext context = new NContext(gui);
        context.getAreaById(area.id);

        // Find buffer containers in this area
        ArrayList<Gob> containers = Finder.findGobs(area, new NAlias(new ArrayList<>(NContext.contcaps.keySet()), new ArrayList<>()));

        // Phase 1: Collect ready-to-slice cheese
        collectReadyToSliceCheeseFromArea(gui, containers, area, place);

        // Re-navigate to area after potential FreeInventory2 calls
        context = new NContext(gui);
        context.getAreaById(area.id);

        // Phase 2: Move remaining cheese to next stages
        moveRemainingCheeseToNextStageFromArea(gui, containers, area, place);
    }

    /**
     * Phase 1: Collect ready-to-slice cheese from buffer containers and slice them
     *
     * @param gui The game UI
     * @param containers The buffer containers in this area
     * @param area The specific area being processed
     * @param place The place type (for navigation back after FreeInventory2)
     */
    private void collectReadyToSliceCheeseFromArea(NGameUI gui, ArrayList<Gob> containers, NArea area, CheeseBranch.Place place) throws InterruptedException {
        // Use centralized constants for sizes
        NContext freshContext = new NContext(gui);

        for (Gob containerGob : containers) {
            // Skip checking empty containers.
            if ((containerGob.ngob.name.equals("gfx/terobjs/chest") || containerGob.ngob.name.equals("gfx/terobjs/cupboard")) && containerGob.ngob.getModelAttribute() == 2) {
                continue;
            }

            Container bufferContainer = new Container(containerGob, NContext.contcaps.get(containerGob.ngob.name), null);
            PathFinder pf = new PathFinder(containerGob);
            pf.isHardMode = true;
            pf.run(gui);
            new OpenTargetContainer(bufferContainer).run(gui);

            // Process this container completely before moving to next
            while (true) {
                // Get cheese trays from this container (only re-fetch after FreeInventory2)
                ArrayList<WItem> trays = CheeseInventoryOperations.getCheeseTraysFromContainer(gui, bufferContainer);

                // Find first ready-to-slice tray in this container
                WItem readyTray = null;
                for (WItem tray : trays) {
                    if (CheeseUtils.isCheeseReadyToSlice(tray, ordersManager)) {
                        readyTray = tray;
                        break;
                    }
                }

                // If no ready trays found, this container is done
                if (readyTray == null) {
                    new CloseTargetContainer(bufferContainer).run(gui);
                    break; // Move to next container
                }

                // Check if inventory has space for slicing (tray + up to 5 cheese pieces = 7 slots)
                if (!CheeseInventoryOperations.hasSpaceForSlicing(gui)) {
                    new CloseTargetContainer(bufferContainer).run(gui);
                    freshContext = new NContext(gui);
                    new FreeInventory2(freshContext).run(gui);

                    // CRITICAL FIX: After FreeInventory2, we need to re-find the area and containers
                    // because the character could be very far from the original location
                    containerGob = refindContainerAfterFreeInventoryInArea(gui, area, containerGob);
                    if (containerGob == null) {
                        break; // Skip this container and move to next
                    }
                    bufferContainer = new Container(containerGob, NContext.contcaps.get(containerGob.ngob.name), null);
                    PathFinder pf2 = new PathFinder(containerGob);
                    pf2.isHardMode = true;
                    pf2.run(gui);
                    new OpenTargetContainer(bufferContainer).run(gui);
                    continue; // Go back to start of while loop with fresh container references
                }

                // Take the ready tray to inventory using the fresh reference
                readyTray.item.wdgmsg("transfer", haven.Coord.z);
                nurgling.NUtils.addTask(new nurgling.tasks.ISRemoved(readyTray.item.wdgid()));

                // Find the tray we just took and slice it (container stays open)
                ArrayList<WItem> inventoryTrays = CheeseInventoryOperations.getCheeseTrays(gui);
                for (WItem inventoryTray : inventoryTrays) {
                    if (CheeseUtils.isCheeseReadyToSlice(inventoryTray, ordersManager)) {
                        slicingManager.sliceCheese(gui, inventoryTray, ordersManager);
                        break; // Only slice one tray per iteration
                    }
                }

                // Check if inventory is getting full after slicing
                if (!CheeseInventoryOperations.hasSpaceForSlicing(gui)) {
                    new CloseTargetContainer(bufferContainer).run(gui);
                    freshContext = new NContext(gui);
                    new FreeInventory2(freshContext).run(gui);

                    // CRITICAL FIX: After FreeInventory2, re-find the container
                    containerGob = refindContainerAfterFreeInventoryInArea(gui, area, containerGob);
                    if (containerGob == null) {
                        break;
                    }
                    bufferContainer = new Container(containerGob, NContext.contcaps.get(containerGob.ngob.name), null);
                    PathFinder pf3 = new PathFinder(containerGob);
                    pf3.isHardMode = true;
                    pf3.run(gui);
                    new OpenTargetContainer(bufferContainer).run(gui);
                }

                // Continue processing this container (loop back, but don't need to re-fetch trays unless we called FreeInventory2)
            }

            new CloseTargetContainer(bufferContainer).run(gui);
        }
        new FreeInventory2(freshContext).run(gui);
    }

    /**
     * Re-find a container after FreeInventory2 has potentially moved the character far away
     * This is critical because after FreeInventory2, the original gob references may be out of range
     *
     * @param gui         The game UI
     * @param area        The specific area where the container should be
     * @param originalGob The original container gob (used for matching by ID)
     * @return The re-found container gob, or null if not found
     */
    private Gob refindContainerAfterFreeInventoryInArea(NGameUI gui, NArea area, Gob originalGob) throws InterruptedException {
        // Step 1: Navigate back to the specific area
        NContext context = new NContext(gui);
        context.getAreaById(area.id);

        // Step 2: Find containers in the area again
        ArrayList<Gob> containers = Finder.findGobs(area, new NAlias(new ArrayList<>(NContext.contcaps.keySet()), new ArrayList<>()));
        if (containers.isEmpty()) {
            gui.msg("No containers found in area " + area.name + " after FreeInventory2");
            return null;
        }

        // Step 3: Try to find the exact same container using Gob.id
        long originalGobId = originalGob.id;
        for (Gob containerGob : containers) {
            if (containerGob.id == originalGobId) {
                return containerGob;
            }
        }

        // Step 4: If original container not found by ID, return the first available container of same type
        String originalContainerName = originalGob.ngob.name;
        for (Gob containerGob : containers) {
            if (containerGob.ngob.name.equals(originalContainerName)) {
                return containerGob;
            }
        }

        // Step 5: Fallback to first available container
        return containers.get(0);
    }

    /**
     * Re-find all containers in an area after returning from a different area.
     * This refreshes stale Gob references that become invalid when the character moves far away.
     *
     * @param gui         The game UI
     * @param area        The area where containers should be found
     * @param originalContainers The original list of containers (used for matching by gobid)
     * @return Fresh list of container Gobs with valid references
     */
    private ArrayList<Gob> refindContainersInArea(NGameUI gui, NArea area, ArrayList<Gob> originalContainers) throws InterruptedException {
        // Find all containers in the area with fresh references
        ArrayList<Gob> freshContainers = Finder.findGobs(area, new NAlias(new ArrayList<>(NContext.contcaps.keySet()), new ArrayList<>()));

        if (freshContainers.isEmpty()) {
            return new ArrayList<>();
        }

        // Match original containers by gobid to preserve processing order
        ArrayList<Gob> result = new ArrayList<>();
        for (Gob originalGob : originalContainers) {
            long originalId = originalGob.id;
            for (Gob freshGob : freshContainers) {
                if (freshGob.id == originalId) {
                    result.add(freshGob);
                    break;
                }
            }
        }

        return result;
    }

    /**
     * Phase 2: Move remaining cheese to next aging stages
     * Process one cheese type at a time for efficient batching
     *
     * @param gui The game UI
     * @param containers The buffer containers in this area
     * @param area The specific area being processed
     * @param place The place type
     */
    private void moveRemainingCheeseToNextStageFromArea(NGameUI gui, ArrayList<Gob> containers, NArea area, CheeseBranch.Place place) throws InterruptedException {
        // Step 1: Collect all cheese locations and destinations in a single pass
        CheeseCollectionResult collectionResult = collectCheeseLocationsFromContainers(gui, containers, place);

        // Step 2: Process each cheese type efficiently
        processCollectedCheeseByType(gui, collectionResult, area, place);
    }

    /**
     * Collect all cheese locations and destinations from containers in a single pass
     *
     * @return CheeseCollectionResult containing organized cheese data
     */
    private CheeseCollectionResult collectCheeseLocationsFromContainers(NGameUI gui, ArrayList<Gob> containers, CheeseBranch.Place place) throws InterruptedException {
        Map<String, CheeseBranch.Place> cheeseTypeToDestination = new HashMap<>();
        Map<String, ArrayList<CheeseLocation>> cheeseByType = new HashMap<>();

        for (Gob containerGob : containers) {
            // Skip checking empty containers.
            if ((containerGob.ngob.name.equals("gfx/terobjs/chest") || containerGob.ngob.name.equals("gfx/terobjs/cupboard")) && containerGob.ngob.getModelAttribute() == 2) {
                continue;
            }
            Container bufferContainer = new Container(containerGob, NContext.contcaps.get(containerGob.ngob.name), null);
            PathFinder pf = new PathFinder(containerGob);
            pf.isHardMode = true;
            pf.run(gui);
            new OpenTargetContainer(bufferContainer).run(gui);

            ArrayList<WItem> trays = CheeseInventoryOperations.getCheeseTraysFromContainer(gui, bufferContainer);
            for (WItem tray : trays) {
                if (CheeseUtils.shouldMoveToNextStage(tray, place)) {
                    String cheeseType = CheeseUtils.getContentName(tray);
                    CheeseBranch.Place nextStage = getCorrectNextStageLocation(cheeseType, place);

                    if (cheeseType != null && nextStage != null) {
                        cheeseTypeToDestination.put(cheeseType, nextStage);
                        cheeseByType.computeIfAbsent(cheeseType, k -> new ArrayList<>())
                                .add(new CheeseLocation(tray, containerGob, bufferContainer));
                    }
                }
            }
            new CloseTargetContainer(bufferContainer).run(gui);
        }

        return new CheeseCollectionResult(cheeseTypeToDestination, cheeseByType);
    }

    /**
     * Process each collected cheese type efficiently
     *
     * @param gui The game UI
     * @param collectionResult The collected cheese data
     * @param area The specific area being processed (for navigation back)
     * @param place The place type
     */
    private void processCollectedCheeseByType(NGameUI gui, CheeseCollectionResult collectionResult, NArea area, CheeseBranch.Place place) throws InterruptedException {
        for (Map.Entry<String, CheeseBranch.Place> entry : collectionResult.cheeseTypeToDestination.entrySet()) {
            String cheeseType = entry.getKey();
            CheeseBranch.Place destination = entry.getValue();
            ArrayList<CheeseLocation> cheeseLocations = collectionResult.cheeseByType.get(cheeseType);

            if (cheeseLocations == null || cheeseLocations.isEmpty()) {
                continue; // No cheese of this type found
            }

            // Check destination capacity before doing anything
            int destinationCapacity = calculateDestinationCapacity(gui, destination);
            int alreadyMovedToDestination = traysMovedToAreas.getOrDefault(destination, 0);
            int remainingCapacity = Math.max(0, destinationCapacity - alreadyMovedToDestination);

            if (remainingCapacity == 0) {
                continue;
            }

            // Collect cheese from containers to inventory
            collectCheeseFromContainersToInventory(gui, cheeseLocations, destination, cheeseType, area, place);

            // Only navigate to destination if we actually collected something
            ArrayList<WItem> collectedCheese = gui.getInventory().getItems(new NAlias("Cheese Tray"));
            if (!collectedCheese.isEmpty()) {
                // Move collected cheese to final destination
                moveCollectedCheeseToDestination(gui, destination, cheeseType, area, place);
            }
        }
    }

    /**
     * Collect cheese from containers to inventory, handling inventory space efficiently
     * Continues collecting until all containers are empty or destination capacity is reached
     *
     * @param gui The game UI
     * @param cheeseLocations The cheese locations to collect from
     * @param destination The destination place type
     * @param cheeseType The cheese type being collected
     * @param area The specific area being processed (for navigation back)
     * @param place The place type of the source area
     */
    private void collectCheeseFromContainersToInventory(NGameUI gui, ArrayList<CheeseLocation> cheeseLocations,
                                                        CheeseBranch.Place destination, String cheeseType, NArea area, CheeseBranch.Place place) throws InterruptedException {
        // Calculate total destination capacity limits once
        int destinationCapacity = calculateDestinationCapacity(gui, destination);
        int alreadyMovedToDestination = traysMovedToAreas.getOrDefault(destination, 0);
        int remainingDestinationCapacity = Math.max(0, destinationCapacity - alreadyMovedToDestination);

        // Get list of container gobs to process
        ArrayList<Gob> containers = new ArrayList<>();
        for (CheeseLocation location : cheeseLocations) {
            if (!containers.contains(location.containerGob)) {
                containers.add(location.containerGob);
            }
        }

        // Refresh container references at the start - they may be stale from previous cheese type processing
        // This is necessary because cheeseLocations was captured before any area navigation
        if (!containers.isEmpty()) {
            containers = refindContainersInArea(gui, area, containers);
        }

        if (containers.isEmpty()) {
            return; // No containers found
        }

        // Continue collecting until all containers are empty or destination capacity reached
        while (remainingDestinationCapacity > 0) {
            boolean foundAnyTrays = false;
            boolean containersRefreshed = false;

            // Check each container for remaining trays
            for (Gob containerGob : containers) {
                // Check if we've reached destination capacity limit
                if (remainingDestinationCapacity <= 0) {
                    break;
                }

                // Check if inventory has space
                int availableSpace = CheeseInventoryOperations.getAvailableCheeseTraySlotsInInventory(gui);
                if (availableSpace <= 0) {
                    moveInventoryCheeseToDestination(gui, destination, cheeseType, area, place);
                    // Navigate back to source area
                    NContext context = new NContext(gui);
                    context.getAreaById(area.id);

                    // CRITICAL FIX: Refresh container references after returning from destination
                    // The original Gob references become stale when character moves far away
                    containers = refindContainersInArea(gui, area, containers);
                    if (containers.isEmpty()) {
                        break; // No containers found, exit the while loop
                    }

                    // Mark that we refreshed containers so the while-loop continues
                    containersRefreshed = true;

                    // Restart the for-loop with fresh container references
                    break;
                }

                // Take what fits in inventory, limited by remaining destination capacity
                int maxToTake = Math.min(remainingDestinationCapacity, availableSpace);

                // Take cheese from this container
                int takenFromContainer = takeCheeseFromSingleContainer(gui, containerGob, cheeseType, maxToTake, place);

                // Track moves for capacity calculation and update remaining capacity
                if (takenFromContainer > 0) {
                    traysMovedToAreas.put(destination, traysMovedToAreas.getOrDefault(destination, 0) + takenFromContainer);
                    remainingDestinationCapacity -= takenFromContainer;
                    foundAnyTrays = true;
                }
            }

            // If no trays were found in any container and we didn't just refresh, all containers are empty
            if (!foundAnyTrays && !containersRefreshed) {
                break;
            }
        }
    }

    /**
     * Take cheese from a single container using fresh WItem references
     * Returns the number of items actually taken
     */
    private int takeCheeseFromSingleContainer(NGameUI gui, Gob containerGob, String cheeseType, int maxToTake, CheeseBranch.Place currentPlace) throws InterruptedException {
        // Navigate to container
        NUtils.getUI().core.addTask(new WaitForGobWithHash(containerGob.ngob.hash));
        PathFinder pf = new PathFinder(containerGob);
        pf.isHardMode = true;
        pf.run(gui);

        // Open container
        Container container = new Container(containerGob, NContext.contcaps.get(containerGob.ngob.name), null);
        new OpenTargetContainer(container).run(gui);

        // Get FRESH WItem references after opening container
        ArrayList<WItem> availableTrays = CheeseInventoryOperations.getCheeseTraysFromContainer(gui, container);

        // Filter for the specific cheese type we want and limit to maxToTake
        ArrayList<WItem> targetTrays = new ArrayList<>();
        for (WItem tray : availableTrays) {
            if (targetTrays.size() >= maxToTake) break;

            if (CheeseUtils.shouldMoveToNextStage(tray, currentPlace)) {
                String trayCheeseType = CheeseUtils.getContentName(tray);
                if (cheeseType.equals(trayCheeseType)) {
                    targetTrays.add(tray);
                }
            }
        }

        // Take the items using fresh references
        int taken = 0;
        if (!targetTrays.isEmpty()) {
            for (WItem tray : targetTrays) {
                // Check inventory space
                if (gui.getInventory().getNumberFreeCoord(CheeseConstants.CHEESE_TRAY_SIZE) == 0) {
                    break;
                }

                // Transfer the item
                tray.item.wdgmsg("transfer", haven.Coord.z);
                NUtils.addTask(new ISRemoved(tray.item.wdgid()));
                taken++;
            }
        }

        // Close container
        new CloseTargetContainer(container).run(gui);

        return taken;
    }

    /**
     * Calculate how many cheese trays can be placed at the destination
     * Returns only the rack capacity - inventory space is handled separately
     */
    private int calculateDestinationCapacity(NGameUI gui, CheeseBranch.Place destination) throws InterruptedException {
        // Use the recorded rack capacity from ClearRacksAndRecordCapacity
        Integer recordedCapacity = recordedRackCapacity.get(destination);
        if (recordedCapacity == null || recordedCapacity <= 0) {
            return 0;
        }

        return recordedCapacity;
    }

    /**
     * Move all collected cheese of a specific type to its destination
     *
     * @param gui The game UI
     * @param destination The destination place type
     * @param cheeseType The cheese type being moved
     * @param area The source area (for navigation back)
     * @param place The source place type
     */
    private void moveCollectedCheeseToDestination(NGameUI gui, CheeseBranch.Place destination, String cheeseType, NArea area, CheeseBranch.Place place) throws InterruptedException {
        ArrayList<WItem> cheeseToMove = CheeseInventoryOperations.getCheeseTrays(gui);
        if (!cheeseToMove.isEmpty()) {
            moveInventoryCheeseToDestination(gui, destination, cheeseType, area, place);
            // Navigate back to source area for next cheese type
            NContext context = new NContext(gui);
            context.getAreaById(area.id);
        }
    }

    /**
     * Move cheese currently in inventory to destination area(s) with order updating
     * Supports multiple destination areas - fills one completely before moving to next
     *
     * @param gui The game UI
     * @param destination The destination place type
     * @param cheeseType The cheese type being moved
     * @param sourceArea The source area (for context)
     * @param fromPlace The source place type
     */
    private void moveInventoryCheeseToDestination(NGameUI gui, CheeseBranch.Place destination, String cheeseType, NArea sourceArea, CheeseBranch.Place fromPlace) throws InterruptedException {
        ArrayList<WItem> cheeseTrays = CheeseInventoryOperations.getCheeseTrays(gui);
        if (cheeseTrays.isEmpty()) {
            return;
        }

        // Get ALL destination areas for this place type
        ArrayList<NArea> destinationAreas = CheeseAreaManager.getAllCheeseAreas(destination);
        if (destinationAreas.isEmpty()) {
            gui.msg("No cheese racks area found for " + destination + ". Using FreeInventory2 as fallback.");
            NContext freshContext = new NContext(gui);
            new FreeInventory2(freshContext).run(gui);
            return;
        }

        // Process each destination area until all cheese is placed
        for (NArea destinationArea : destinationAreas) {
            cheeseTrays = CheeseInventoryOperations.getCheeseTrays(gui);
            if (cheeseTrays.isEmpty()) {
                break; // All cheese placed
            }

            // Navigate to this destination area
            NContext context = new NContext(gui);
            context.getAreaById(destinationArea.id);

            // Find and filter available racks in this area
            ArrayList<Gob> availableRacks = findAvailableRacksInArea(gui, destinationArea, destination);
            if (availableRacks.isEmpty()) {
                continue; // No space in this area, try next
            }

            // Place cheese on racks with order updating
            placeCheeseOnRacksWithOrderUpdates(gui, availableRacks, destination, cheeseType, fromPlace);
        }

        // Handle any remaining cheese that couldn't be placed in any destination area
        handleRemainingCheeseTrays(gui);
    }

    /**
     * Get and validate destination area, handling fallback if area not found
     */
    private NArea getValidatedDestinationArea(NGameUI gui, CheeseBranch.Place destination) throws InterruptedException {
        NArea destinationArea = CheeseAreaManager.getCheeseArea(gui, destination);
        if (destinationArea == null) {
            gui.msg("No cheese racks area found for " + destination + ". Using FreeInventory2 as fallback.");
            NContext freshContext = new NContext(gui);
            new FreeInventory2(freshContext).run(gui);
        }
        return destinationArea;
    }

    /**
     * Find available racks in the destination area, filtering out full ones
     */
    private ArrayList<Gob> findAvailableRacksInArea(NGameUI gui, NArea destinationArea, CheeseBranch.Place destination) throws InterruptedException {
        ArrayList<Gob> racks = Finder.findGobs(destinationArea, new NAlias("gfx/terobjs/cheeserack"));
        if (racks.isEmpty()) {
            gui.msg("No cheese racks found in " + destination + " area. Using FreeInventory2 as fallback.");
            NContext freshContext = new NContext(gui);
            new FreeInventory2(freshContext).run(gui);
            return new ArrayList<>();
        }

        // Filter out full racks using overlay checks
        ArrayList<Gob> availableRacks = new ArrayList<>();
        for (Gob rackGob : racks) {
            if (CheeseRackOverlayUtils.canAcceptTrays(rackGob)) {
                availableRacks.add(rackGob);
            }
        }

        return availableRacks;
    }

    /**
     * Place cheese on racks with order updates and proper inventory management
     */
    private void placeCheeseOnRacksWithOrderUpdates(NGameUI gui, ArrayList<Gob> availableRacks, CheeseBranch.Place destination,
                                                    String cheeseType, CheeseBranch.Place fromPlace) throws InterruptedException {
        ArrayList<WItem> cheeseTrays = gui.getInventory().getItems(new NAlias("Cheese Tray"));

        for (Gob rackGob : availableRacks) {
            if (cheeseTrays.isEmpty()) {
                break; // All cheese placed
            }

            Container rack = new Container(rackGob, "Rack", null);
            new PathFinder(rackGob).run(gui);
            new OpenTargetContainer(rack).run(gui);

            // Place trays on this rack (updates and saves orders inside)
            int traysPlaced = placeTraysOnSingleRack(gui, rack, cheeseTrays, cheeseType, fromPlace);

            // Refresh inventory after transfers
            cheeseTrays = gui.getInventory().getItems(new NAlias("Cheese Tray"));
            gui.msg("Placed " + traysPlaced + " trays on rack in " + destination);

            new CloseTargetContainer(rack).run(gui);
        }
    }

    /**
     * Place trays on a single rack, returning the number of trays actually placed
     */
    private int placeTraysOnSingleRack(NGameUI gui, Container rack, ArrayList<WItem> cheeseTrays, 
                                       String cheeseType, CheeseBranch.Place fromPlace) throws InterruptedException {
        int availableSpace = gui.getInventory(rack.cap).getNumberFreeCoord(CheeseConstants.CHEESE_TRAY_SIZE);
        if (availableSpace <= 0) {
            return 0; // No space on this rack
        }

        int traysToPlace = Math.min(availableSpace, cheeseTrays.size());
        for (int i = 0; i < traysToPlace; i++) {
            WItem tray = cheeseTrays.get(i);
            tray.item.wdgmsg("transfer", haven.Coord.z);
            nurgling.NUtils.addTask(new nurgling.tasks.ISRemoved(tray.item.wdgid()));
        }

        // Update and save orders after placing trays on rack
        if (traysToPlace > 0 && ordersManager != null && cheeseType != null && fromPlace != null) {
            updateOrdersAfterCheeseMovement(cheeseType, traysToPlace, fromPlace);
            ordersManager.writeOrders();
        }

        return traysToPlace;
    }

    /**
     * Handle cheese trays that couldn't be placed on racks
     */
    private void handleRemainingCheeseTrays(NGameUI gui) throws InterruptedException {
        ArrayList<WItem> remainingTrays = gui.getInventory().getItems(new NAlias("Cheese Tray"));
        if (!remainingTrays.isEmpty()) {
            NContext freshContext = new NContext(gui);
            new FreeInventory2(freshContext).run(gui);
        }
    }

    /**
     * Update cheese orders after moving cheese to the next stage.
     * Distributes moved cheese across multiple orders that need it (FIFO by order ID).
     * When cheese moves from current place to next place, we need to:
     * 1. Reduce count of current stage step by amount moved
     * 2. Add/increase count of next stage step by amount moved
     *
     * This fixes the batch overflow bug where all cheese was attributed to one order
     * even when multiple orders needed the same intermediate cheese type.
     */
    private void updateOrdersAfterCheeseMovement(String cheeseType, int movedCount,
                                                 CheeseBranch.Place fromPlace) {
        int remaining = movedCount;

        // Distribution loop - give cheese to orders until exhausted
        while (remaining > 0) {
            // Find next order that needs this cheese (sorted by ID for FIFO)
            CheeseOrder order = findOrderContainingCheeseType(cheeseType);
            if (order == null) {
                break; // No more orders need this cheese
            }

            // Find the current step for this cheese type
            CheeseOrder.StepStatus currentStep = null;
            for (CheeseOrder.StepStatus step : order.getStatus()) {
                if (step.name.equals(cheeseType) && step.place.equals(fromPlace.toString())) {
                    currentStep = step;
                    break;
                }
            }

            if (currentStep == null || currentStep.left <= 0) {
                break; // Shouldn't happen if findOrderContainingCheeseType works correctly
            }

            // Calculate how much this order can absorb
            int amountForThisOrder = Math.min(currentStep.left, remaining);

            // Find the next cheese step in this order's progression chain
            CheeseBranch.Cheese nextCheeseStep = getNextCheeseStepInChain(cheeseType, fromPlace, order.getCheeseType());
            if (nextCheeseStep == null) {
                break; // No next step found
            }

            // Update current step (reduce by amount allocated to this order)
            currentStep.left -= amountForThisOrder;

            // Update or create next step
            String nextCheeseType = nextCheeseStep.name;
            CheeseBranch.Place nextCheesePlace = nextCheeseStep.place;

            CheeseOrder.StepStatus nextStep = null;
            for (CheeseOrder.StepStatus step : order.getStatus()) {
                if (step.name.equals(nextCheeseType) && step.place.equals(nextCheesePlace.toString())) {
                    nextStep = step;
                    break;
                }
            }

            if (nextStep != null) {
                nextStep.left += amountForThisOrder;
            } else {
                // Create new step for next stage
                nextStep = new CheeseOrder.StepStatus(nextCheeseType, nextCheesePlace.toString(), amountForThisOrder);
                order.getStatus().add(nextStep);
            }

            ordersManager.addOrUpdateOrder(order);
            remaining -= amountForThisOrder;
        }

        // Log warning about orphan cheese (moved but not tracked by any order)
        if (remaining > 0) {
            NUtils.getGameUI().msg("Warning: " + remaining + " " + cheeseType + " trays moved but no order needs them");
        }

        if (movedCount > remaining) {
            ordersNeedSaving = true; // Mark that orders need saving if we updated any
        }
    }

    /**
     * Find the order that contains a specific cheese type in its progression
     * AND actually needs more of this cheese (step.left > 0).
     * Orders are sorted by ID (FIFO) to ensure deterministic distribution.
     * This fixes the bug where cheese was attributed to the wrong order when
     * multiple orders share the same intermediate cheese type.
     */
    private CheeseOrder findOrderContainingCheeseType(String cheeseType) {
        // Sort orders by ID for deterministic FIFO behavior
        ArrayList<CheeseOrder> sortedOrders = new ArrayList<>(ordersManager.getOrders().values());
        sortedOrders.sort((a, b) -> Integer.compare(a.getId(), b.getId()));

        for (CheeseOrder order : sortedOrders) {
            // Check if this order still needs this cheese type (step.left > 0)
            for (CheeseOrder.StepStatus step : order.getStatus()) {
                if (step.name.equals(cheeseType) && step.left > 0) {
                    return order; // Found order that actually needs this cheese
                }
            }
        }
        return null; // No matching order found
    }

    /**
     * Determine the next cheese step (name + place) in the progression chain for a specific target product
     * This ensures we get the correct progression chain when multiple chains share the same intermediate steps
     */
    private CheeseBranch.Cheese getNextCheeseStepInChain(String currentCheeseType, CheeseBranch.Place currentPlace, String targetProduct) {
        // Get the specific chain for the target product
        List<CheeseBranch.Cheese> chain = CheeseBranch.getChainToProduct(targetProduct);
        if (chain == null) {
            return null;
        }

        // Find the current step in this specific chain
        for (int i = 0; i < chain.size() - 1; i++) {
            CheeseBranch.Cheese currentStep = chain.get(i);
            if (currentStep.name.equals(currentCheeseType) && currentStep.place == currentPlace) {
                // Found the current step, return the next step
                return chain.get(i + 1);
            }
        }
        return null; // Next step NOT found (might be final product)
    }

    /**
     * Determine the correct next stage location for a cheese type using the specific order's progression
     * This fixes the issue where CheeseUtils.getNextStageLocation returns the wrong location for shared intermediate steps
     */
    private CheeseBranch.Place getCorrectNextStageLocation(String cheeseType, CheeseBranch.Place currentPlace) {
        // Find which order this cheese belongs to
        CheeseOrder relevantOrder = findOrderContainingCheeseType(cheeseType);
        if (relevantOrder == null) {
            // Fallback to the old method if no order found
            return CheeseUtils.getNextStageLocation(null, currentPlace); // Passing null since we only need the location logic
        }

        // Use the specific order's progression chain to determine correct destination
        CheeseBranch.Cheese nextStep = getNextCheeseStepInChain(cheeseType, currentPlace, relevantOrder.getCheeseType());
        return nextStep != null ? nextStep.place : null;
    }

    /**
     * Helper class to track cheese location information
     */
    private static class CheeseLocation {
        public final WItem tray;
        public final Gob containerGob;
        public final Container container;

        public CheeseLocation(WItem tray, Gob containerGob, Container container) {
            this.tray = tray;
            this.containerGob = containerGob;
            this.container = container;
        }
    }

    /**
     * Helper class to organize cheese collection results
     */
    private static class CheeseCollectionResult {
        public final Map<String, CheeseBranch.Place> cheeseTypeToDestination;
        public final Map<String, ArrayList<CheeseLocation>> cheeseByType;

        public CheeseCollectionResult(Map<String, CheeseBranch.Place> cheeseTypeToDestination,
                                      Map<String, ArrayList<CheeseLocation>> cheeseByType) {
            this.cheeseTypeToDestination = cheeseTypeToDestination;
            this.cheeseByType = cheeseByType;
        }
    }
}