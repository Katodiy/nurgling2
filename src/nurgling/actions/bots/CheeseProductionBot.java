package nurgling.actions.bots;

import nurgling.NGameUI;
import nurgling.actions.Action;
import nurgling.actions.Results;
import nurgling.areas.NContext;
import nurgling.actions.bots.cheese.*;
import nurgling.cheese.CheeseBranch;

import java.util.Map;

/**
 * This orchestrates the high-level workflow and delegates specific tasks
 */
public class CheeseProductionBot implements Action {
    
    private CheeseOrderProcessor orderProcessor;
    private CheeseRackManager rackManager;
    private CheeseBufferManager bufferManager;
    private CheeseSlicingManager slicingManager;
    private CheeseWorkflowUtils workflowUtils;
    private NContext context;
    
    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        // Initialize components
        context = new NContext(gui);
        workflowUtils = new CheeseWorkflowUtils();
        slicingManager = new CheeseSlicingManager();
        rackManager = new CheeseRackManager(context);
        bufferManager = new CheeseBufferManager(context, workflowUtils, slicingManager);
        orderProcessor = new CheeseOrderProcessor();
        
        gui.msg("=== Starting Cheese Production Bot ===");
        
        // 1. Analyze current orders and determine what needs to be done
        Map<String, Integer> workNeeded = orderProcessor.analyzeOrders();
        if (workNeeded.isEmpty()) {
            gui.msg("No work needed - all orders complete!");
            return Results.SUCCESS();
        }
        
        // 2. Phase 1: Clear all ready cheese from racks to buffer containers
        gui.msg("=== Phase 1: Clearing ready cheese from racks ===");
//        bufferManager.clearReadyCheeseFromAllRacks(gui);
        
        // 3. Phase 2: Process cheese from buffer containers
        gui.msg("=== Phase 2: Processing cheese from buffer containers ===");
//        bufferManager.processCheeseFromBufferContainers(gui);
        
        // 4. Phase 3: Check available rack capacity after clearing
//        Map<CheeseBranch.Place, Integer> rackCapacity = rackManager.checkRackCapacity(gui);
        
        // 5. Phase 4: Create new cheese trays if needed and space available
        gui.msg("=== Phase 3: Creating new cheese trays ===");
        for (Map.Entry<String, Integer> work : workNeeded.entrySet()) {
            String cheeseType = work.getKey();
            int quantity = work.getValue();
            
            gui.msg("Processing " + quantity + " " + cheeseType + " cheese");
            
            // Get inventory capacity for batch processing
            int inventoryCapacity = rackManager.getInventoryCapacity(gui);
            
            // Process this order in batches
            orderProcessor.processCheeseOrderInBatches(gui, cheeseType, quantity, 
                                                      inventoryCapacity, rackManager);
        }
        
        gui.msg("=== Cheese Production Bot Complete ===");
        return Results.SUCCESS();
    }
}