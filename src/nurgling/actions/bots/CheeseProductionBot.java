package nurgling.actions.bots;

import nurgling.NGameUI;
import nurgling.actions.Action;
import nurgling.actions.Results;
import nurgling.actions.ClearRacksAndRecordCapacity;
import nurgling.actions.ProcessCheeseFromBufferContainers;
import nurgling.actions.ProcessCheeseOrderInBatches;
import nurgling.actions.bots.cheese.*;
import nurgling.cheese.CheeseBranch;

import java.util.Map;

/**
 * This orchestrates the high-level workflow and delegates specific tasks
 */
public class CheeseProductionBot implements Action {
    
    private CheeseOrderProcessor orderProcessor;
    private CheeseRackManager rackManager;
    
    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        rackManager = new CheeseRackManager();
        orderProcessor = new CheeseOrderProcessor();
        
        gui.msg("=== Starting Cheese Production Bot ===");
        
        // 1. Analyze current orders and determine what needs to be done
        Map<String, Integer> workNeeded = orderProcessor.analyzeOrders(gui);
        if (workNeeded.isEmpty()) {
            gui.msg("No work needed - all orders complete!");
            return Results.SUCCESS();
        }
        
        // 2. Pass 1: Clear all racks into buffer and record capacity
        gui.msg("=== Pass 1: Clearing all racks and recording capacity ===");
        ClearRacksAndRecordCapacity clearAction = new ClearRacksAndRecordCapacity();
        Results clearResult = clearAction.run(gui);
        if (!clearResult.IsSuccess()) {
            gui.error("Failed to clear racks and record capacity");
        }

        Map<CheeseBranch.Place, Integer> rackCapacity = clearAction.getLastRecordedCapacity();

        // 3. Pass 2: Process buffers + create new cheese trays
        gui.msg("=== Pass 2: Processing buffers and creating new cheese trays ===");
        Results bufferResult = new ProcessCheeseFromBufferContainers().run(gui);
        if (!bufferResult.IsSuccess()) {
            gui.error("Failed to process cheese from buffer containers");
        }

        for (Map.Entry<String, Integer> work : workNeeded.entrySet()) {
            String cheeseType = work.getKey();
            int quantity = work.getValue();
            
            gui.msg("Processing " + quantity + " " + cheeseType + " cheese");
            
            // Get inventory capacity for batch processing
            int inventoryCapacity = rackManager.getInventoryCapacity(gui);
            
            // Process this order in batches
            Results orderResult = new ProcessCheeseOrderInBatches(cheeseType, quantity, inventoryCapacity, 
                                                                 rackManager, rackCapacity).run(gui);
            if (!orderResult.IsSuccess()) {
                gui.error("Failed to process " + cheeseType + " order");
                // Continue with other orders instead of failing completely
                continue;
            }
        }
        
        gui.msg("=== Cheese Production Bot Complete ===");
        return Results.SUCCESS();
    }
}