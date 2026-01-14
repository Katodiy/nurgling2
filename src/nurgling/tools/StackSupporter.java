package nurgling.tools;

import haven.Coord;
import haven.Window;
import nurgling.NInventory;
import nurgling.NUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class StackSupporter {

    // category -> stack size
    private static final Map<String, Integer> categorySize = new HashMap<>();

    private static final HashSet<String> catExceptions = new HashSet<>();
    private static final HashMap<String, Integer> customStackSizes = new HashMap<>();

    static {
        // Custom stack sizes for items that differ from their category defaults
        customStackSizes.put("Earthworm", 5);
        customStackSizes.put("Wolf's Claw", 4);
        customStackSizes.put("Clove of Garlic", 5);
        customStackSizes.put("Stinging Nettle", 4);
        customStackSizes.put("Yarrow", 4);
        customStackSizes.put("Reeds", 4);
        customStackSizes.put("Gooseneck Barnacle", 4);
        customStackSizes.put("River Pearl Mussel", 4);
        customStackSizes.put("Tuft of Squirrel's Finest Hair", 3);
        customStackSizes.put("Forest Lizard", 3);
        customStackSizes.put("Cavebulb", 4);
        customStackSizes.put("Dusk Fern", 4);
        customStackSizes.put("Straw", 5);
        customStackSizes.put("Standing Grass", 4);
        customStackSizes.put("Frog", 3);
        customStackSizes.put("Toad", 3);
        customStackSizes.put("Waybroad", 4);
        customStackSizes.put("Green Kelp", 4);
        customStackSizes.put("Cattail Roots", 4);
        customStackSizes.put("Heartwood Leaves", 4);

        putAll(3,
                "Tuber", "Onion", "Beetroot", "Carrot", "Cucumber",
                "Salad Greens", "Malted Grains", "Millable Seed", "Egg",
                "Gellant", "Stuffing", "Dried Fruit", "Flour",
                "Giant Ant", "Royal Ant", "Fishline", "Sweetener",
                "Thatching Material", "Vegetable Oil", "Solid Fat",
                "Snail", "Edible Seashell", "Candle", "Pearl",
                "Finer Plant Fibre", "Wicker", "Cloth", "Pigment",
                "Fine Clay", "Any Brick", "Clay", "Casting Material",
                "Ore", "Stone", "Lures", "Hooks", "Dried Fish",
                "Medicine", "Intestines", "Bait", "Pipeweed"
        );

        putAll(4,
                "Hide Fresh", "Prepared Animal Hide", "Bone Material",
                "Coal", "Wool", "Leaf", "Flower", "String", "Berry",
                "Edible Mushroom", "Spices", "Fruit", "Fruit or Berry",
                "Mantle", "Seed of Tree or Bush",
                "Decent-sized Conifer Cone", "Tree Bough",
                "Forageable", "Bug", "Miscellaneous",
                "Bark", "Shellfish", "Fish Fresh Water", "Fish Ocean",
                "Fish Cave", "Fish", "Cured Tea", "Stackable Curiosities",
                "Chitin"
        );

        putAll(5,
                "Entrails", "Feather", "Fine Feather", " Meat",
                "Raw Meat", "Bollock", "Filet of ", "Raw Chevon",
                "Raw Beef", "Raw Mutton", "Raw Pork", "Raw Horse",
                "Raw ", "Crab Meat", "Poultry", "Soil", "Nuts"
        );

        putAll(10,
                "Nugget of a Precious Metal",
                "Nugget of Bronze, Iron or Steel",
                "Nugget of Any Common Metal",
                "Nugget of Any Metal"
        );
    }

    private static void putAll(int size, String... cats) {
        for (String c : cats) {
            categorySize.put(c, size);
        }
    }

    static {
        catExceptions.add("Moose Antlers");
        catExceptions.add("Red Deer Antlers");
        catExceptions.add("Reindeer Antlers");
        catExceptions.add("Roe Deer Antlers");
        catExceptions.add("Wolf's Claw");
        catExceptions.add("Lynx Claws");
        catExceptions.add("Silkworm");
        catExceptions.add("Female Silkmoth");
        catExceptions.add("Male Silkmoth");
        catExceptions.add("Bog Turtle Shell");
        catExceptions.add("Mole's Pawbone");
        catExceptions.add("Lobster");
        catExceptions.add("Leech");
        catExceptions.add("Dried Filet");
        catExceptions.add("Billygoat Horn");
        catExceptions.add("Wildgoat Horn");
        catExceptions.add("Ant Chitin");
        catExceptions.add("Cave Louse Chitin");
        catExceptions.add("Driftkelp");
        catExceptions.add("A Beautiful Dream");
    }

    private static final NAlias unstackableContainers = new NAlias(
            "Smith's Smelter", "Ore Smelter", "Herbalist Table", "Tub",
            "Oven", "Steelbox", "Frame", "Kiln", "Smoke Shed", "Stack furnace"
    );

    public static boolean isStackable(NInventory inv, String name) {
        Window win = inv.getparent(Window.class);
        if (win != null) {
            if (NParser.checkName(win.cap, unstackableContainers)
                || NParser.checkName(name, new NAlias("Lynx Claws"))
                || name.equals("Silkworm")
                || name.contains("Dried Filet")
                || catExceptions.contains(name)) {
                return false;
            } else {
                ArrayList<String> categories = VSpec.getCategory(name);
                for (String cat : categories) {
                    if (categorySize.containsKey(cat)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static int getFullStackSize(String name) {
        Integer custom = customStackSizes.get(name);
        if (custom != null) {
            return custom;
        }

        if (catExceptions.contains(name)) {
            return 1;
        }

        ArrayList<String> categories = VSpec.getCategory(name);
        for (String cat : categories) {
            Integer size = categorySize.get(cat);
            if (size != null) {
                return size;
            }
        }

        return 1;
    }

    public static boolean isSameExist(NAlias items, NInventory inv) throws InterruptedException {
        if (items.keys.size() > 1) {
            return false;
        }

        ArrayList<String> categories = VSpec.getCategory(items.getDefault());
        if (categories.contains("Hide Fresh"))
            categories.add("Prepared Animal Hide");
        else if (categories.contains("Prepared Animal Hide"))
            categories.add("Hide Fresh");

        for (String cat : categories) {
            ArrayList<String> categoryContent = new ArrayList<>(VSpec.getCategoryContent(cat));
            categoryContent.removeAll(items.keys);
            if (!categoryContent.isEmpty()) {
                NAlias same = new NAlias(categoryContent);
                if (!inv.getItems(same).isEmpty())
                    return true;
            }
        }
        return false;
    }

    /**
     * Universal method to calculate optimal item capacity considering stacking
     */
    public static int getOptimalItemCapacity(NInventory inventory, String itemName, Coord itemSize, int targetCount) throws InterruptedException {
        int freeSlots = inventory.getNumberFreeCoord(itemSize);

        if (((NInventory) NUtils.getGameUI().maininv).bundle.a && isStackable(inventory, itemName)) {
            int maxStackSize = getFullStackSize(itemName);
            int maxCapacity = freeSlots * maxStackSize;
            return Math.min(targetCount, maxCapacity);
        }

        return Math.min(targetCount, freeSlots);
    }
}
