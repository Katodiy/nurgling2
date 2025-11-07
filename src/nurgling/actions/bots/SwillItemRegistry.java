package nurgling.actions.bots;

import java.util.Set;
import java.util.HashSet;

/**
 * Central registry for all swill-compatible items based on Ring of Brodgar wiki data.
 * Items are categorized by their swill value and practical collection considerations.
 */
public class SwillItemRegistry {

    public enum SwillCategory {
        HIGH_VALUE,      // >0.3L swill value
        STANDARD,        // 0.1L swill value
        LOW_VALUE,       // 0.05L and below
        SEEDS           // 0.02L, separate category for optional collection
    }

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
        "Barley Seed", "Carrot Seed", "Cucumber Seed", "Hemp Seed",
        "Millet Seed", "Pipeweed Seed", "Poppy Seed", "Turnip Seed",
        "Wheat Seed", "Flax Seed", "Pea Seed", "Pumpkin Seed"
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
     * @param includeLowValue Whether to include low-value items (leaves, wildflowers)
     * @param includeSeeds Whether to include crop seeds
     * @return true if the item should be collected as swill
     */
    public static boolean isSwillItem(String itemName, boolean includeLowValue, boolean includeSeeds) {
        if (HIGH_VALUE_SWILL.contains(itemName) || STANDARD_SWILL.contains(itemName)) {
            return true;
        }

        if (includeLowValue && LOW_VALUE_SWILL.contains(itemName)) {
            return true;
        }

        if (includeSeeds && SEED_SWILL.contains(itemName)) {
            return true;
        }

        return false;
    }

    /**
     * Get the swill category for a given item.
     *
     * @param itemName The exact item name as it appears in game
     * @return The SwillCategory or null if not a swill item
     */
    public static SwillCategory getCategory(String itemName) {
        if (HIGH_VALUE_SWILL.contains(itemName)) {
            return SwillCategory.HIGH_VALUE;
        } else if (STANDARD_SWILL.contains(itemName)) {
            return SwillCategory.STANDARD;
        } else if (LOW_VALUE_SWILL.contains(itemName)) {
            return SwillCategory.LOW_VALUE;
        } else if (SEED_SWILL.contains(itemName)) {
            return SwillCategory.SEEDS;
        }
        return null;
    }

    /**
     * Get all swill items for validation purposes.
     *
     * @return Set containing all defined swill item names
     */
    public static Set<String> getAllSwillItems() {
        return new HashSet<>(ALL_SWILL_ITEMS);
    }

    /**
     * Get the estimated swill value in liters for an item.
     *
     * @param itemName The exact item name as it appears in game
     * @return Swill value in liters, or 0.0 if not a swill item
     */
    public static double getSwillValue(String itemName) {
        if (itemName.equals("Giant Pumpkin")) return 1.6;
        if (itemName.equals("Clover") || itemName.equals("Rabbit Food")) return 0.5;
        if (itemName.equals("Head of Lettuce")) return 0.33;
        if (STANDARD_SWILL.contains(itemName)) return 0.1;
        if (itemName.equals("Lettuce Leaf") || itemName.equals("Wildflower")) return 0.05;
        if (SEED_SWILL.contains(itemName)) return 0.02;
        if (itemName.equals("Mulberry Leaf")) return 0.01;
        return 0.0;
    }

    /**
     * Get items prioritized by value for collection planning.
     * Higher value items should be collected first when inventory space is limited.
     *
     * @return Array of item sets in priority order (high to low)
     */
    public static Set<String>[] getItemsByPriority() {
        @SuppressWarnings("unchecked")
        Set<String>[] priorities = new Set[4];
        priorities[0] = HIGH_VALUE_SWILL;
        priorities[1] = STANDARD_SWILL;
        priorities[2] = LOW_VALUE_SWILL;
        priorities[3] = SEED_SWILL;
        return priorities;
    }
}