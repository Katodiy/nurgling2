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
                CheeseBranch.Place.outside,
                CheeseBranch.Place.inside,
                CheeseBranch.Place.mine,
                CheeseBranch.Place.cellar
        };

        for (CheeseBranch.Place place : places) {
            // Skip areas that have all empty buffers (optimization from ClearRacksAndRecordCapacity)
            if (bufferEmptinessMap.containsKey(place) && bufferEmptinessMap.get(place)) {
                continue;
            }

            processBufferContainers(gui, place);
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
     */
    private void processBufferContainers(NGameUI gui, CheeseBranch.Place place) throws InterruptedException {
        // Get cheese area using centralized manager
        NArea area = CheeseAreaManager.getCheeseArea(gui, place);
        if (area == null) {
            gui.msg("No cheese area found for " + place);
            return;
        }

        // Find buffer containers in this area
        ArrayList<Gob> containers = Finder.findGobs(area, new NAlias(new ArrayList<>(NContext.contcaps.keySet()), new ArrayList<>()));

        // Phase 1: Collect ready-to-slice cheese
        collectReadyToSliceCheese(gui, containers, place);

        area = CheeseAreaManager.getCheeseArea(gui, place);
        if (area == null) {
            gui.msg("No cheese area found for " + place);
            return;
        }

        // Phase 2: Move remaining cheese to next stages
        moveRemainingCheeseToNextStage(gui, containers, place);
    }

    /**
     * Phase 1: Collect ready-to-slice cheese from buffer containers and slice them
     */
    private void collectReadyToSliceCheese(NGameUI gui, ArrayList<Gob> containers, CheeseBranch.Place place) throws InterruptedException {
        // Use centralized constants for sizes
        NContext freshContext = new NContext(gui);

        for (Gob containerGob : containers) {
            // Skip checking empty containers.
            if ((containerGob.ngob.name.equals("gfx/terobjs/chest") || containerGob.ngob.name.equals("gfx/terobjs/cupboard")) && containerGob.ngob.getModelAttribute() == 2) {
                continue;
            }

            Container bufferContainer = new Container(containerGob, NContext.contcaps.get(containerGob.ngob.name));
            new PathFinder(containerGob).run(gui);
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
                    containerGob = refindContainerAfterFreeInventory(gui, place, containerGob);
                    if (containerGob == null) {
                        break; // Skip this container and move to next
                    }
                    bufferContainer = new Container(containerGob, NContext.contcaps.get(containerGob.ngob.name));
                    new PathFinder(containerGob).run(gui);
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
                    containerGob = refindContainerAfterFreeInventory(gui, place, containerGob);
                    if (containerGob == null) {
                        break;
                    }
                    bufferContainer = new Container(containerGob, NContext.contcaps.get(containerGob.ngob.name));
                    new PathFinder(containerGob).run(gui);
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
     * @param place       The cheese area place where the container should be
     * @param originalGob The original container gob (used for matching by ID)
     * @return The re-found container gob, or null if not found
     */
    private Gob refindContainerAfterFreeInventory(NGameUI gui, CheeseBranch.Place place, Gob originalGob) throws InterruptedException {
        // Step 1: Navigate back to the cheese area
        NArea area = CheeseAreaManager.getCheeseArea(gui, place);
        if (area == null) {
            gui.msg("Could not find cheese area for " + place + " after FreeInventory2");
            return null;
        }

        // Step 2: Find containers in the area again
        ArrayList<Gob> containers = Finder.findGobs(area, new NAlias(new ArrayList<>(NContext.contcaps.keySet()), new ArrayList<>()));
        if (containers.isEmpty()) {
            gui.msg("No containers found in " + place + " area after FreeInventory2");
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
     * Phase 2: Move remaining cheese to next aging stages
     * Process one cheese type at a time for efficient batching
     */
    private void moveRemainingCheeseToNextStage(NGameUI gui, ArrayList<Gob> containers, CheeseBranch.Place place) throws InterruptedException {
        // Step 1: Collect all cheese locations and destinations in a single pass
        CheeseCollectionResult collectionResult = collectCheeseLocationsFromContainers(gui, containers, place);

        // Step 2: Process each cheese type efficiently
        processCollectedCheeseByType(gui, collectionResult, place);
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
            Container bufferContainer = new Container(containerGob, NContext.contcaps.get(containerGob.ngob.name));
            new PathFinder(containerGob).run(gui);
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
     */
    private void processCollectedCheeseByType(NGameUI gui, CheeseCollectionResult collectionResult, CheeseBranch.Place place) throws InterruptedException {
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
            collectCheeseFromContainersToInventory(gui, cheeseLocations, destination, cheeseType, place);

            // Only navigate to destination if we actually collected something
            ArrayList<WItem> collectedCheese = gui.getInventory().getItems(new NAlias("Cheese Tray"));
            if (!collectedCheese.isEmpty()) {
                // Move collected cheese to final destination
                moveCollectedCheeseToDestination(gui, destination, cheeseType, place);
            }
        }
    }

    /**
     * Collect cheese from containers to inventory, handling inventory space efficiently
     * Groups items by container to minimize open/close operations
     */
    private void collectCheeseFromContainersToInventory(NGameUI gui, ArrayList<CheeseLocation> cheeseLocations,
                                                        CheeseBranch.Place destination, String cheeseType, CheeseBranch.Place place) throws InterruptedException {
        // Group cheese locations by container to batch operations
        Map<Gob, List<CheeseLocation>> itemsByContainer = new HashMap<>();
        for (CheeseLocation location : cheeseLocations) {
            itemsByContainer.computeIfAbsent(location.containerGob, k -> new ArrayList<>()).add(location);
        }

        // Process each container once
        for (Map.Entry<Gob, List<CheeseLocation>> entry : itemsByContainer.entrySet()) {
            Gob containerGob = entry.getKey();
            List<CheeseLocation> locationsInContainer = entry.getValue();

            // Check if inventory has space
            int availableSpace = CheeseInventoryOperations.getAvailableCheeseTraySlotsInInventory(gui);
            if (availableSpace <= 0) {
                moveInventoryCheeseToDestination(gui, destination, cheeseType, place);
                CheeseAreaManager.getCheeseArea(gui, place); // Navigate back
            }

            // Calculate how many we can actually take based on destination capacity and inventory space
            int destinationCapacity = calculateDestinationCapacity(gui, destination);
            int alreadyMovedToDestination = traysMovedToAreas.getOrDefault(destination, 0);
            int remainingDestinationCapacity = Math.max(0, destinationCapacity - alreadyMovedToDestination);
            
            int inventorySpace = CheeseInventoryOperations.getAvailableCheeseTraySlotsInInventory(gui);
            int maxToTake = Math.min(locationsInContainer.size(), 
                                   Math.min(remainingDestinationCapacity, inventorySpace));

            // Only take what we can actually place at destination
            int takenFromContainer = takeCheeseFromSingleContainer(gui, containerGob, cheeseType, maxToTake, place);

            // Track moves for capacity calculation
            if (takenFromContainer > 0) {
                traysMovedToAreas.put(destination, traysMovedToAreas.getOrDefault(destination, 0) + takenFromContainer);
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
        new PathFinder(containerGob).run(gui);

        // Open container
        Container container = new Container(containerGob, NContext.contcaps.get(containerGob.ngob.name));
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
     * Considers both available rack space and inventory capacity
     */
    private int calculateDestinationCapacity(NGameUI gui, CheeseBranch.Place destination) throws InterruptedException {
        // Use the recorded rack capacity from ClearRacksAndRecordCapacity
        Integer recordedCapacity = recordedRackCapacity.get(destination);
        if (recordedCapacity == null || recordedCapacity <= 0) {
            return 0;
        }

        // Also consider player inventory capacity as a limiting factor
        int inventoryCapacity = CheeseInventoryOperations.getAvailableCheeseTraySlotsInInventory(gui);

        return Math.min(recordedCapacity, inventoryCapacity);
    }

    /**
     * Move all collected cheese of a specific type to its destination
     */
    private void moveCollectedCheeseToDestination(NGameUI gui, CheeseBranch.Place destination, String cheeseType, CheeseBranch.Place place) throws InterruptedException {
        ArrayList<WItem> cheeseToMove = CheeseInventoryOperations.getCheeseTrays(gui);
        if (!cheeseToMove.isEmpty()) {
            moveInventoryCheeseToDestination(gui, destination, cheeseType, place);
            CheeseAreaManager.getCheeseArea(gui, place); // Navigate back for next cheese type
        }
    }

    /**
     * Move cheese currently in inventory to a specific destination area with order updating
     */
    private void moveInventoryCheeseToDestination(NGameUI gui, CheeseBranch.Place destination, String cheeseType, CheeseBranch.Place fromPlace) throws InterruptedException {
        ArrayList<WItem> cheeseTrays = CheeseInventoryOperations.getCheeseTrays(gui);
        if (cheeseTrays.isEmpty()) {
            return;
        }

        // Get destination area and validate
        NArea destinationArea = getValidatedDestinationArea(gui, destination);
        if (destinationArea == null) {
            return; // Error handled in getValidatedDestinationArea
        }

        // Find and filter available racks
        ArrayList<Gob> availableRacks = findAvailableRacksInArea(gui, destinationArea, destination);
        if (availableRacks.isEmpty()) {
            return; // Error handled in findAvailableRacksInArea
        }

        // Place cheese on racks with order updating
        placeCheeseOnRacksWithOrderUpdates(gui, availableRacks, destination, cheeseType, fromPlace);
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

            Container rack = new Container(rackGob, "Rack");
            new PathFinder(rackGob).run(gui);
            new OpenTargetContainer(rack).run(gui);

            // Place trays on this rack (updates and saves orders inside)
            int traysPlaced = placeTraysOnSingleRack(gui, rack, cheeseTrays, cheeseType, fromPlace);

            // Refresh inventory after transfers
            cheeseTrays = gui.getInventory().getItems(new NAlias("Cheese Tray"));
            gui.msg("Placed " + traysPlaced + " trays on rack in " + destination);

            new CloseTargetContainer(rack).run(gui);
        }

        // Handle any remaining cheese that couldn't be placed
        handleRemainingCheeseTrays(gui);
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
     * Update cheese orders after moving cheese to the next stage
     * When cheese moves from current place to next place, we need to:
     * 1. Reduce count of current stage step by amount moved
     * 2. Add/increase count of next stage step by amount moved
     */
    private void updateOrdersAfterCheeseMovement(String cheeseType, int movedCount,
                                                 CheeseBranch.Place fromPlace) {
        // First find which order this cheese belongs to
        CheeseOrder relevantOrder = findOrderContainingCheeseType(cheeseType);
        if (relevantOrder == null) {
            return;
        }

        // Find the next cheese type and its correct location in the progression chain for this specific order
        CheeseBranch.Cheese nextCheeseStep = getNextCheeseStepInChain(cheeseType, fromPlace, relevantOrder.getCheeseType());
        if (nextCheeseStep == null) {
            return;
        }

        String nextCheeseType = nextCheeseStep.name;
        CheeseBranch.Place nextCheesePlace = nextCheeseStep.place;

        // Look for current stage step and reduce it
        for (CheeseOrder.StepStatus step : relevantOrder.getStatus()) {
            if (step.name.equals(cheeseType) && step.place.equals(fromPlace.toString())) {
                step.left = Math.max(0, step.left - movedCount);
                break;
            }
        }

        // Look for next stage step and increase it (or create it)
        // Use the correct cheese progression place, not the physical destination
        CheeseOrder.StepStatus nextStep = null;
        for (CheeseOrder.StepStatus step : relevantOrder.getStatus()) {
            if (step.name.equals(nextCheeseType) && step.place.equals(nextCheesePlace.toString())) {
                nextStep = step;
                break;
            }
        }

        if (nextStep != null) {
            nextStep.left += movedCount;
        } else {
            // Create new step for next stage using correct progression location
            nextStep = new CheeseOrder.StepStatus(nextCheeseType, nextCheesePlace.toString(), movedCount);
            relevantOrder.getStatus().add(nextStep);
        }

        ordersManager.addOrUpdateOrder(relevantOrder);
        ordersNeedSaving = true; // Mark that orders need saving, but don't save yet
    }

    /**
     * Find the order that contains a specific cheese type in its progression
     */
    private CheeseOrder findOrderContainingCheeseType(String cheeseType) {
        for (CheeseOrder order : ordersManager.getOrders().values()) {
            // Check if this order's progression chain contains the cheese type
            List<CheeseBranch.Cheese> chain = CheeseBranch.getChainToProduct(order.getCheeseType());
            if (chain != null) {
                for (CheeseBranch.Cheese step : chain) {
                    if (step.name.equals(cheeseType)) {
                        return order; // Found order with matching progression
                    }
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