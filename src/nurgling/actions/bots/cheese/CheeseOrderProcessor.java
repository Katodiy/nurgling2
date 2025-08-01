package nurgling.actions.bots.cheese;

import nurgling.NGameUI;
import nurgling.cheese.CheeseOrder;
import nurgling.cheese.CheeseOrdersManager;
import nurgling.cheese.CheeseBranch;

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
    public Map<String, Integer> analyzeOrders(NGameUI gui) {
        Map<String, Integer> workNeeded = new HashMap<>();
        
        for (CheeseOrder order : ordersManager.getOrders().values()) {
            String cheeseType = order.getCheeseType();
            
            // Get the production chain for this cheese
            List<CheeseBranch.Cheese> chain = CheeseBranch.getChainToProduct(cheeseType);
            if (chain == null) {
                gui.msg("Unknown cheese type: " + cheeseType);
                continue;
            }
            
            // For now, focus on the first step (creating curds)
            // TODO Add the rest of the steps
            for (CheeseOrder.StepStatus status : order.getStatus()) {
                if (status.left > 0) {
                    workNeeded.put(cheeseType, status.left);
                    break; // Focus on first incomplete step
                }
            }
        }
        
        return workNeeded;
    }
}