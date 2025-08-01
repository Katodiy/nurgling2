package nurgling.actions.bots.cheese;

import haven.WItem;
import nurgling.cheese.CheeseBranch;

/**
 * Utility methods for cheese detection, progression logic, and workflow decisions
 */
public class CheeseWorkflowUtils {
    
    /**
     * Check if a cheese tray is ready to move to next stage
     */
    public boolean isCheeseReadyToMove(WItem tray, CheeseBranch.Place currentPlace) {
        // TODO: Implement logic to check if cheese has aged long enough
        // For now, assume all cheese is ready to move (this needs refinement)
        
        String resourcePath = tray.item.res.get().name;
        
        // Find what cheese this is and what stage it should be at
//        for (CheeseBranch branch : CheeseBranch.branches) {
//            for (int i = 0; i < branch.steps.size(); i++) {
//                CheeseBranch.Cheese step = branch.steps.get(i);
//                if (step.place == currentPlace && resourceMatches(resourcePath, step.name)) {
//                    // This cheese is in the right place, check if it should move to next stage
//                    return i < branch.steps.size() - 1; // Not the final product yet
//                }
//            }
//        }
        
        return false; // Don't move if we're not sure
    }
    
    /**
     * Check if cheese is ready to slice (final product in chain)
     */
    public boolean isCheeseReadyToSlice(WItem tray) {
        String contentName = CheeseTrayUtils.getContentName(tray);
        if (contentName == null) return false; // Empty tray
        
        // Find this cheese in the production chains
        for (CheeseBranch branch : CheeseBranch.branches) {
            for (int i = 0; i < branch.steps.size(); i++) {
                CheeseBranch.Cheese step = branch.steps.get(i);
                if (step.name.equals(contentName)) {
                    // This is the final step if it's the last in the chain
                    return i == branch.steps.size() - 1;
                }
            }
        }
        return false;
    }
    
    /**
     * Check if cheese should move to next stage
     */
    public boolean shouldMoveToNextStage(WItem tray, CheeseBranch.Place currentPlace) {
        // If it's not ready to slice, and it's aged enough, it should move
        return !isCheeseReadyToSlice(tray) && isCheeseReadyToMove(tray, currentPlace);
    }
    
    /**
     * Get the next stage location for a cheese
     */
    public CheeseBranch.Place getNextStageLocation(WItem tray, CheeseBranch.Place currentPlace) {
        String contentName = CheeseTrayUtils.getContentName(tray);
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
    
    /**
     * Check if resource path matches cheese name (simplified)
     */
    public boolean resourceMatches(String resourcePath, String cheeseName) {
        // This is a simplified check - you might need more sophisticated matching
        return resourcePath.toLowerCase().contains(cheeseName.toLowerCase().replace(" ", "").replace("'", ""));
    }
    
    /**
     * Get the cheese type name from a tray's content
     */
    public String getCheeseTypeName(WItem tray) {
        String contentName = CheeseTrayUtils.getContentName(tray);
        return contentName != null ? contentName : "Unknown Cheese";
    }
    
    /**
     * Get the final product name for a production chain
     */
    public String getFinalProductName(String cheeseType) {
        for (CheeseBranch branch : CheeseBranch.branches) {
            if (!branch.steps.isEmpty()) {
                // Check if this branch produces the target cheese type
                for (CheeseBranch.Cheese step : branch.steps) {
                    if (step.name.equals(cheeseType)) {
                        // Return the final step's name
                        return branch.steps.get(branch.steps.size() - 1).name;
                    }
                }
            }
        }
        return cheeseType; // Default to input if not found
    }
}