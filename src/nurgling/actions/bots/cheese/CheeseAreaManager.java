package nurgling.actions.bots.cheese;

import nurgling.NGameUI;
import nurgling.areas.NArea;
import nurgling.areas.NContext;
import nurgling.cheese.CheeseBranch;
import nurgling.widgets.Specialisation;

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
    
    /**
     * Check if a cheese area exists for the given place
     * 
     * @param gui The game UI
     * @param place The cheese area place
     * @return true if area exists, false otherwise
     */
    public static boolean hasCheeseArea(NGameUI gui, CheeseBranch.Place place) throws InterruptedException {
        return getCheeseArea(gui, place) != null;
    }
    
    /**
     * Navigate to a specific cheese area if it exists
     * 
     * @param gui The game UI
     * @param place The cheese area place
     * @return The area if found and navigated to, null otherwise
     */
    public static NArea navigateToCheeseArea(NGameUI gui, CheeseBranch.Place place) throws InterruptedException {
        NArea area = getCheeseArea(gui, place);
        if (area != null) {
            // Area is automatically set as current context when retrieved
            gui.msg("Navigated to " + place + " cheese area");
        } else {
            gui.msg("No cheese area found for " + place);
        }
        return area;
    }
}