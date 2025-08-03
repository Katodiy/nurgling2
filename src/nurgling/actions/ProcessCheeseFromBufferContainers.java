package nurgling.actions;

import haven.Gob;
import haven.WItem;
import nurgling.NGameUI;
import nurgling.areas.NArea;
import nurgling.areas.NContext;
import nurgling.cheese.CheeseBranch;
import nurgling.actions.bots.cheese.CheeseUtils;
import nurgling.actions.bots.cheese.CheeseSlicingManager;
import nurgling.actions.bots.cheese.CheeseRackOverlayUtils;
import nurgling.actions.FreeInventory2;
import nurgling.actions.TakeWItemsFromContainer;
import nurgling.tools.Container;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.widgets.Specialisation;
import nurgling.cheese.CheeseOrder;
import nurgling.cheese.CheeseOrdersManager;
import nurgling.cheese.CheeseBranch;

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
    private CheeseSlicingManager slicingManager;
    private Map<CheeseBranch.Place, Integer> traysMovedToAreas = new HashMap<>();
    private CheeseOrdersManager ordersManager;
    
    public ProcessCheeseFromBufferContainers() {
        this.slicingManager = new CheeseSlicingManager();
        this.ordersManager = new CheeseOrdersManager();
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
            // Create fresh context to avoid caching issues
            NContext freshContext = new NContext(gui);
            NArea area = freshContext.getSpecArea(Specialisation.SpecName.cheeseRacks, place.toString());
            if (area == null) {
                gui.msg("No cheese area found for " + place);
                return;
            }
            
            // Find buffer containers in this area
            ArrayList<Gob> containers = Finder.findGobs(area, new NAlias(new ArrayList<String>(NContext.contcaps.keySet()), new ArrayList<>()));
            
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
        final haven.Coord SINGLE_SLOT = new haven.Coord(1, 1);
        NContext freshContext = new NContext(gui);
        
        for (Gob containerGob : containers) {
                Container bufferContainer = new Container(containerGob, NContext.contcaps.get(containerGob.ngob.name));
                new PathFinder(containerGob).run(gui);
                new OpenTargetContainer(bufferContainer).run(gui);
                
                // Get all cheese trays from this container
                ArrayList<WItem> trays = gui.getInventory(bufferContainer.cap).getItems(new NAlias("Cheese Tray"));
                
                // Process ready trays one by one to manage inventory space properly
                for (WItem tray : trays) {
                    if (CheeseUtils.isCheeseReadyToSlice(tray)) {
                        gui.msg("Found ready-to-slice cheese: " + CheeseUtils.getContentName(tray));
                        
                        // Check if inventory has space for tray + cheese pieces (worst case: 5 pieces + tray = 7 slots)
                        int availableSlots = gui.getInventory().getNumberFreeCoord(SINGLE_SLOT);
                        if (availableSlots < 7) {
                            gui.msg("Not enough inventory space for slicing (need 7 slots, have " + availableSlots + "). Freeing inventory...");
                            new CloseTargetContainer(bufferContainer).run(gui);
                            new FreeInventory2(freshContext).run(gui);
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
                        ArrayList<WItem> inventoryTrays = gui.getInventory().getItems(new NAlias("Cheese Tray"));
                        for (WItem inventoryTray : inventoryTrays) {
                            if (CheeseUtils.isCheeseReadyToSlice(inventoryTray)) {
                                gui.msg("Slicing cheese: " + CheeseUtils.getContentName(inventoryTray));
                                slicingManager.sliceCheese(gui, inventoryTray);
                                break; // Only slice one tray per iteration
                            }
                        }
                        
                        // Check if inventory is getting full after slicing
                        int remainingSpace = gui.getInventory().getNumberFreeCoord(SINGLE_SLOT);
                        if (remainingSpace < 7) {
                            gui.msg("Inventory getting full after slicing. Freeing inventory...");
                            new FreeInventory2(freshContext).run(gui);
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
     * Phase 2: Move remaining cheese to next aging stages
     * Process one cheese type at a time for efficient batching
     */
    private void moveRemainingCheeseToNextStage(NGameUI gui, ArrayList<Gob> containers, CheeseBranch.Place place) throws InterruptedException {
        final haven.Coord TRAY_SIZE = new haven.Coord(1, 2);
        
        // Process each container individually for cheese type batching
        Map<String, CheeseBranch.Place> cheeseTypeToDestination = new HashMap<>();
        
        // Collect cheese types and their destinations first
        for (Gob containerGob : containers) {
            Container bufferContainer = new Container(containerGob, NContext.contcaps.get(containerGob.ngob.name));
            new PathFinder(containerGob).run(gui);
            new OpenTargetContainer(bufferContainer).run(gui);
            
            ArrayList<WItem> trays = gui.getInventory(bufferContainer.cap).getItems(new NAlias("Cheese Tray"));
            for (WItem tray : trays) {
                if (CheeseUtils.shouldMoveToNextStage(tray, place)) {
                    String cheeseType = CheeseUtils.getContentName(tray);
                    
                    // Determine correct destination using the specific order's progression chain
                    CheeseBranch.Place nextStage = getCorrectNextStageLocation(cheeseType, place);
                    
                    if (cheeseType != null && nextStage != null) {
                        cheeseTypeToDestination.put(cheeseType, nextStage);
                    }
                }
            }
            
            new CloseTargetContainer(bufferContainer).run(gui);
        }
        
        // Process each cheese type separately
        for (Map.Entry<String, CheeseBranch.Place> entry : cheeseTypeToDestination.entrySet()) {
            String cheeseType = entry.getKey();
            CheeseBranch.Place destination = entry.getValue();
            
            gui.msg("Processing " + cheeseType + " trays for move to " + destination);
            
            // Collect all cheese of this type from all containers
            while (true) {
                // Check if inventory has space
                int availableSpace = gui.getInventory().getNumberFreeCoord(TRAY_SIZE);
                if (availableSpace <= 0) {
                    gui.msg("Inventory full! Moving current batch of " + cheeseType + " to " + destination);
                    moveInventoryCheeseToDestination(gui, destination, cheeseType, place);
                    
                    // Navigate back to original area to continue processing
                    NContext freshContext = new NContext(gui);
                    freshContext.getSpecArea(Specialisation.SpecName.cheeseRacks, place.toString());
                }
                
                // Find matching cheese in any container
                ArrayList<WItem> matchingTrays = new ArrayList<>();
                Container currentContainer = null;
                
                for (Gob containerGob : containers) {
                    Container bufferContainer = new Container(containerGob, NContext.contcaps.get(containerGob.ngob.name));
                    new PathFinder(containerGob).run(gui);
                    new OpenTargetContainer(bufferContainer).run(gui);
                    
                    ArrayList<WItem> trays = gui.getInventory(bufferContainer.cap).getItems(new NAlias("Cheese Tray"));
                    for (WItem tray : trays) {
                        if (CheeseUtils.shouldMoveToNextStage(tray, place)) {
                            String currentCheeseType = CheeseUtils.getContentName(tray);
                            if (cheeseType.equals(currentCheeseType)) {
                                matchingTrays.add(tray);
                            }
                        }
                    }
                    
                    if (!matchingTrays.isEmpty()) {
                        currentContainer = bufferContainer;
                        break; // Found cheese in this container
                    }
                    
                    new CloseTargetContainer(bufferContainer).run(gui);
                }
                
                if (matchingTrays.isEmpty()) {
                    break; // No more cheese of this type found
                }
                
                // Take cheese using TakeWItemsFromContainer
                int traysToTake = Math.min(matchingTrays.size(), availableSpace);
                ArrayList<WItem> traysToTakeList = new ArrayList<>(matchingTrays.subList(0, traysToTake));
                
                new TakeWItemsFromContainer(currentContainer, traysToTakeList).run(gui);
                
                // Track these moves for capacity calculation
                traysMovedToAreas.put(destination, traysMovedToAreas.getOrDefault(destination, 0) + traysToTake);
                
                new CloseTargetContainer(currentContainer).run(gui);
            }
            
            // Move all collected cheese of this type to destination
            ArrayList<WItem> cheeseToMove = gui.getInventory().getItems(new NAlias("Cheese Tray"));
            if (!cheeseToMove.isEmpty()) {
                gui.msg("Moving collected " + cheeseType + " to " + destination);
                moveInventoryCheeseToDestination(gui, destination, cheeseType, place);
                
                // Navigate back to original area for next cheese type
                NContext freshContext = new NContext(gui);
                freshContext.getSpecArea(Specialisation.SpecName.cheeseRacks, place.toString());
            }
        }
    }
    
    /**
     * Move cheese currently in inventory to a specific destination area
     * Updates orders immediately after each tray is successfully placed
     */
    private void moveInventoryCheeseToDestination(NGameUI gui, CheeseBranch.Place destination) throws InterruptedException {
        moveInventoryCheeseToDestination(gui, destination, null, null);
    }
    
    /**
     * Move cheese currently in inventory to a specific destination area without updating orders
     * Used when we want to update orders only once per cheese type at the end
     */
    private void moveInventoryCheeseToDestinationNoOrderUpdate(NGameUI gui, CheeseBranch.Place destination) throws InterruptedException {
        ArrayList<WItem> cheeseTrays = gui.getInventory().getItems(new NAlias("Cheese Tray"));
        if (cheeseTrays.isEmpty()) {
            return;
        }
        
        gui.msg("Moving " + cheeseTrays.size() + " cheese trays to " + destination + " area (no order update)");
        
        // Navigate to destination area
        NContext freshContext = new NContext(gui);
        NArea destinationArea = freshContext.getSpecArea(Specialisation.SpecName.cheeseRacks, destination.toString());
        if (destinationArea == null) {
            gui.msg("No cheese racks area found for " + destination + ". Using FreeInventory2 as fallback.");
            new FreeInventory2(freshContext).run(gui);
            return;
        }
        
        // Find available racks in destination area
        ArrayList<Gob> racks = Finder.findGobs(destinationArea, new NAlias("gfx/terobjs/cheeserack"));
        if (racks.isEmpty()) {
            gui.msg("No cheese racks found in " + destination + " area. Using FreeInventory2 as fallback.");
            new FreeInventory2(freshContext).run(gui);
            return;
        }
        
        // Place cheese trays on racks WITHOUT updating orders - filter full racks first
        final haven.Coord TRAY_SIZE = new haven.Coord(1, 2);
        ArrayList<Gob> availableRacks = new ArrayList<>();
        for (Gob rackGob : racks) {
            if (CheeseRackOverlayUtils.canAcceptTrays(rackGob)) {
                availableRacks.add(rackGob);
            }
        }
        
        for (Gob rackGob : availableRacks) {
            cheeseTrays = gui.getInventory().getItems(new NAlias("Cheese Tray"));
            if (cheeseTrays.isEmpty()) {
                break; // All cheese placed
            }
            
            Container rack = new Container(rackGob, "Rack");
            new PathFinder(rackGob).run(gui);
            new OpenTargetContainer(rack).run(gui);
            
            // Check available space in this rack
            int availableSpace = gui.getInventory(rack.cap).getNumberFreeCoord(TRAY_SIZE);
            if (availableSpace > 0) {
                // Transfer trays to this rack but DON'T update orders
                int traysToPlace = Math.min(availableSpace, cheeseTrays.size());
                for (int i = 0; i < traysToPlace; i++) {
                    WItem tray = cheeseTrays.get(i);
                    tray.item.wdgmsg("transfer", haven.Coord.z);
                    nurgling.NUtils.addTask(new nurgling.tasks.ISRemoved(tray.item.wdgid()));
                }
                
                gui.msg("Placed " + traysToPlace + " trays on rack in " + destination + " (no order update)");
            }
            
            new CloseTargetContainer(rack).run(gui);
        }
        
        // If any cheese remains, use FreeInventory2 as fallback
        cheeseTrays = gui.getInventory().getItems(new NAlias("Cheese Tray"));
        if (!cheeseTrays.isEmpty()) {
            gui.msg("Warning: " + cheeseTrays.size() + " cheese trays couldn't fit in " + destination + " racks. Using FreeInventory2.");
            new FreeInventory2(freshContext).run(gui);
        }
    }
    
    /**
     * Move cheese currently in inventory to a specific destination area with order updating
     */
    private void moveInventoryCheeseToDestination(NGameUI gui, CheeseBranch.Place destination, String cheeseType, CheeseBranch.Place fromPlace) throws InterruptedException {
        ArrayList<WItem> cheeseTrays = gui.getInventory().getItems(new NAlias("Cheese Tray"));
        if (cheeseTrays.isEmpty()) {
            return;
        }
        
        gui.msg("Moving " + cheeseTrays.size() + " cheese trays to " + destination + " area");
        
        // Navigate to destination area
        NContext freshContext = new NContext(gui);
        NArea destinationArea = freshContext.getSpecArea(Specialisation.SpecName.cheeseRacks, destination.toString());
        if (destinationArea == null) {
            gui.msg("No cheese racks area found for " + destination + ". Using FreeInventory2 as fallback.");
            new FreeInventory2(freshContext).run(gui);
            return;
        }
        
        // Find available racks in destination area
        ArrayList<Gob> racks = Finder.findGobs(destinationArea, new NAlias("gfx/terobjs/cheeserack"));
        if (racks.isEmpty()) {
            gui.msg("No cheese racks found in " + destination + " area. Using FreeInventory2 as fallback.");
            new FreeInventory2(freshContext).run(gui);
            return;
        }
        
        // Place cheese trays on racks - filter out full racks first using overlays
        final haven.Coord TRAY_SIZE = new haven.Coord(1, 2);
        ArrayList<Gob> availableRacks = new ArrayList<>();
        for (Gob rackGob : racks) {
            if (CheeseRackOverlayUtils.canAcceptTrays(rackGob)) {
                availableRacks.add(rackGob);
            } else {
                gui.msg("Skipping full rack (overlay check) - no space available");
            }
        }
        
        gui.msg("Found " + availableRacks.size() + " racks with space out of " + racks.size() + " total racks");
        
        for (Gob rackGob : availableRacks) {
            cheeseTrays = gui.getInventory().getItems(new NAlias("Cheese Tray"));
            if (cheeseTrays.isEmpty()) {
                break; // All cheese placed
            }
            
            Container rack = new Container(rackGob, "Rack");
            new PathFinder(rackGob).run(gui);
            new OpenTargetContainer(rack).run(gui);
            
            // Check available space in this rack (still needed for exact count)
            int availableSpace = gui.getInventory(rack.cap).getNumberFreeCoord(TRAY_SIZE);
            if (availableSpace > 0) {
                // Transfer trays to this rack and update orders immediately for each tray
                int traysToPlace = Math.min(availableSpace, cheeseTrays.size());
                for (int i = 0; i < traysToPlace; i++) {
                    WItem tray = cheeseTrays.get(i);
                    tray.item.wdgmsg("transfer", haven.Coord.z);
                    nurgling.NUtils.addTask(new nurgling.tasks.ISRemoved(tray.item.wdgid()));
                }
                
                // Update orders immediately after placing trays - this ensures each tray is recorded
                // Only update if we have the expected cheese type and source location
                if (traysToPlace > 0 && cheeseType != null && fromPlace != null) {
                    updateOrdersAfterCheeseMovement(gui, cheeseType, traysToPlace, fromPlace, destination);
                }
                
                gui.msg("Placed " + traysToPlace + " trays on rack in " + destination);
            }
            
            new CloseTargetContainer(rack).run(gui);
        }
        
        // If any cheese remains, use FreeInventory2 as fallback
        cheeseTrays = gui.getInventory().getItems(new NAlias("Cheese Tray"));
        if (!cheeseTrays.isEmpty()) {
            gui.msg("Warning: " + cheeseTrays.size() + " cheese trays couldn't fit in " + destination + " racks. Using FreeInventory2.");
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
                                                CheeseBranch.Place fromPlace, CheeseBranch.Place toPlace) {
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
            
            boolean orderUpdated = false;
            
            // Look for current stage step and reduce it
            for (CheeseOrder.StepStatus step : relevantOrder.getStatus()) {
                if (step.name.equals(cheeseType) && step.place.equals(fromPlace.toString())) {
                    step.left = Math.max(0, step.left - movedCount);
                    gui.msg("Updated order " + relevantOrder.getCheeseType() + ": reduced " + cheeseType + 
                           " at " + fromPlace + " by " + movedCount + " (now " + step.left + " left)");
                    orderUpdated = true;
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
                orderUpdated = true;
            } else {
                // Create new step for next stage using correct progression location
                nextStep = new CheeseOrder.StepStatus(nextCheeseType, nextCheesePlace.toString(), movedCount);
                relevantOrder.getStatus().add(nextStep);
                gui.msg("Created new step in order " + relevantOrder.getCheeseType() + ": " + nextCheeseType + 
                       " at " + nextCheesePlace + " with " + movedCount + " trays");
                orderUpdated = true;
            }
            
            if (orderUpdated) {
                ordersManager.addOrUpdateOrder(relevantOrder);
            }
            
            // Save updated orders
            ordersManager.writeOrders(null);
            
        } catch (Exception e) {
            gui.msg("Error updating orders after cheese movement: " + e.getMessage());
        }
    }
    
    /**
     * Find the specific order that contains a cheese progression
     * Only returns an order if it contains the full progression chain
     */
    private CheeseOrder findOrderForCheeseType(String currentCheeseType, String nextCheeseType) {
        for (CheeseOrder order : ordersManager.getOrders().values()) {
            // Check if this order's progression chain contains both cheese types
            boolean hasCurrentType = false;
            boolean hasNextType = false;
            
            for (CheeseOrder.StepStatus step : order.getStatus()) {
                if (step.name.equals(currentCheeseType)) {
                    hasCurrentType = true;
                }
                if (step.name.equals(nextCheeseType)) {
                    hasNextType = true;
                }
            }
            
            // Also check if the final product matches the progression
            List<CheeseBranch.Cheese> chain = CheeseBranch.getChainToProduct(order.getCheeseType());
            if (chain != null) {
                for (CheeseBranch.Cheese step : chain) {
                    if (step.name.equals(currentCheeseType) || step.name.equals(nextCheeseType)) {
                        return order; // Found order with matching progression
                    }
                }
            }
        }
        return null; // No matching order found
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
        return null; // No next step found (might be final product)
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
     * Determine the next cheese type in the progression chain for a specific target product
     * This ensures we get the correct progression chain when multiple chains share the same intermediate steps
     */
    private String getNextCheeseTypeInChain(String currentCheeseType, CheeseBranch.Place currentPlace, String targetProduct) {
        CheeseBranch.Cheese nextStep = getNextCheeseStepInChain(currentCheeseType, currentPlace, targetProduct);
        return nextStep != null ? nextStep.name : null;
    }
    
    /**
     * Legacy method for backward compatibility
     */
    private String getNextCheeseTypeInChain(String currentCheeseType, CheeseBranch.Place currentPlace) {
        for (CheeseBranch branch : CheeseBranch.branches) {
            for (int i = 0; i < branch.steps.size() - 1; i++) {
                CheeseBranch.Cheese currentStep = branch.steps.get(i);
                if (currentStep.name.equals(currentCheeseType) && currentStep.place == currentPlace) {
                    // Found the current step, return the next step's cheese type
                    CheeseBranch.Cheese nextStep = branch.steps.get(i + 1);
                    return nextStep.name;
                }
            }
        }
        return null; // No next step found (might be final product)
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
}