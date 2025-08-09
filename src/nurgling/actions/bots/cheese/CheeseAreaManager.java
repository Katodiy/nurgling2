package nurgling.actions.bots.cheese;

import nurgling.NGameUI;
import nurgling.areas.NArea;
import nurgling.areas.NContext;
import nurgling.cheese.CheeseBranch;

/**
 * Manages cheese area navigation and context operations
 * Eliminates the repeated freshContext.getSpecArea pattern (7+ occurrences)
 */
public class CheeseAreaManager {
    
    /**
     * Get cheese racks area for a specific place
     * Replaces the repeated pattern: freshContext.getSpecArea(Specialisation.SpecName.cheeseRacks, place.toString())
     * 
     * @param gui The game UI
     * @param place The cheese area place (outside, inside, mine, cellar)
     * @return The cheese racks area, or null if not found
     */
    public static NArea getCheeseArea(NGameUI gui, CheeseBranch.Place place) throws InterruptedException {
        NContext context = new NContext(gui);
        return context.getSpecArea(CheeseConstants.CHEESE_RACKS_SPEC, place.toString());
    }
}