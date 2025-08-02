package nurgling.actions.bots.cheese;

import haven.WItem;
import nurgling.NGameUI;
import nurgling.NGItem;
import nurgling.cheese.CheeseOrder;
import nurgling.cheese.CheeseOrdersManager;
import nurgling.cheese.CheeseBranch;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Combined utility class for all cheese-related operations including:
 * - Cheese tray content inspection
 * - Cheese workflow and progression logic
 * - Cheese order processing and analysis
 */
public class CheeseUtils {
    
    // ============== CHEESE TRAY UTILITIES ==============
    
    /**
     * Check if a cheese tray is empty (no content)
     */
    public static boolean isEmpty(WItem tray) {
        if (tray == null) return true;
        
        List<NGItem.NContent> contents = ((NGItem) tray.item).content();
        return contents == null || contents.isEmpty();
    }
    
    /**
     * Get the content name of what's inside a cheese tray
     * @param tray The cheese tray WItem
     * @return The name of the content (e.g., "Sheep's Curd", "Feta", etc.) or null if empty
     */
    public static String getContentName(WItem tray) {
        if (tray == null) return null;
        
        List<NGItem.NContent> contents = ((NGItem) tray.item).content();
        if (contents == null || contents.isEmpty()) {
            return null; // Empty tray
        }
        
        // Return the name of the first content item
        return contents.get(0).name();
    }
    
    // ============== CHEESE WORKFLOW UTILITIES ==============
    
    /**
     * Check if a cheese tray is ready to move to next stage
     * 
     * Uses location-based filtering logic: builds a candidates list of cheese names 
     * that belong in the current location, then checks if the tray's content matches 
     * any candidate name and has a next stage. No explicit readiness state - assumes 
     * any cheese of the correct type for the current location is ready to move if it 
     * has a next progression step.
     */
    public static boolean isCheeseReadyToMove(WItem tray, CheeseBranch.Place currentPlace) {
        // Get the content name from the tray
        String contentName = getContentName(tray);
        if (contentName == null) {
            return false; // Empty tray cannot be moved
        }
        
        // Build candidates list: cheese names that belong in the current location
        // and check if they have a next stage in their progression
        for (CheeseBranch branch : CheeseBranch.branches) {
            for (int i = 0; i < branch.steps.size(); i++) {
                CheeseBranch.Cheese step = branch.steps.get(i);
                if (step.place == currentPlace && step.name.equals(contentName)) {
                    // This cheese is in the correct current location according to 
                    // the progression schedule. Check if there's a next stage.
                    return i < branch.steps.size() - 1; // Ready to move if not final product
                }
            }
        }
        
        return false; // This cheese doesn't belong in current location or is final product
    }
    
    /**
     * Check if cheese is ready to slice (has been ordered by a customer)
     * A cheese is ready to slice if it appears in any active order, regardless of 
     * whether it's the final product in its branch or an intermediate step
     */
    public static boolean isCheeseReadyToSlice(WItem tray) {
        String contentName = getContentName(tray);
        if (contentName == null) return false; // Empty tray
        
        // Check if this cheese type is in any active order
        CheeseOrdersManager ordersManager = new CheeseOrdersManager();
        for (CheeseOrder order : ordersManager.getOrders().values()) {
            if (order.getCheeseType().equals(contentName)) {
                // This cheese type has been ordered, so it's ready to slice
                return true;
            }
        }
        
        return false; // This cheese type hasn't been ordered
    }
    
    /**
     * Check if cheese should move to next stage
     */
    public static boolean shouldMoveToNextStage(WItem tray, CheeseBranch.Place currentPlace) {
        // If it's not ready to slice, and it's aged enough, it should move
        return !isCheeseReadyToSlice(tray) && isCheeseReadyToMove(tray, currentPlace);
    }
    
    /**
     * Get the next stage location for a cheese
     */
    public static CheeseBranch.Place getNextStageLocation(WItem tray, CheeseBranch.Place currentPlace) {
        String contentName = getContentName(tray);
        if (contentName == null) return null; // Empty tray
        
        for (CheeseBranch branch : CheeseBranch.branches) {
            for (int i = 0; i < branch.steps.size() - 1; i++) { // -1 because we want next step
                CheeseBranch.Cheese step = branch.steps.get(i);
                if (step.place == currentPlace && step.name.equals(contentName)) {
                    // Return the next step's location
                    return branch.steps.get(i + 1).place;
                }
            }
        }
        return null;
    }
    
    // ============== CHEESE ORDER PROCESSING ==============
    
    /**
     * Analyze current orders to determine what curd creation work needs to be done
     * Only focuses on "start" step since movement work is handled by buffer processing
     */
    public static Map<String, Integer> analyzeOrders(NGameUI gui) {
        CheeseOrdersManager ordersManager = new CheeseOrdersManager();
        Map<String, Integer> curdWorkNeeded = new HashMap<>();
        
        for (CheeseOrder order : ordersManager.getOrders().values()) {
            String cheeseType = order.getCheeseType();
            
            // Get the production chain for this cheese
            List<CheeseBranch.Cheese> chain = CheeseBranch.getChainToProduct(cheeseType);
            if (chain == null) {
                gui.msg("Unknown cheese type: " + cheeseType);
                continue;
            }
            
            // Only look for "start" step work (curd creation)
            // Movement work between stages is handled by buffer processing
            for (CheeseOrder.StepStatus status : order.getStatus()) {
                if (status.left > 0 && status.place.equals("start")) {
                    curdWorkNeeded.put(cheeseType, status.left);
                    gui.msg("Need to create " + status.left + " " + status.name + " trays for " + cheeseType);
                    break; // Only need the start step
                }
            }
        }
        
        return curdWorkNeeded;
    }
}