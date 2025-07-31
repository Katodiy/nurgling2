package nurgling.actions.bots;

import haven.Coord;
import haven.Gob;
import haven.WItem;
import nurgling.NGameUI;
import nurgling.actions.*;
import nurgling.areas.NArea;
import nurgling.areas.NContext;
import nurgling.cheese.CheeseBranch;
import nurgling.cheese.CheeseOrder;
import nurgling.cheese.CheeseOrdersManager;
import nurgling.tools.Container;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.widgets.Specialisation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Main cheese production bot that processes orders and manages cheese production workflow
 */
public class CheeseProductionBot implements Action {
    
    private final Coord TRAY_SIZE = new Coord(1, 2);
    private CheeseOrdersManager ordersManager;
    private NContext context;
    
    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        context = new NContext(gui);
        ordersManager = new CheeseOrdersManager();
        
        gui.msg("=== Starting Cheese Production Bot ===");
        
        // 1. Analyze current orders and determine what needs to be done
        Map<String, Integer> workNeeded = analyzeOrders();
        if (workNeeded.isEmpty()) {
            gui.msg("No work needed - all orders complete!");
            return Results.SUCCESS();
        }
        
        // 2. Phase 1: Clear all ready cheese from racks to buffer zones
        gui.msg("=== Phase 1: Clearing ready cheese from racks ===");
        clearReadyCheeseFromAllRacks(gui);
        
        // 3. Phase 2: Process cheese from buffer zones  
        gui.msg("=== Phase 2: Processing cheese from buffer zones ===");
        processCheeseFromBufferZones(gui);
        
        // 4. Phase 3: Check available rack capacity after clearing
        Map<CheeseBranch.Place, Integer> rackCapacity = checkRackCapacity(gui);
        
        // 5. Phase 4: Create new cheese trays if needed and space available
        gui.msg("=== Phase 3: Creating new cheese trays ===");
        for (Map.Entry<String, Integer> work : workNeeded.entrySet()) {
            String cheeseType = work.getKey();
            int quantity = work.getValue();
            
            gui.msg("Processing " + quantity + " " + cheeseType + " cheese");
            processCheeseOrder(gui, cheeseType, quantity, rackCapacity);
        }
        
        gui.msg("=== Cheese Production Bot Complete ===");
        return Results.SUCCESS();
    }
    
    /**
     * Analyze current orders to determine what work needs to be done
     */
    private Map<String, Integer> analyzeOrders() {
        Map<String, Integer> workNeeded = new HashMap<>();
        
        for (CheeseOrder order : ordersManager.getOrders().values()) {
            String cheeseType = order.getCheeseType();
            
            // Get the production chain for this cheese
            List<CheeseBranch.Cheese> chain = CheeseBranch.getChainToProduct(cheeseType);
            if (chain == null) {
                System.err.println("Unknown cheese type: " + cheeseType);
                continue;
            }
            
            // For now, focus on the first step (creating curds)
            for (CheeseOrder.StepStatus status : order.getStatus()) {
                if (status.left > 0) {
                    workNeeded.put(cheeseType, status.left);
                    break; // Focus on first incomplete step
                }
            }
        }
        
        return workNeeded;
    }
    
    /**
     * Check available capacity in cheese racks across all areas
     */
    private Map<CheeseBranch.Place, Integer> checkRackCapacity(NGameUI gui) throws InterruptedException {
        Map<CheeseBranch.Place, Integer> capacity = new HashMap<>();
        
        for (CheeseBranch.Place place : CheeseBranch.Place.values()) {
            if (place == CheeseBranch.Place.start) continue; // Skip start placeholder
            
            try {
                NArea area = context.getSpecArea(Specialisation.SpecName.cheeseRacks, place.toString());
                if (area != null) {
                    int rackSpace = calculateRackSpace(gui, area);
                    capacity.put(place, rackSpace);
                    gui.msg(place + " cheese racks can fit " + rackSpace + " more trays");
                }
            } catch (Exception e) {
                gui.msg("Could not access " + place + " cheese racks: " + e.getMessage());
                capacity.put(place, 0);
            }
        }
        
        return capacity;
    }
    
    /**
     * Calculate available space in cheese racks in an area
     */
    private int calculateRackSpace(NGameUI gui, NArea area) throws InterruptedException {
        ArrayList<Gob> racks = Finder.findGobs(area, new NAlias("gfx/terobjs/cheeserack"));
        int totalSpace = 0;
        
        for (Gob rack : racks) {
            Container rackContainer = new Container(rack, "Cheese Rack");
            new PathFinder(rack).run(gui);
            new OpenTargetContainer(rackContainer).run(gui);
            
            int freeSpace = gui.getInventory(rackContainer.cap).getNumberFreeCoord(TRAY_SIZE);
            totalSpace += freeSpace;
            
            new CloseTargetContainer(rackContainer).run(gui);
        }
        
        return totalSpace;
    }
    
    /**
     * Process a specific cheese order and update order status
     */
    private void processCheeseOrder(NGameUI gui, String cheeseType, int quantity, 
                                   Map<CheeseBranch.Place, Integer> rackCapacity) throws InterruptedException {
        
        List<CheeseBranch.Cheese> chain = CheeseBranch.getChainToProduct(cheeseType);
        if (chain == null) return;
        
        // Find the current order
        CheeseOrder order = findOrderByType(cheeseType);
        if (order == null) return;
        
        // Find the current step that needs work
        CheeseOrder.StepStatus currentStep = getCurrentStep(order);
        if (currentStep == null) {
            gui.msg("No work needed for " + cheeseType);
            return;
        }
        
        gui.msg("Processing step: " + currentStep.name + " (" + currentStep.left + " remaining)");
        
        // Determine how much work we can actually do in batches
        int totalWorkNeeded = Math.min(quantity, currentStep.left);
        int inventoryCapacity = getInventoryCapacity(gui);
        
        gui.msg("Need to process " + totalWorkNeeded + " trays, inventory can hold " + inventoryCapacity);
        
        // Process in batches to avoid inventory overflow
        int totalProcessed = 0;
        while (totalProcessed < totalWorkNeeded) {
            int batchSize = Math.min(inventoryCapacity, totalWorkNeeded - totalProcessed);
            processBatch(gui, order, currentStep, chain, batchSize, rackCapacity);
            totalProcessed += batchSize;
            
            gui.msg("Completed batch of " + batchSize + " trays (" + totalProcessed + "/" + totalWorkNeeded + " total)");
        }
        
        // Save updated orders
        ordersManager.writeOrders(null);
    }
    
    /**
     * Get available inventory capacity for cheese trays
     */
    private int getInventoryCapacity(NGameUI gui) throws InterruptedException {
        // Calculate how many cheese trays can fit in current inventory
        return gui.getInventory().getNumberFreeCoord(TRAY_SIZE);
    }
    
    /**
     * Process a single batch of cheese production
     */
    private void processBatch(NGameUI gui, CheeseOrder order, CheeseOrder.StepStatus currentStep, 
                             List<CheeseBranch.Cheese> chain, int batchSize, 
                             Map<CheeseBranch.Place, Integer> rackCapacity) throws InterruptedException {
        
        if (currentStep.place.equals("start")) {
            // This is the curd creation step
            String curdType = currentStep.name; // e.g., "Sheep's Curd"
            
            gui.msg("Creating batch of " + batchSize + " trays with " + curdType);
            
            // Create trays with curds
            new CreateTraysWithCurds(curdType, batchSize).run(gui);
            
            // Update the order status and advance completed trays to next step
            updateOrderProgress(gui, order, currentStep, batchSize);
            advanceTraysToNextStep(gui, order, chain, batchSize);
            
            // Determine where the trays should go for aging
            if (chain.size() > 1) {
                CheeseBranch.Cheese nextCheeseStep = chain.get(1);
                CheeseBranch.Place targetPlace = nextCheeseStep.place;
                
                // Check if there's space in the target area's racks
                if (rackCapacity.getOrDefault(targetPlace, 0) >= batchSize) {
                    gui.msg("Moving batch to " + targetPlace + " racks");
                    moveTraysToArea(gui, targetPlace, batchSize);
                    // Reduce available rack capacity
                    rackCapacity.put(targetPlace, rackCapacity.get(targetPlace) - batchSize);
                } else {
                    gui.msg("Not enough space in " + targetPlace + " racks, sending to buffer zones");
                    // Send filled trays to buffer zones using area matcher
                    new CheeseAreaMatcher.TransferCheeseTraysToCorrectAreas().run(gui);
                }
            }
        } else {
            // This is a cheese aging/movement step
            gui.msg("Moving batch of " + batchSize + " " + currentStep.name + " trays");
            // TODO: Implement moving existing cheese between areas
            updateOrderProgress(gui, order, currentStep, batchSize);
        }
    }
    
    /**
     * Find order by cheese type
     */
    private CheeseOrder findOrderByType(String cheeseType) {
        for (CheeseOrder order : ordersManager.getOrders().values()) {
            if (order.getCheeseType().equals(cheeseType)) {
                return order;
            }
        }
        return null;
    }
    
    /**
     * Get the current step that needs work
     */
    private CheeseOrder.StepStatus getCurrentStep(CheeseOrder order) {
        for (CheeseOrder.StepStatus step : order.getStatus()) {
            if (step.left > 0) {
                return step;
            }
        }
        return null;
    }
    
    /**
     * Update order progress by reducing the "left" count
     */
    private void updateOrderProgress(NGameUI gui, CheeseOrder order, CheeseOrder.StepStatus step, int completed) {
        step.left -= completed;
        gui.msg("Updated " + order.getCheeseType() + " - " + step.name + ": " + step.left + " remaining");
    }
    
    /**
     * Advance completed trays to the next step in the production chain
     */
    private void advanceTraysToNextStep(NGameUI gui, CheeseOrder order, List<CheeseBranch.Cheese> chain, int completedCount) {
        if (chain.size() <= 1) return; // No next step
        
        // Find the next step in the chain
        CheeseBranch.Cheese nextCheeseStep = chain.get(1); // Next step after start
        
        // Look for existing status entry for this step
        CheeseOrder.StepStatus nextStepStatus = null;
        for (CheeseOrder.StepStatus status : order.getStatus()) {
            if (status.name.equals(nextCheeseStep.name) && 
                status.place.equals(nextCheeseStep.place.toString())) {
                nextStepStatus = status;
                break;
            }
        }
        
        // Create new status entry if it doesn't exist
        if (nextStepStatus == null) {
            nextStepStatus = new CheeseOrder.StepStatus(
                nextCheeseStep.name,
                nextCheeseStep.place.toString(),
                0 // Start with 0, will add completed count below
            );
            order.getStatus().add(nextStepStatus);
            gui.msg("Created next step: " + nextStepStatus.name + " at " + nextStepStatus.place);
        }
        
        // Add the completed trays to the next step
        nextStepStatus.left += completedCount;
        gui.msg("Advanced " + completedCount + " trays to " + nextStepStatus.name + 
                " (now " + nextStepStatus.left + " ready)");
    }
    
    /**
     * Move cheese trays to a specific area's racks
     */
    private void moveTraysToArea(NGameUI gui, CheeseBranch.Place targetPlace, int quantity) throws InterruptedException {
        try {
            NArea targetArea = context.getSpecArea(Specialisation.SpecName.cheeseRacks, targetPlace.toString());
            if (targetArea == null) {
                gui.msg("Target area not found: " + targetPlace);
                return;
            }
            
            // Find filled cheese trays in inventory
            ArrayList<WItem> filledTrays = gui.getInventory().getItems(new NAlias("Cheese Tray"));
            int moved = 0;
            
            for (WItem tray : filledTrays) {
                if (moved >= quantity) break;
                
                // Check if this is a filled tray (contains curd)
                if (tray.item.res.get().name.contains("curd")) {
                    // Move to target area racks
                    moveToRacks(gui, tray, targetArea);
                    moved++;
                }
            }
            
            gui.msg("Moved " + moved + " trays to " + targetPlace + " area");
            
        } catch (Exception e) {
            gui.msg("Error moving trays to " + targetPlace + ": " + e.getMessage());
        }
    }
    
    /**
     * Move a tray to racks in a specific area
     */
    private void moveToRacks(NGameUI gui, WItem tray, NArea area) throws InterruptedException {
        ArrayList<Gob> racks = Finder.findGobs(area, new NAlias("gfx/terobjs/cheeserack"));
        
        for (Gob rack : racks) {
            Container rackContainer = new Container(rack, "Cheese Rack");
            new PathFinder(rack).run(gui);
            new OpenTargetContainer(rackContainer).run(gui);
            
            // Check if there's space
            if (gui.getInventory(rackContainer.cap).getNumberFreeCoord(TRAY_SIZE) > 0) {
                // Transfer the tray
                new TransferToContainer(rackContainer, new NAlias("Cheese Tray"), 1).run(gui);
                new CloseTargetContainer(rackContainer).run(gui);
                return; // Successfully placed
            }
            
            new CloseTargetContainer(rackContainer).run(gui);
        }
        
        gui.msg("No space found in racks for tray");
    }
    
    /**
     * Phase 1: Clear all ready cheese from racks to buffer zones
     */
    private void clearReadyCheeseFromAllRacks(NGameUI gui) throws InterruptedException {
        CheeseBranch.Place[] places = {
                CheeseBranch.Place.outside,
                CheeseBranch.Place.inside,
                CheeseBranch.Place.mine,
                CheeseBranch.Place.cellar
        };
        
        for (CheeseBranch.Place place : places) {
            gui.msg("Clearing ready cheese from " + place + " racks");
            clearReadyCheeseFromArea(gui, place);
        }
    }
    
    /**
     * Clear ready cheese from a specific area's racks to its buffer zone
     */
    private void clearReadyCheeseFromArea(NGameUI gui, CheeseBranch.Place place) throws InterruptedException {
        try {
            // Get the cheese racks area
            NArea rackArea = context.getSpecArea(Specialisation.SpecName.cheeseRacks, place.toString());
            if (rackArea == null) {
                gui.msg("No " + place + " cheese racks area configured");
                return;
            }
            
            // Get all racks in this area
            ArrayList<Gob> racks = Finder.findGobs(rackArea, new NAlias("gfx/terobjs/cheeserack"));
            
            for (Gob rack : racks) {
                Container rackContainer = new Container(rack, "Cheese Rack");
                new PathFinder(rack).run(gui);
                new OpenTargetContainer(rackContainer).run(gui);
                
                // Check each tray in the rack
                ArrayList<WItem> traysInRack = gui.getInventory(rackContainer.cap).getItems(new NAlias("Cheese Tray"));
                
                for (WItem tray : traysInRack) {
                    if (isCheeseReadyToMove(tray, place)) {
                        // Move this tray to buffer zone
                        moveToBufferZone(gui, tray, place);
                    }
                }
                
                new CloseTargetContainer(rackContainer).run(gui);
            }
            
        } catch (Exception e) {
            gui.msg("Error clearing " + place + " area: " + e.getMessage());
        }
    }
    
    /**
     * Check if a cheese tray is ready to move to next stage
     */
    private boolean isCheeseReadyToMove(WItem tray, CheeseBranch.Place currentPlace) {
        // TODO: Implement logic to check if cheese has aged long enough
        // For now, assume all cheese is ready to move (this needs refinement)
        
        String resourcePath = tray.item.res.get().name;
        
        // Find what cheese this is and what stage it should be at
        for (CheeseBranch branch : CheeseBranch.branches) {
            for (int i = 0; i < branch.steps.size(); i++) {
                CheeseBranch.Cheese step = branch.steps.get(i);
                if (step.place == currentPlace && resourceMatches(resourcePath, step.name)) {
                    // This cheese is in the right place, check if it should move to next stage
                    return i < branch.steps.size() - 1; // Not the final product yet
                }
            }
        }
        
        return false; // Don't move if we're not sure
    }
    
    /**
     * Check if resource path matches cheese name (simplified)
     */
    private boolean resourceMatches(String resourcePath, String cheeseName) {
        // This is a simplified check - you might need more sophisticated matching
        return resourcePath.toLowerCase().contains(cheeseName.toLowerCase().replace(" ", "").replace("'", ""));
    }
    
    /**
     * Move a tray to the buffer containers in the same area
     */
    private void moveToBufferZone(NGameUI gui, WItem tray, CheeseBranch.Place place) throws InterruptedException {
        try {
            // Get the cheese racks area (same area, but look for containers instead of racks)
            NArea rackArea = context.getSpecArea(Specialisation.SpecName.cheeseRacks, place.toString());
            if (rackArea == null) return;
            
            // Find containers in this area to use as buffer storage
            ArrayList<Gob> containers = Finder.findGobs(rackArea, new NAlias(new ArrayList<String>(NContext.contcaps.keySet()), new ArrayList<>()));
            
            for (Gob container : containers) {
                Container bufferContainer = new Container(container, NContext.contcaps.get(container.ngob.name));
                new PathFinder(container).run(gui);
                new OpenTargetContainer(bufferContainer).run(gui);
                
                // Check if there's space in this container
                if (bufferContainer.getattr(Container.Space.class) != null && 
                    bufferContainer.getattr(Container.Space.class).getFreeSpace() > 0) {
                    
                    // Transfer the tray to buffer container
                    new TransferToContainer(bufferContainer, new NAlias("Cheese Tray"), 1).run(gui);
                    new CloseTargetContainer(bufferContainer).run(gui);
                    
                    gui.msg("Moved " + tray.item.res.get().name + " to buffer container in " + place + " area");
                    return;
                }
                
                new CloseTargetContainer(bufferContainer).run(gui);
            }
            
            gui.msg("No space in buffer containers for " + place + " area");
            
        } catch (Exception e) {
            gui.msg("Error moving to buffer in " + place + ": " + e.getMessage());
        }
    }
    
    /**
     * Phase 2: Process cheese from all buffer zones
     */
    private void processCheeseFromBufferZones(NGameUI gui) throws InterruptedException {
        CheeseBranch.Place[] places = {
                CheeseBranch.Place.outside,
                CheeseBranch.Place.inside,
                CheeseBranch.Place.mine,
                CheeseBranch.Place.cellar
        };
        
        for (CheeseBranch.Place place : places) {
            gui.msg("Processing cheese from " + place + " buffer zone");
            processBufferZone(gui, place);
        }
    }
    
    /**
     * Process cheese from a specific buffer zone
     */
    private void processBufferZone(NGameUI gui, CheeseBranch.Place place) throws InterruptedException {
        // TODO: Get buffer zone area for this place
        // For now, assuming we have cheese trays to process
        
        // Find cheese trays in this buffer zone
        ArrayList<WItem> bufferTrays = getTraysFromBufferZone(gui, place);
        
        for (WItem tray : bufferTrays) {
            String resourcePath = tray.item.res.get().name;
            
            // Check if this cheese is ready to slice (final product)
            if (isCheeseReadyToSlice(tray)) {
                gui.msg("Slicing " + resourcePath + " - final product!");
                sliceCheeseAndReturnEmptyTray(gui, tray);
            } 
            // Check if cheese should move to next stage
            else if (shouldMoveToNextStage(tray, place)) {
                CheeseBranch.Place nextPlace = getNextStageLocation(tray, place);
                if (nextPlace != null) {
                    gui.msg("Moving " + resourcePath + " from " + place + " to " + nextPlace);
                    moveCheeseToNextStage(gui, tray, nextPlace);
                }
            }
        }
    }
    
    /**
     * Check if cheese is ready to slice (final product in chain)
     */
    private boolean isCheeseReadyToSlice(WItem tray) {
        String resourcePath = tray.item.res.get().name;
        
        // Find this cheese in the production chains
        for (CheeseBranch branch : CheeseBranch.branches) {
            for (int i = 0; i < branch.steps.size(); i++) {
                CheeseBranch.Cheese step = branch.steps.get(i);
                if (resourceMatches(resourcePath, step.name)) {
                    // This is the final step if it's the last in the chain
                    return i == branch.steps.size() - 1;
                }
            }
        }
        return false;
    }
    
    /**
     * Slice cheese and return empty tray to empty tray area
     */
    private void sliceCheeseAndReturnEmptyTray(NGameUI gui, WItem tray) throws InterruptedException {
        // TODO: Implement actual slicing action
        // This would involve:
        // 1. Right-click tray and select "Slice" option
        // 2. Wait for slicing to complete
        // 3. Collect sliced cheese to appropriate storage
        // 4. Handle the now-empty tray
        
        gui.msg("Slicing cheese from tray...");
        
        // After slicing, the tray becomes empty and needs to go back to empty tray area
        returnEmptyTrayToStorage(gui, tray);
    }
    
    /**
     * Return empty tray to the empty cheese tray storage area
     */
    private void returnEmptyTrayToStorage(NGameUI gui, WItem emptyTray) throws InterruptedException {
        gui.msg("Returning empty tray to storage area");
        
        // Find the area configured for empty cheese trays
        try {
            NContext returnContext = new NContext(gui);
            if (returnContext.addOutItem("Empty Cheese Tray", null, 1.0)) {
                // Use the custom area matcher to send empty tray to correct area
                new CheeseAreaMatcher.TransferCheeseTraysToCorrectAreas().run(gui);
            } else if (returnContext.addOutItem("Cheese Tray", null, 1.0)) {
                // Fallback to generic cheese tray area
                new CheeseAreaMatcher.TransferCheeseTraysToCorrectAreas().run(gui);
            } else {
                gui.msg("No empty cheese tray area configured - keeping in inventory");
            }
        } catch (Exception e) {
            gui.msg("Error returning empty tray: " + e.getMessage());
        }
    }
    
    /**
     * Get cheese trays from buffer containers in the area
     */
    private ArrayList<WItem> getTraysFromBufferZone(NGameUI gui, CheeseBranch.Place place) throws InterruptedException {
        ArrayList<WItem> bufferTrays = new ArrayList<>();
        
        try {
            // Get the cheese racks area
            NArea rackArea = context.getSpecArea(Specialisation.SpecName.cheeseRacks, place.toString());
            if (rackArea == null) return bufferTrays;
            
            // Find containers in this area (buffer containers)
            ArrayList<Gob> containers = Finder.findGobs(rackArea, new NAlias(new ArrayList<String>(NContext.contcaps.keySet()), new ArrayList<>()));
            
            for (Gob container : containers) {
                Container bufferContainer = new Container(container, NContext.contcaps.get(container.ngob.name));
                new PathFinder(container).run(gui);
                new OpenTargetContainer(bufferContainer).run(gui);
                
                // Get all cheese trays from this buffer container
                ArrayList<WItem> containerTrays = gui.getInventory(bufferContainer.cap).getItems(new NAlias("Cheese Tray"));
                bufferTrays.addAll(containerTrays);
                
                new CloseTargetContainer(bufferContainer).run(gui);
            }
            
        } catch (Exception e) {
            gui.msg("Error getting trays from " + place + " buffer containers: " + e.getMessage());
        }
        
        return bufferTrays;
    }
    
    /**
     * Check if cheese should move to next stage
     */
    private boolean shouldMoveToNextStage(WItem tray, CheeseBranch.Place currentPlace) {
        // If it's not ready to slice, and it's aged enough, it should move
        return !isCheeseReadyToSlice(tray) && isCheeseReadyToMove(tray, currentPlace);
    }
    
    /**
     * Get the next stage location for a cheese
     */
    private CheeseBranch.Place getNextStageLocation(WItem tray, CheeseBranch.Place currentPlace) {
        String resourcePath = tray.item.res.get().name;
        
        for (CheeseBranch branch : CheeseBranch.branches) {
            for (int i = 0; i < branch.steps.size() - 1; i++) { // -1 because we want next step
                CheeseBranch.Cheese step = branch.steps.get(i);
                if (step.place == currentPlace && resourceMatches(resourcePath, step.name)) {
                    // Return the next step's location
                    return branch.steps.get(i + 1).place;
                }
            }
        }
        return null;
    }
    
    /**
     * Move cheese to next stage area
     */
    private void moveCheeseToNextStage(NGameUI gui, WItem tray, CheeseBranch.Place nextPlace) throws InterruptedException {
        // Move the tray from buffer zone to the next area's racks
        moveTraysToArea(gui, nextPlace, 1);
    }
}