package nurgling.actions;

import haven.WItem;
import nurgling.NGameUI;
import nurgling.cheese.CheeseOrder;
import nurgling.cheese.CheeseOrdersManager;
import nurgling.cheese.CheeseBranch;
import nurgling.actions.bots.cheese.CheeseRackManager;
import nurgling.actions.bots.cheese.CheeseUtils;
import nurgling.actions.bots.cheese.CheeseInventoryOperations;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Processes a specific cheese order in batches, handling batch sizing and order tracking
 */
public class ProcessCheeseOrderInBatches implements Action {
    private final String cheeseType;
    private final int totalQuantity;
    private final int inventoryCapacity;
    private final CheeseRackManager rackManager;
    private final Map<CheeseBranch.Place, Integer> rackCapacity;
    private final CheeseOrdersManager ordersManager;
    
    public ProcessCheeseOrderInBatches(String cheeseType, int totalQuantity, int inventoryCapacity, 
                                      CheeseRackManager rackManager, Map<CheeseBranch.Place, Integer> rackCapacity,
                                      CheeseOrdersManager ordersManager) {
        this.cheeseType = cheeseType;
        this.totalQuantity = totalQuantity;
        this.inventoryCapacity = inventoryCapacity;
        this.rackManager = rackManager;
        this.rackCapacity = rackCapacity;
        this.ordersManager = ordersManager;
    }
    
    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        List<CheeseBranch.Cheese> chain = CheeseBranch.getChainToProduct(cheeseType);
        if (chain == null) return Results.ERROR("Unknown cheese type: " + cheeseType);
        
        // Find the current order
        CheeseOrder order = findOrderByType(cheeseType);
        if (order == null) return Results.ERROR("No order found for " + cheeseType);
        
        // Find the current step that needs work
        CheeseOrder.StepStatus currentStep = getCurrentStep(order);
        if (currentStep == null) {
            gui.msg("No work needed for " + cheeseType);
            return Results.SUCCESS();
        }
        
        gui.msg("Processing step: " + currentStep.name + " (" + currentStep.left + " remaining)");
        
        // Determine how much work we can actually do in batches
        int totalWorkNeeded = Math.min(totalQuantity, currentStep.left);
        
        // Calculate available rack space for this cheese type
        int totalRackSpace = calculateAvailableRackSpace(chain, rackCapacity);
        
        gui.msg("Need to process " + totalWorkNeeded + " trays");
        gui.msg("Inventory can hold " + inventoryCapacity + " trays");
        gui.msg("Available rack space: " + totalRackSpace + " trays");

        if(totalRackSpace == 0) {
            gui.msg("No cheese can fit on racks, continuing to next order.");
            return Results.SUCCESS();
        }
        
        // Process in batches considering inventory and rack capacity limits
        int totalProcessed = 0;
        while (totalProcessed < totalWorkNeeded) {
            // Limit batch size by both inventory capacity AND available rack space
            int maxBatchSize = Math.min(inventoryCapacity, totalWorkNeeded - totalProcessed);
            if (totalRackSpace > 0) {
                maxBatchSize = Math.min(maxBatchSize, totalRackSpace);
                gui.msg("Batch size limited by rack capacity: " + maxBatchSize);
            }
            
            int actualProcessed = processBatch(gui, order, currentStep, chain, maxBatchSize);
            totalProcessed += actualProcessed;

            gui.msg("Completed batch: " + actualProcessed + " trays (" + totalProcessed + "/" + totalWorkNeeded + " total)");
            
            // If we couldn't process anything, break to avoid infinite loop
            if (actualProcessed == 0) {
                gui.msg("Could not process any more trays - stopping batch processing");
                break;
            }
        }
        
        // Save updated orders
        ordersManager.writeOrders();
        return Results.SUCCESS();
    }
    
    /**
     * Process a single batch of cheese production
     * @return actual number of trays processed
     */
    private int processBatch(NGameUI gui, CheeseOrder order, CheeseOrder.StepStatus currentStep,
                             List<CheeseBranch.Cheese> chain, int batchSize) throws InterruptedException {
        
        if (currentStep.place.equals("start")) {
            // This is the curd creation step
            String curdType = currentStep.name; // e.g., "Sheep's Curd"
            
            gui.msg("Creating batch of " + batchSize + " trays with " + curdType);
            
            // Create trays with curds and get actual count created
            CreateTraysWithCurds createAction = new CreateTraysWithCurds(curdType, batchSize);
            createAction.run(gui);
            int actualCount = createAction.getLastTraysCreated();
            
            gui.msg("Created " + actualCount + " trays (requested " + batchSize + ")");

            // Count total trays of this type in inventory (new + existing)
            int totalTraysInInventory = countTraysOfTypeInInventory(gui, curdType);
            
            gui.msg("Total " + curdType + " trays in inventory: " + totalTraysInInventory + 
                   " (created " + actualCount + " new)");
            
            // Handle tray placement for ALL trays of this type in inventory
            int traysPlaced;
            if (chain.size() > 1 && totalTraysInInventory > 0) {
                CheeseBranch.Cheese nextCheeseStep = chain.get(1);
                CheeseBranch.Place targetPlace = nextCheeseStep.place;
                
                traysPlaced = rackManager.handleTrayPlacement(gui, targetPlace, totalTraysInInventory, curdType);
                
                // Update orders only after successful placement
                if (traysPlaced > 0) {
                    gui.msg("Placed " + traysPlaced + " trays on racks, updating order progress");
                    
                    // Reduce current step by number of trays actually placed
                    updateOrderProgress(gui, order, currentStep, traysPlaced);
                    
                    // Advance placed trays to next step
                    advanceTraysToNextStep(gui, order, chain, traysPlaced);
                }
            }
            
            return actualCount;
        } else {
            // This is a cheese aging/movement step
            // TODO: Implement moving existing cheese between areas
            return 0; // No work done yet for aging steps
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
     * Only advances the specific number of trays that were actually placed
     */
    private void advanceTraysToNextStep(NGameUI gui, CheeseOrder order, List<CheeseBranch.Cheese> chain, int traysPlaced) {
        if (chain.size() <= 1) return; // No next step
        
        // Find the current and next steps in the chain
        CheeseBranch.Cheese nextCheeseStep = chain.get(1); // Next step after start (e.g., "Abbaye")
        
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
                0 // Start with 0, will be increased by traysPlaced below
            );
            order.getStatus().add(nextStepStatus);
            gui.msg("Created next step: " + nextStepStatus.name + " at " + nextStepStatus.place);
        }
        
        // Increase the next step count by the number of trays that were actually placed
        nextStepStatus.left += traysPlaced;
        gui.msg("Advanced " + traysPlaced + " trays to next step: " + nextStepStatus.name + 
                " at " + nextStepStatus.place + " (now " + nextStepStatus.left + " total)");
    }
    
    /**
     * Count how many cheese trays of a specific type are in inventory
     */
    private int countTraysOfTypeInInventory(NGameUI gui, String cheeseType) throws InterruptedException {
        ArrayList<WItem> allTrays = CheeseInventoryOperations.getCheeseTrays(gui);
        int count = 0;
        
        for (WItem tray : allTrays) {
            String contentName = CheeseUtils.getContentName(tray);
            if (cheeseType.equals(contentName)) {
                count++;
            }
        }
        
        return count;
    }
    
    /**
     * Calculate available rack space for a cheese production chain
     */
    private int calculateAvailableRackSpace(List<CheeseBranch.Cheese> chain, Map<CheeseBranch.Place, Integer> rackCapacity) {
        if (chain.size() <= 1) return 0; // No next step means no rack placement needed
        
        // Get the target area for the next step in the chain
        CheeseBranch.Place targetPlace = chain.get(1).place;
        
        return rackCapacity.getOrDefault(targetPlace, 0);
    }
}