package nurgling.actions.bots;

import nurgling.areas.NContext;

import java.util.List;

/**
 * Simple swill destination finder.
 * For the initial implementation, this just provides basic area discovery.
 */
public class SwillDestinationFinder {

    /**
     * Find swill delivery areas using NContext specialization system.
     */
    public static List<nurgling.areas.NArea> findSwillAreas() {
        return NContext.findSwillDeliveryAreas();
    }
}