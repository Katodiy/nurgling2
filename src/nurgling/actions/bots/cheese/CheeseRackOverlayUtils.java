package nurgling.actions.bots.cheese;

import haven.Gob;
import nurgling.tools.Container;
import nurgling.tools.Finder;

/**
 * Utility class for checking cheese rack status using overlays without opening containers
 * This provides significant performance improvements by avoiding unnecessary container operations
 */
public class CheeseRackOverlayUtils {
    
    public enum RackStatus {
        EMPTY,      // 0 overlays - can fit 3 trays, no cheese to move
        PARTIAL,    // 1-2 overlays - has some cheese, may have space
        FULL        // 3 overlays - no space available, may have ready cheese
    }
    
    /**
     * Check cheese rack status using overlay counting without opening the container
     * @param rackGob The cheese rack game object
     * @return RackStatus indicating empty/partial/full
     */
    public static RackStatus getRackStatus(Gob rackGob) {
        if (rackGob == null) {
            return RackStatus.EMPTY;
        }
        
        int overlayCount = 0;
        for (Gob.Overlay ol : rackGob.ols) {
            if (ol.spr instanceof haven.res.gfx.fx.eq.Equed) {
                overlayCount++;
            }
        }
        
        if (overlayCount == 0) {
            return RackStatus.EMPTY;
        } else if (overlayCount >= 3) {
            return RackStatus.FULL;
        } else {
            return RackStatus.PARTIAL;
        }
    }
    
    /**
     * Check if a cheese rack is empty (no overlays)
     * Empty racks can fit 3 trays and have no cheese to process
     */
    public static boolean isRackEmpty(Gob rackGob) {
        return getRackStatus(rackGob) == RackStatus.EMPTY;
    }
    
    /**
     * Check if a cheese rack is full (3+ overlays)
     * Full racks cannot accept more trays from buffers
     */
    public static boolean isRackFull(Gob rackGob) {
        return getRackStatus(rackGob) == RackStatus.FULL;
    }
    
    /**
     * Get available capacity for a rack based on overlay count
     * @param rackGob The cheese rack game object
     * @return Estimated number of tray slots available (0-3)
     */
    public static int getEstimatedAvailableCapacity(Gob rackGob) {
        RackStatus status = getRackStatus(rackGob);
        switch (status) {
            case EMPTY:
                return 3; // Can fit 3 trays
            case PARTIAL:
                // Conservative estimate - could be 1 or 2 available
                return 1; // Safe assumption for partial racks
            case FULL:
                return 0; // No space available
            default:
                return 0;
        }
    }
    
    /**
     * Check if a rack is worth processing for moving cheese TO it
     * @param rackGob The cheese rack game object
     * @return true if rack has space for new trays
     */
    public static boolean canAcceptTrays(Gob rackGob) {
        return !isRackFull(rackGob);
    }
    
    /**
     * Check if a rack is worth processing for moving cheese FROM it
     * @param rackGob The cheese rack game object  
     * @return true if rack might have cheese to move
     */
    public static boolean mightHaveCheeseToMove(Gob rackGob) {
        return !isRackEmpty(rackGob);
    }
    
    /**
     * Filter racks by status for batch processing
     * @param racks List of rack game objects
     * @param targetStatus Only return racks with this status
     * @return Filtered list of racks
     */
    public static java.util.ArrayList<Gob> filterRacksByStatus(java.util.ArrayList<Gob> racks, RackStatus targetStatus) {
        java.util.ArrayList<Gob> filtered = new java.util.ArrayList<>();
        for (Gob rack : racks) {
            if (getRackStatus(rack) == targetStatus) {
                filtered.add(rack);
            }
        }
        return filtered;
    }
    
    /**
     * Get summary of rack statuses for debugging/logging
     * @param racks List of rack game objects
     * @return String summary like "Empty: 5, Partial: 2, Full: 3"
     */
    public static String getRackStatusSummary(java.util.ArrayList<Gob> racks) {
        int empty = 0, partial = 0, full = 0;
        for (Gob rack : racks) {
            RackStatus status = getRackStatus(rack);
            switch (status) {
                case EMPTY: empty++; break;
                case PARTIAL: partial++; break;
                case FULL: full++; break;
            }
        }
        return String.format("Empty: %d, Partial: %d, Full: %d", empty, partial, full);
    }
}