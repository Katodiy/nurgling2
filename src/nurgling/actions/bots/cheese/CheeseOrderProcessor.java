package nurgling.actions.bots.cheese;

import haven.WItem;
import nurgling.NGameUI;
import nurgling.actions.Results;
import nurgling.cheese.CheeseOrder;
import nurgling.cheese.CheeseOrdersManager;
import nurgling.cheese.CheeseBranch;
import nurgling.actions.CreateTraysWithCurds;
import nurgling.tools.NAlias;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles cheese order processing, status updates, and batch management
 */
public class CheeseOrderProcessor {
    private CheeseOrdersManager ordersManager;
    
    public CheeseOrderProcessor() {
        this.ordersManager = new CheeseOrdersManager();
    }
    
    /**
     * Analyze current orders to determine what work needs to be done
     */
    public Map<String, Integer> analyzeOrders() {
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
     * Process cheese order in batches
     */
    public void processCheeseOrderInBatches(NGameUI gui, String cheeseType, int totalQuantity,
                                           int inventoryCapacity, CheeseRackManager rackManager) throws InterruptedException {
        
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
        int totalWorkNeeded = Math.min(totalQuantity, currentStep.left);
        
        gui.msg("Need to process " + totalWorkNeeded + " trays, inventory can hold " + inventoryCapacity);
        
        // Process in batches to avoid inventory overflow
        int totalProcessed = 0;
        while (totalProcessed < totalWorkNeeded) {
            int batchSize = Math.min(inventoryCapacity, totalWorkNeeded - totalProcessed);
            int actualProcessed = processBatch(gui, order, currentStep, chain, batchSize, rackManager);
            totalProcessed += actualProcessed;

            gui.msg("Completed batch: " + actualProcessed + " trays (" + totalProcessed + "/" + totalWorkNeeded + " total)");
            
            // If we couldn't process anything, break to avoid infinite loop
            if (actualProcessed == 0) {
                gui.msg("Could not process any more trays - stopping batch processing");
                break;
            }
        }
        
        // Save updated orders
        ordersManager.writeOrders(null);
    }
    
    /**
     * Process a single batch of cheese production
     * @return actual number of trays processed
     */
    private int processBatch(NGameUI gui, CheeseOrder order, CheeseOrder.StepStatus currentStep,
                             List<CheeseBranch.Cheese> chain, int batchSize, 
                             CheeseRackManager rackManager) throws InterruptedException {
        
        if (currentStep.place.equals("start")) {
            // This is the curd creation step
            String curdType = currentStep.name; // e.g., "Sheep's Curd"
            
            gui.msg("Creating batch of " + batchSize + " trays with " + curdType);
            
            // Create trays with curds and get actual count created
            Results result = new CreateTraysWithCurds(curdType, batchSize).run(gui);
            int actualCount = result.hasPayload() ? (Integer) result.getPayload() : 0;
            
            gui.msg("Created " + actualCount + " trays (requested " + batchSize + ")");

            int totalTraysInInventory = countTraysOfTypeInInventory(gui, curdType);
            
            gui.msg("Total " + curdType + " trays in inventory: " + totalTraysInInventory + 
                   " (created " + actualCount + " new)");
            
            // Update the order status with actual count created
            updateOrderProgress(gui, order, currentStep, actualCount);
            
            // Advance all trays of this type to next step (including existing ones)
            advanceTraysToNextStep(gui, order, chain, actualCount);
            
            // Handle tray placement for ALL trays of this type in inventory
            int traysPlaced = 0;
            if (chain.size() > 1 && totalTraysInInventory > 0) {
                CheeseBranch.Cheese nextCheeseStep = chain.get(1);
                CheeseBranch.Place targetPlace = nextCheeseStep.place;
                
                traysPlaced = rackManager.handleTrayPlacement(gui, targetPlace, totalTraysInInventory, curdType);
                
                // If trays were placed, we need to reduce the current step count
                if (traysPlaced > 0) {
                    gui.msg("Placed " + traysPlaced + " trays on racks, reducing current step count");
                    currentStep.left -= traysPlaced;
                    gui.msg("Current step " + currentStep.name + " now has " + currentStep.left + " remaining");
                }
            }
            
            return actualCount;
        } else {
            // This is a cheese aging/movement step
//            gui.msg("Moving batch of " + batchSize + " " + currentStep.name + " trays");
//            // TODO: Implement moving existing cheese between areas
//            updateOrderProgress(gui, order, currentStep, batchSize);
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
     * This accounts for both newly created trays AND existing trays in inventory
     */
    private void advanceTraysToNextStep(NGameUI gui, CheeseOrder order, List<CheeseBranch.Cheese> chain, int completedCount) throws InterruptedException {
        if (chain.size() <= 1) return; // No next step
        
        // Find the current and next steps in the chain
        CheeseBranch.Cheese currentCheeseStep = chain.get(0); // Current step (e.g., "Sheep's Curd")
        CheeseBranch.Cheese nextCheeseStep = chain.get(1); // Next step after start (e.g., "Abbaye")
        
        // Count how many trays of the CURRENT type are actually in inventory
        // These are the trays that are ready to be placed and will age into the next step
        int traysInInventory = countTraysOfTypeInInventory(gui, currentCheeseStep.name);
        
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
                0 // Start with 0, will be set to actual inventory count below
            );
            order.getStatus().add(nextStepStatus);
            gui.msg("Created next step: " + nextStepStatus.name + " at " + nextStepStatus.place);
        }
        
        // Set the next step count to actual inventory count of CURRENT step trays ready to age
        nextStepStatus.left = traysInInventory;
        gui.msg("Next step " + nextStepStatus.name + " now has " + traysInInventory + 
                " " + currentCheeseStep.name + " trays ready to age (includes " + completedCount + " newly created)");
    }
    
    /**
     * Count how many cheese trays of a specific type are in inventory
     */
    private int countTraysOfTypeInInventory(NGameUI gui, String cheeseType) throws InterruptedException {
        ArrayList<WItem> allTrays = gui.getInventory().getItems(new NAlias("Cheese Tray"));
        int count = 0;
        
        for (WItem tray : allTrays) {
            String contentName = CheeseTrayUtils.getContentName(tray);
            if (cheeseType.equals(contentName)) {
                count++;
            }
        }
        
        return count;
    }
}