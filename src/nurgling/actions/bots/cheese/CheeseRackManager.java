package nurgling.actions.bots.cheese;

import haven.Coord;
import haven.Gob;
import haven.WItem;
import nurgling.NGameUI;
import nurgling.areas.NArea;
import nurgling.areas.NContext;
import nurgling.cheese.CheeseBranch;
import nurgling.actions.PathFinder;
import nurgling.actions.OpenTargetContainer;
import nurgling.actions.CloseTargetContainer;
import nurgling.actions.TransferWItemsToContainer;
import nurgling.tools.Container;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.cheese.CheeseOrdersManager;
import nurgling.cheese.CheeseOrder;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles cheese rack capacity checking and tray movement between areas
 */
public class CheeseRackManager {
    private final Coord TRAY_SIZE = new Coord(1, 2);
    
    public CheeseRackManager() {
    }
    
    /**
     * Get available inventory capacity for cheese trays
     */
    public int getInventoryCapacity(NGameUI gui) throws InterruptedException {
        // Calculate how many cheese trays can fit in current inventory
        return gui.getInventory().getNumberFreeCoord(TRAY_SIZE);
    }
    
    /**
     * Handle tray placement with order saving
     * @return number of trays actually placed
     */
    public int handleTrayPlacement(NGameUI gui, CheeseBranch.Place targetPlace, int batchSize, String cheeseType, CheeseOrdersManager ordersManager, CheeseOrder specificOrder) throws InterruptedException {
        return moveTraysToRacks(gui, targetPlace, batchSize, cheeseType, ordersManager, specificOrder);
    }
    
    /**
     * Move cheese trays to racks in target place type's areas with order saving
     * Supports multiple areas per place type - fills one area completely before moving to next
     */
    public int moveTraysToRacks(NGameUI gui, CheeseBranch.Place targetPlace, int quantity, String cheeseType, CheeseOrdersManager ordersManager, CheeseOrder specificOrder) throws InterruptedException {
        int moved = 0;

        // Get ALL areas for this place type
        ArrayList<NArea> targetAreas = CheeseAreaManager.getAllCheeseAreas(targetPlace);
        if (targetAreas.isEmpty()) {
            return 0;
        }

        // Process each area until all trays are placed or no more space
        for (NArea targetArea : targetAreas) {
            if (moved >= quantity) break;

            // Navigate to this area
            NContext context = new NContext(gui);
            context.getAreaById(targetArea.id);

            ArrayList<Gob> racks = Finder.findGobs(targetArea, new NAlias("gfx/terobjs/cheeserack"));

            for (Gob rack : racks) {
                if (moved >= quantity) break;
                
                // Skip visually full racks before walking to them
                if (rack.ngob.isContainerFull()) {
                    continue;
                }
                
                Container rackContainer = new Container(rack, "Rack", targetArea);
                rackContainer.initattr(Container.Space.class);
                new PathFinder(rack).run(gui);
                new OpenTargetContainer(rackContainer).run(gui);

                // Check how many trays we can fit in this rack
                int availableSpace = gui.getInventory(rackContainer.cap).getNumberFreeCoord(TRAY_SIZE);
                
                if (availableSpace == 0) {
                    // Rack is full, skip it
                    new CloseTargetContainer(rackContainer).run(gui);
                    continue;
                }
                
                int toMove = Math.min(availableSpace, quantity - moved);

                // Get specific trays of the requested cheese type
                ArrayList<WItem> specificTrays = getTraysOfType(gui, cheeseType, toMove);
                if (!specificTrays.isEmpty()) {
                    // Transfer while container is still open
                    new TransferWItemsToContainer(rackContainer, specificTrays).run(gui);
                    moved += specificTrays.size();

                    // Update and save orders after placing trays on rack
                    if (ordersManager != null && specificOrder != null) {
                        updateOrdersAfterCurdPlacement(ordersManager, cheeseType, specificTrays.size(), specificOrder);
                        ordersManager.writeOrders();
                    }
                }

                new CloseTargetContainer(rackContainer).run(gui);
            }
        }

        return moved;
    }
    
    /**
     * Get specific cheese trays of a particular type from inventory
     */
    private ArrayList<WItem> getTraysOfType(NGameUI gui, String cheeseType, int maxCount) throws InterruptedException {
        ArrayList<WItem> allTrays = CheeseInventoryOperations.getCheeseTrays(gui);
        ArrayList<WItem> specificTrays = new ArrayList<>();
        
        for (WItem tray : allTrays) {
            if (specificTrays.size() >= maxCount) break;
            
            String contentName = CheeseUtils.getContentName(tray);
            if (cheeseType.equals(contentName)) {
                specificTrays.add(tray);
            }
        }
        
        return specificTrays;
    }
    
    /**
     * Update specific order after placing curd trays on racks
     */
    private void updateOrdersAfterCurdPlacement(CheeseOrdersManager ordersManager, String cheeseType, int traysPlaced, CheeseOrder specificOrder) {
        // Find the production chain for this specific order
        List<CheeseBranch.Cheese> chain = CheeseBranch.getChainToProduct(specificOrder.getCheeseType());
        if (chain == null) return;
        
        // Find the "start" step and reduce it
        for (CheeseOrder.StepStatus step : specificOrder.getStatus()) {
            if (step.place.equals("start") && step.name.equals(cheeseType) && step.left > 0) {
                step.left = Math.max(0, step.left - traysPlaced);
                
                // Add to next step if there is one
                if (chain.size() > 1) {
                    CheeseBranch.Cheese nextStep = chain.get(1);
                    
                    // Find or create next step
                    CheeseOrder.StepStatus nextStepStatus = null;
                    for (CheeseOrder.StepStatus nextStepCheck : specificOrder.getStatus()) {
                        if (nextStepCheck.name.equals(nextStep.name) && 
                            nextStepCheck.place.equals(nextStep.place.toString())) {
                            nextStepStatus = nextStepCheck;
                            break;
                        }
                    }
                    
                    if (nextStepStatus == null) {
                        nextStepStatus = new CheeseOrder.StepStatus(nextStep.name, nextStep.place.toString(), 0);
                        specificOrder.getStatus().add(nextStepStatus);
                    }
                    
                    nextStepStatus.left += traysPlaced;
                }
                
                ordersManager.addOrUpdateOrder(specificOrder);
                return;
            }
        }
    }
}