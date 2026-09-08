package nurgling.tools;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Ore / Smith's Smelter tooltip timings from the Ring of Brodgar Ore Smelter wiki:
 * 12 charcoal = 55 min; Well Mined is 9/12 of that (41 min 15 sec) per piece.
 */
public final class SmelterOreTip {
    public static final int REGULAR_SECONDS = 3300;
    public static final int WELL_MINED_SECONDS = 9 * REGULAR_SECONDS / 12;

    private static final Set<String> ORES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "Cassiterite", "Lead Glance", "Wine Glance", "Chalcopyrite", "Malachite", "Peacock Ore",
            "Cinnabar", "Heavy Earth", "Iron Ochre", "Bloodstone", "Black Ore", "Galena",
            "Silvershine", "Horn Silver", "Direvein", "Schrifterz", "Leaf Ore", "Meteorite", "Dross"
    )));

    private static final Set<String> FUEL = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "Coal", "Charcoal", "Black Coal"
    )));

    private SmelterOreTip() {
    }

    public static boolean isSmelterWindow(String cap) {
        return "Ore Smelter".equals(cap) || "Smith's Smelter".equals(cap);
    }

    public static boolean isOre(String itemName) {
        return itemName != null && ORES.contains(itemName);
    }

    public static boolean isFuel(String itemName) {
        return itemName != null && FUEL.contains(itemName);
    }

    /**
     * Remaining real-time smelt seconds from the current meter (0-100).
     * Missing meter is 0% (full duration). Well Mined uses 9/12 of 55 min.
     */
    public static int remainingSeconds(int meterPercent, boolean wellMined) {
        int pct = KilnFiringTip.meterPercent(meterPercent);
        int full = wellMined ? WELL_MINED_SECONDS : REGULAR_SECONDS;
        return (int) Math.round((100.0 - pct) / 100.0 * full);
    }

    /**
     * Extra smelter tip for Ore / Smith's Smelter ore slots. Fuel is never shown.
     * Known ore names render even unlit; Well Mined or a drawn meter cover unlisted ore.
     */
    public static boolean shouldRender(String windowCap, String itemName,
                                       boolean wellMined, boolean hasMeter) {
        if (!isSmelterWindow(windowCap))
            return false;
        if (isFuel(itemName))
            return false;
        return isOre(itemName) || wellMined || hasMeter;
    }
}
