package nurgling.actions.bots;

import nurgling.NGameUI;
import nurgling.actions.*;
import nurgling.actions.bots.cheese.*;
import nurgling.areas.NContext;
import nurgling.cheese.CheeseBranch;
import nurgling.cheese.CheeseOrdersManager;

import java.util.Map;

/**
 * This orchestrates the high-level workflow and delegates specific tasks
 */
public class CheeseProductionBot implements Action {

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        CheeseRackManager rackManager = new CheeseRackManager();
        
        // Create a single shared CheeseOrdersManager instance to eliminate redundant file I/O
        CheeseOrdersManager sharedOrdersManager = new CheeseOrdersManager();
        
        gui.msg("=== Starting Cheese Production Bot ===");
        
        // 1. Analyze current orders and determine what needs to be done
        Map<String, Integer> workNeeded = CheeseUtils.analyzeOrders(gui, sharedOrdersManager);
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
        Map<CheeseBranch.Place, Boolean> bufferEmptinessMap = clearAction.getBufferEmptinessMap();

        // 3. Pass 2: Process buffers (slice ready cheese, move aging cheese)
        gui.msg("=== Pass 2: Processing buffers ===");
        ProcessCheeseFromBufferContainers bufferAction = new ProcessCheeseFromBufferContainers(sharedOrdersManager, rackCapacity, bufferEmptinessMap);
        Results bufferResult = bufferAction.run(gui);
        if (!bufferResult.IsSuccess()) {
            gui.error("Failed to process cheese from buffer containers");
        }

        // 4. Update rack capacity based on cheese movements
        Map<CheeseBranch.Place, Integer> traysMovedToAreas = bufferAction.getTraysMovedToAreas();
        for (Map.Entry<CheeseBranch.Place, Integer> entry : traysMovedToAreas.entrySet()) {
            CheeseBranch.Place area = entry.getKey();
            int traysMovedToArea = entry.getValue();
            int currentCapacity = rackCapacity.getOrDefault(area, 0);
            int updatedCapacity = Math.max(0, currentCapacity - traysMovedToArea);
            rackCapacity.put(area, updatedCapacity);
            gui.msg("Updated " + area + " capacity: " + currentCapacity + " - " + traysMovedToArea + " = " + updatedCapacity);
        }

        // 5. Create new curd trays for incomplete orders (only "start" step)
        gui.msg("=== Pass 3: Creating new curd trays ===");
        for (Map.Entry<String, Integer> work : workNeeded.entrySet()) {
            String cheeseType = work.getKey();
            int quantity = work.getValue();
            
            gui.msg("Checking if " + cheeseType + " needs new curd creation");
            
            // Get inventory capacity for batch processing
            int inventoryCapacity = rackManager.getInventoryCapacity(gui);
            
            // Only process curd creation (start step) - movement work was already handled in steps 2-3
            Results orderResult = new ProcessCheeseOrderInBatches(cheeseType, quantity, inventoryCapacity,
                    rackManager, rackCapacity, sharedOrdersManager).run(gui);
            if (!orderResult.IsSuccess()) {
                gui.error("Failed to process " + cheeseType + " curd creation");
            }
        }
        
        gui.msg("=== Cheese Production Bot Complete ===");
        new FreeInventory2(new NContext(gui)).run(gui);
        return Results.SUCCESS();
    }
}