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
    private boolean ordersNeedSaving = false;

    public ProcessCheeseFromBufferContainers(CheeseOrdersManager ordersManager) {
        this.slicingManager = new CheeseSlicingManager();
        this.ordersManager = ordersManager;
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
            gui.msg("Processing cheese from " + place + " buffer containers");
            processBufferContainers(gui, place);
        }

        // Batch save all order updates at the end - much more efficient than writing after every tray
        if (ordersNeedSaving) {
            gui.msg("Saving all order updates to disk (batched for efficiency)");
            ordersManager.writeOrders();
            ordersNeedSaving = false;
        }

        return Results.SUCCESS();
    }

    /**
     * Get the number of trays moved to each area during buffer processing
     * This impacts rack capacity - these areas now have fewer available slots
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
            gui.msg("Phase 1: Collecting ready-to-slice cheese from " + place + " buffers");
            collectReadyToSliceCheese(gui, containers, place);

            // Phase 2: Move remaining cheese to next stages
            gui.msg("Phase 2: Moving remaining cheese to next stages from " + place + " buffers");
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
                if((containerGob.ngob.name.equals("gfx/terobjs/chest") || containerGob.ngob.name.equals("gfx/terobjs/cupboard")) && containerGob.ngob.getModelAttribute() == 2) {
                    continue;
                }

                Container bufferContainer = new Container(containerGob, NContext.contcaps.get(containerGob.ngob.name));
                new PathFinder(containerGob).run(gui);
                new OpenTargetContainer(bufferContainer).run(gui);

                // Get all cheese trays from this container
                ArrayList<WItem> trays = CheeseInventoryOperations.getCheeseTraysFromContainer(gui, bufferContainer);

                // Process ready trays one by one to manage inventory space properly
                for (WItem tray : trays) {
                    if (CheeseUtils.isCheeseReadyToSlice(tray, ordersManager)) {
                        gui.msg("Found ready-to-slice cheese: " + CheeseUtils.getContentName(tray));

                        // Check if inventory has space for slicing (tray + up to 5 cheese pieces = 7 slots)
                        if (!CheeseInventoryOperations.hasSpaceForSlicing(gui)) {
                            new CloseTargetContainer(bufferContainer).run(gui);
                            freshContext = new NContext(gui);
                            new FreeInventory2(freshContext).run(gui);
                            
                            // CRITICAL FIX: After FreeInventory2, we need to re-find the area and containers
                            // because the character could be very far from the original location
                            containerGob = refindContainerAfterFreeInventory(gui, place, containerGob);
                            if (containerGob == null) {
                                gui.msg("Could not re-find container after FreeInventory2, skipping this container");
                                break; // Skip this container and move to next
                            }
                            bufferContainer = new Container(containerGob, NContext.contcaps.get(containerGob.ngob.name));
                            new PathFinder(containerGob).run(gui);
                            new OpenTargetContainer(bufferContainer).run(gui);
                        }

                        // Take the tray to inventory
                        tray.item.wdgmsg("transfer", haven.Coord.z);
                        nurgling.NUtils.addTask(new nurgling.tasks.ISRemoved(tray.item.wdgid()));
                        gui.msg("Took ready cheese to inventory: " + CheeseUtils.getContentName(tray));

                        // Close container to slice the cheese
                        new CloseTargetContainer(bufferContainer).run(gui);

                        // Find the tray we just took and slice it
                        ArrayList<WItem> inventoryTrays = CheeseInventoryOperations.getCheeseTrays(gui);
                        for (WItem inventoryTray : inventoryTrays) {
                            if (CheeseUtils.isCheeseReadyToSlice(inventoryTray, ordersManager)) {
                                gui.msg("Slicing cheese: " + CheeseUtils.getContentName(inventoryTray));
                                slicingManager.sliceCheese(gui, inventoryTray);
                                break; // Only slice one tray per iteration
                            }
                        }

                        // Check if inventory is getting full after slicing
                        if (!CheeseInventoryOperations.hasSpaceForSlicing(gui)) {
                            gui.msg("Inventory getting full after slicing. Freeing inventory...");
                            freshContext = new NContext(gui);
                            new FreeInventory2(freshContext).run(gui);
                            
                            // CRITICAL FIX: After FreeInventory2, re-find the container
                            containerGob = refindContainerAfterFreeInventory(gui, place, containerGob);
                            if (containerGob == null) {
                                gui.msg("Could not re-find container after slicing FreeInventory2, skipping this container");
                                break; // Skip this container and move to next
                            }
                            bufferContainer = new Container(containerGob, NContext.contcaps.get(containerGob.ngob.name));
                        }

                        // Reopen container to continue
                        new PathFinder(containerGob).run(gui);
                        new OpenTargetContainer(bufferContainer).run(gui);
                    }
                }

                new CloseTargetContainer(bufferContainer).run(gui);
        }

        // Final inventory cleanup after processing all ready cheese
        gui.msg("Final inventory cleanup after slicing ready cheese");
        new FreeInventory2(freshContext).run(gui);
    }
    
    /**
     * Re-find a container after FreeInventory2 has potentially moved the character far away
     * This is critical because after FreeInventory2, the original gob references may be out of range
     * 
     * @param gui The game UI
     * @param place The cheese area place where the container should be
     * @param originalGob The original container gob (used for matching by ID)
     * @return The re-found container gob, or null if not found
     */
    private Gob refindContainerAfterFreeInventory(NGameUI gui, CheeseBranch.Place place, Gob originalGob) throws InterruptedException {
        try {
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
                    gui.msg("Successfully re-found original container (ID: " + originalGobId + ") in " + place + " area");
                    return containerGob; // Found the exact same container
                }
            }
            
            // Step 4: If original container not found by ID, return the first available container of same type
            String originalContainerName = originalGob.ngob.name;
            for (Gob containerGob : containers) {
                if (containerGob.ngob.name.equals(originalContainerName)) {
                    gui.msg("Original container ID not found, using container of same type: " + originalContainerName + " in " + place + " area");
                    return containerGob;
                }
            }
            
            // Step 5: Fallback to first available container
            gui.msg("No matching container found, using first available container in " + place + " area");
            return containers.get(0);
            
        } catch (Exception e) {
            gui.msg("Error re-finding container after FreeInventory2: " + e.getMessage());
            return null;
        }
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
     * @return CheeseCollectionResult containing organized cheese data
     */
    private CheeseCollectionResult collectCheeseLocationsFromContainers(NGameUI gui, ArrayList<Gob> containers, CheeseBranch.Place place) throws InterruptedException {
        Map<String, CheeseBranch.Place> cheeseTypeToDestination = new HashMap<>();
        Map<String, ArrayList<CheeseLocation>> cheeseByType = new HashMap<>();

        for (Gob containerGob : containers) {
            // Skip checking empty containers.
            if((containerGob.ngob.name.equals("gfx/terobjs/chest") || containerGob.ngob.name.equals("gfx/terobjs/cupboard")) && containerGob.ngob.getModelAttribute() == 2) {
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

            gui.msg("Processing " + cheeseLocations.size() + " " + cheeseType + " trays for move to " + destination);
            
            // Collect cheese from containers to inventory
            collectCheeseFromContainersToInventory(gui, cheeseLocations, destination, cheeseType, place);
            
            // Move collected cheese to final destination
            moveCollectedCheeseToDestination(gui, destination, cheeseType, place);
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
                gui.msg("Inventory full! Moving current batch of " + cheeseType + " to " + destination);
                moveInventoryCheeseToDestination(gui, destination, cheeseType, place);
                CheeseAreaManager.getCheeseArea(gui, place); // Navigate back
            }

            // Calculate how many we can actually place at destination
            // Account for items already moved to this destination in current session
            int destinationCapacity = calculateDestinationCapacity(gui, destination);
            int alreadyMovedToDestination = traysMovedToAreas.getOrDefault(destination, 0);
            int remainingCapacity = Math.max(0, destinationCapacity - alreadyMovedToDestination);
            int maxToTake = Math.min(locationsInContainer.size(), remainingCapacity);
            
            if (maxToTake <= 0) {
                gui.msg("Destination " + destination + " has no remaining capacity for " + cheeseType + " (capacity: " + destinationCapacity + ", already moved: " + alreadyMovedToDestination + "), skipping");
                continue;
            }
            
            gui.msg("Taking " + maxToTake + " " + cheeseType + " trays (capacity: " + destinationCapacity + ", already moved: " + alreadyMovedToDestination + ", remaining: " + remainingCapacity + ")");
            
            // Take only what we can place at destination
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
                    gui.msg("Inventory full, stopping take from this container");
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
        
        gui.msg("Took " + taken + " " + cheeseType + " trays from container");
        return taken;
    }
    
    /**
     * Calculate how many cheese trays can be placed at the destination
     * Considers both available rack space and inventory capacity
     */
    private int calculateDestinationCapacity(NGameUI gui, CheeseBranch.Place destination) throws InterruptedException {
        try {
            NArea destinationArea = CheeseAreaManager.getCheeseArea(gui, destination);
            if (destinationArea == null) {
                gui.msg("No area found for " + destination + ", capacity = 0");
                return 0;
            }
            
            // Find available racks in destination area
            ArrayList<Gob> racks = Finder.findGobs(destinationArea, new NAlias("gfx/terobjs/cheeserack"));
            if (racks.isEmpty()) {
                gui.msg("No racks found in " + destination + " area, capacity = 0");
                return 0;
            }
            
            // Calculate total available capacity across all racks
            int totalCapacity = 0;
            for (Gob rackGob : racks) {
                if (CheeseRackOverlayUtils.canAcceptTrays(rackGob)) {
                    // For now, assume each rack can hold up to 3 trays
                    // TODO: Could make this more precise by checking actual rack inventory
                    totalCapacity += CheeseConstants.RACK_CAPACITY_TRAYS;
                } else {
                    gui.msg("Rack is full (overlay check), skipping");
                }
            }
            
            // Also consider player inventory capacity as a limiting factor
            int inventoryCapacity = CheeseInventoryOperations.getAvailableCheeseTraySlotsInInventory(gui);
            int effectiveCapacity = Math.min(totalCapacity, inventoryCapacity);
            
            gui.msg("Destination " + destination + " capacity: " + effectiveCapacity + " (racks: " + totalCapacity + ", inventory: " + inventoryCapacity + ")");
            return effectiveCapacity;
            
        } catch (Exception e) {
            gui.msg("Error calculating destination capacity for " + destination + ": " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * Helper to determine the current place based on container location
     */
    private CheeseBranch.Place getCurrentPlace(Gob containerGob) {
        // This is a simplified version - you may need to implement proper area detection
        // For now, we'll need to track this differently or pass it as parameter
        return CheeseBranch.Place.outside; // TODO: Implement proper place detection
    }
    
    /**
     * Move all collected cheese of a specific type to its destination
     */
    private void moveCollectedCheeseToDestination(NGameUI gui, CheeseBranch.Place destination, String cheeseType, CheeseBranch.Place place) throws InterruptedException {
        ArrayList<WItem> cheeseToMove = CheeseInventoryOperations.getCheeseTrays(gui);
        if (!cheeseToMove.isEmpty()) {
            gui.msg("Moving collected " + cheeseType + " to " + destination);
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

        gui.msg("Moving " + cheeseTrays.size() + " cheese trays to " + destination + " area");
        
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
            } else {
                gui.msg("Skipping full rack (overlay check) - no space available");
            }
        }

        gui.msg("Found " + availableRacks.size() + " racks with space out of " + racks.size() + " total racks");
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

            // Place trays on this rack
            int traysPlaced = placeTraysOnSingleRack(gui, rack, cheeseTrays);
            
            // Update orders if trays were placed successfully
            if (traysPlaced > 0 && cheeseType != null && fromPlace != null) {
                updateOrdersAfterCheeseMovement(gui, cheeseType, traysPlaced, fromPlace);
            }
            
            // Refresh inventory after transfers
            cheeseTrays = gui.getInventory().getItems(new NAlias("Cheese Tray"));
            gui.msg("Placed " + traysPlaced + " trays on rack in " + destination);

            new CloseTargetContainer(rack).run(gui);
        }

        // Handle any remaining cheese that couldn't be placed
        handleRemainingCheeseTrays(gui, destination);
    }
    
    /**
     * Place trays on a single rack, returning the number of trays actually placed
     */
    private int placeTraysOnSingleRack(NGameUI gui, Container rack, ArrayList<WItem> cheeseTrays) throws InterruptedException {
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
        
        return traysToPlace;
    }
    
    /**
     * Handle cheese trays that couldn't be placed on racks
     */
    private void handleRemainingCheeseTrays(NGameUI gui, CheeseBranch.Place destination) throws InterruptedException {
        ArrayList<WItem> remainingTrays = gui.getInventory().getItems(new NAlias("Cheese Tray"));
        if (!remainingTrays.isEmpty()) {
            gui.msg("Warning: " + remainingTrays.size() + " cheese trays couldn't fit in " + destination + " racks. Using FreeInventory2.");
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
    private void updateOrdersAfterCheeseMovement(NGameUI gui, String cheeseType, int movedCount,
                                                CheeseBranch.Place fromPlace) {
        try {
            // First find which order this cheese belongs to
            CheeseOrder relevantOrder = findOrderContainingCheeseType(cheeseType);
            if (relevantOrder == null) {
                gui.msg("Warning: No order found containing cheese type " + cheeseType);
                return;
            }

            // Find the next cheese type and its correct location in the progression chain for this specific order
            CheeseBranch.Cheese nextCheeseStep = getNextCheeseStepInChain(cheeseType, fromPlace, relevantOrder.getCheeseType());
            if (nextCheeseStep == null) {
                gui.msg("Warning: Could not determine next cheese step for " + cheeseType + " from " + fromPlace + " in " + relevantOrder.getCheeseType() + " chain");
                return;
            }

            String nextCheeseType = nextCheeseStep.name;
            CheeseBranch.Place nextCheesePlace = nextCheeseStep.place;

            // Look for current stage step and reduce it
            for (CheeseOrder.StepStatus step : relevantOrder.getStatus()) {
                if (step.name.equals(cheeseType) && step.place.equals(fromPlace.toString())) {
                    step.left = Math.max(0, step.left - movedCount);
                    gui.msg("Updated order " + relevantOrder.getCheeseType() + ": reduced " + cheeseType +
                           " at " + fromPlace + " by " + movedCount + " (now " + step.left + " left)");
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
                gui.msg("Updated order " + relevantOrder.getCheeseType() + ": increased " + nextCheeseType +
                       " at " + nextCheesePlace + " by " + movedCount + " (now " + nextStep.left + " left)");
            } else {
                // Create new step for next stage using correct progression location
                nextStep = new CheeseOrder.StepStatus(nextCheeseType, nextCheesePlace.toString(), movedCount);
                relevantOrder.getStatus().add(nextStep);
                gui.msg("Created new step in order " + relevantOrder.getCheeseType() + ": " + nextCheeseType +
                       " at " + nextCheesePlace + " with " + movedCount + " trays");
            }

            ordersManager.addOrUpdateOrder(relevantOrder);
            ordersNeedSaving = true; // Mark that orders need saving, but don't save yet

        } catch (Exception e) {
            gui.msg("Error updating orders after cheese movement: " + e.getMessage());
        }
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