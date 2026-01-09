package nurgling.actions.bots.cheese;

import nurgling.NGameUI;
import nurgling.areas.NArea;
import nurgling.areas.NContext;
import nurgling.cheese.CheeseBranch;

import java.util.ArrayList;

/**
 * Manages cheese area navigation and context operations
 * Eliminates the repeated freshContext.getSpecArea pattern (7+ occurrences)
 */
public class CheeseAreaManager {

    /**
     * Get cheese racks area for a specific place (returns nearest single area)
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
     * Get ALL cheese racks areas for a specific place (supports multiple areas per place type)
     * Returns areas sorted by distance from player.
     *
     * @param place The cheese area place (outside, inside, mine, cellar)
     * @return List of all cheese racks areas for this place type, sorted by distance
     */
    public static ArrayList<NArea> getAllCheeseAreas(CheeseBranch.Place place) {
        return NContext.findAllSpec(CheeseConstants.CHEESE_RACKS_SPEC.toString(), place.toString());
    }
}