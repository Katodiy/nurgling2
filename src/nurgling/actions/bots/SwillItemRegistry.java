package nurgling.actions.bots;

import java.util.Set;
import java.util.HashSet;

/**
 * Central registry for all swill-compatible items based on Ring of Brodgar wiki data.
 * Items are categorized by their swill value and practical collection considerations.
 */
public class SwillItemRegistry {

    // High value items (>0.3L swill value)
    public static final Set<String> HIGH_VALUE_SWILL = Set.of(
        "Giant Pumpkin",     // 1.6L
        "Head of Lettuce",   // 0.33L
        "Clover",           // 0.5L
        "Rabbit Food"       // 0.5L
    );

    // Standard items (0.1L swill value)
    public static final Set<String> STANDARD_SWILL = Set.of(
        "Bee Larvae", "Beetroot", "Beetroot Leaves", "Blueberries",
        "Bollock", "Bread", "Carrot", "Cattail Roots", "Chantrelles",
        "Chives", "Coltsfoot", "Crab Roe", "Dandelion",
        "Dewy Lady's Mantle", "Dusk Fern", "Grapes", "Green Kelp",
        "Grub", "Kvann", "Lady's Mantle", "Liberty Caps", "Lingonberries",
        "Mulberry", "Parasol Mushroom", "Peapod", "Portobello Mushroom",
        "Pumpkin Flesh", "Pumpkin Stew", "Raw Meat", "Red Onion",
        "Sorb Apple", "Spindly Taproot", "Stalagoom", "Straw",
        "Strawberry", "Turnip", "Wild Onion", "Yellowfeet", "Yellow Onion"
    );

    // Low value items (0.05L and below)
    public static final Set<String> LOW_VALUE_SWILL = Set.of(
        "Lettuce Leaf",     // 0.05L
        "Wildflower",       // 0.05L
        "Mulberry Leaf"     // 0.01L
    );

    // Seeds (0.02L) - separate category for optional collection
    public static final Set<String> SEED_SWILL = Set.of(
        "Barley Seeds", "Carrot Seeds", "Cucumber Seeds", "Hemp Seeds",
        "Millet Seeds", "Pipeweed Seeds", "Poppy Seeds", "Turnip Seeds",
        "Wheat Seeds", "Flax Seeds", "Pea Seeds", "Pumpkin Seeds"
    );

    // Combined set of all swill items for convenience
    private static final Set<String> ALL_SWILL_ITEMS = new HashSet<>();
    static {
        ALL_SWILL_ITEMS.addAll(HIGH_VALUE_SWILL);
        ALL_SWILL_ITEMS.addAll(STANDARD_SWILL);
        ALL_SWILL_ITEMS.addAll(LOW_VALUE_SWILL);
        ALL_SWILL_ITEMS.addAll(SEED_SWILL);
    }

    /**
     * Check if an item can be used as swill based on configuration options.
     *
     * @param itemName The exact item name as it appears in game
     * @return true if the item should be collected as swill
     */
    public static boolean isSwillItem(String itemName) {
        if (HIGH_VALUE_SWILL.contains(itemName) || STANDARD_SWILL.contains(itemName)) {
            return true;
        }

        if (LOW_VALUE_SWILL.contains(itemName)) {
            return true;
        }

        if (SEED_SWILL.contains(itemName)) {
            return true;
        }

        return false;
    }
}