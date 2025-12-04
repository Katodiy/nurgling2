package nurgling.actions.bots;

import haven.Gob;
import nurgling.tools.NAlias;

import java.util.ArrayList;

/**
 * Utility methods and constants for garden pot operations.
 * Used by both GardenPotFiller and GardenPotPlanter bots.
 */
public class GardenPotUtils {

    public static final NAlias GARDEN_POT = new NAlias("gfx/terobjs/gardenpot");
    public static final NAlias SOIL = new NAlias("Soil", "Mulch");
    public static final NAlias WATER = new NAlias("Water");

    // Marker states
    public static final long MARKER_EMPTY = 0;           // Needs both mulch and water
    public static final long MARKER_WATER_ONLY = 1;      // Needs mulch
    public static final long MARKER_MULCH_ONLY = 2;      // Needs water
    public static final long MARKER_COMPLETE = 3;        // Ready for planting

    /**
     * Check if a pot needs mulch/soil.
     */
    public static boolean needsMulch(Gob pot) {
        long marker = pot.ngob.getModelAttribute();
        return marker == MARKER_EMPTY || marker == MARKER_WATER_ONLY;
    }

    /**
     * Check if a pot needs water.
     */
    public static boolean needsWater(Gob pot) {
        long marker = pot.ngob.getModelAttribute();
        return marker == MARKER_EMPTY || marker == MARKER_MULCH_ONLY;
    }

    /**
     * Check if a pot is ready for planting (has both soil and water).
     */
    public static boolean isReadyForPlanting(Gob pot) {
        return pot.ngob.getModelAttribute() == MARKER_COMPLETE;
    }

    /**
     * Filter pots that need mulch from a list.
     */
    public static ArrayList<Gob> filterPotsNeedingMulch(ArrayList<Gob> allPots) {
        ArrayList<Gob> result = new ArrayList<>();
        for (Gob pot : allPots) {
            if (needsMulch(pot)) {
                result.add(pot);
            }
        }
        return result;
    }

    /**
     * Filter pots that need water from a list.
     */
    public static ArrayList<Gob> filterPotsNeedingWater(ArrayList<Gob> allPots) {
        ArrayList<Gob> result = new ArrayList<>();
        for (Gob pot : allPots) {
            if (needsWater(pot)) {
                result.add(pot);
            }
        }
        return result;
    }

    /**
     * Filter pots that are ready for planting from a list.
     */
    public static ArrayList<Gob> filterPotsReadyForPlanting(ArrayList<Gob> allPots) {
        ArrayList<Gob> result = new ArrayList<>();
        for (Gob pot : allPots) {
            if (isReadyForPlanting(pot)) {
                result.add(pot);
            }
        }
        return result;
    }
}
