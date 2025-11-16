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
        
        // Determine how much work we can actually do in batches
        int totalWorkNeeded = Math.min(totalQuantity, currentStep.left);
        
        // Calculate available rack space for this cheese type
        int totalRackSpace = calculateAvailableRackSpace(chain, rackCapacity);

        if(totalRackSpace == 0) {
            return Results.SUCCESS();
        }

        totalWorkNeeded = Math.min(totalWorkNeeded, totalRackSpace);
        
        // Process in batches considering inventory and rack capacity limits
        int totalProcessed = 0;
        while (totalProcessed < totalWorkNeeded) {
            // Limit batch size by both inventory capacity AND available rack space
            int maxBatchSize = Math.min(inventoryCapacity, totalWorkNeeded - totalProcessed);
            maxBatchSize = Math.min(maxBatchSize, totalRackSpace);

            int actualProcessed = processBatch(gui, order, currentStep, chain, maxBatchSize);
            totalProcessed += actualProcessed;
            
            // If we couldn't process anything, break to avoid infinite loop
            if (actualProcessed == 0) {
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
            
            // Create trays with curds and get actual count created
            CreateTraysWithCurds createAction = new CreateTraysWithCurds(curdType, batchSize);
            createAction.run(gui);
            int actualCount = createAction.getLastTraysCreated();

            // Count total trays of this type in inventory (new + existing)
            int totalTraysInInventory = countTraysOfTypeInInventory(gui, curdType);
            
            // Handle tray placement for ALL trays of this type in inventory
            if (chain.size() > 1 && totalTraysInInventory > 0) {
                CheeseBranch.Cheese nextCheeseStep = chain.get(1);
                CheeseBranch.Place targetPlace = nextCheeseStep.place;
                
                rackManager.handleTrayPlacement(gui, targetPlace, totalTraysInInventory, curdType, ordersManager, order);
            }
            
            return actualCount;
        } else {
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
    private void updateOrderProgress(CheeseOrder.StepStatus step, int completed) {
        step.left -= completed;
    }
    
    /**
     * Advance completed trays to the next step in the production chain
     * Only advances the specific number of trays that were actually placed
     */
    private void advanceTraysToNextStep(CheeseOrder order, List<CheeseBranch.Cheese> chain, int traysPlaced) {
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
        }
        
        // Increase the next step count by the number of trays that were actually placed
        nextStepStatus.left += traysPlaced;
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