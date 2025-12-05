package nurgling.tools;

import haven.Coord;
import haven.Window;
import nurgling.NInventory;
import nurgling.NUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class StackSupporter {
    private static final HashMap<HashSet<String>,Integer> catSize = new HashMap<>();
    private static final HashSet<String> catExceptions = new HashSet<>();
    private static final HashMap<String, Integer> customStackSizes = new HashMap<>();
    static {
        // Custom stack sizes for items that differ from their category defaults
        customStackSizes.put("Earthworm", 5);
        customStackSizes.put("Itsy Bitsy Spider", 4);
        customStackSizes.put("Cave Moth", 4);
        customStackSizes.put("Bog Turtle Shell", 1);
        customStackSizes.put("Wolf's Claw", 4);
        customStackSizes.put("Clove of Garlic", 5);
        HashSet<String> size3 = new HashSet<>();
        size3.add("Tuber");
        size3.add("Onion");
        size3.add("Beetroot");
        size3.add("Carrot");
        size3.add("Cucumber");
        size3.add("Salad Greens");
        size3.add("Malted Grains");
        size3.add("Millable Seed");
        size3.add("Egg");
        size3.add("Gellant");
        size3.add("Stuffing");
        size3.add("Dried Fruit");
        size3.add("Flour");
        size3.add("Giant Ant");
        size3.add("Royal Ant");
        size3.add("Bug");
        size3.add("Fishline");
        size3.add("Sweetener");
        size3.add("Thatching Material");
        size3.add("Vegetable Oil");
        size3.add("Solid Fat");
        size3.add("Cured Tea");
        size3.add("Snail");
        size3.add("Clean Animal Carcass");
        size3.add("Edible Seashell");
        size3.add("Candle");
        size3.add("Pearl");
        size3.add("Finer Plant Fibre");
        size3.add("Wicker");
        size3.add("Cloth");
        size3.add("Pigment");
        size3.add("Fine Clay");
        size3.add("Any Brick");
        size3.add("Clay");
        size3.add("Casting Material");
        size3.add("Board");
        size3.add("Block of Wood");
        size3.add("Ore");
        size3.add("Stone");
        size3.add("Fish Fresh Water");
        size3.add("Fish Ocean");
        size3.add("Fish Cave");
        size3.add("Fish");
        size3.add("Lures");
        size3.add("Hooks");
        size3.add("Dried Fish");
        size3.add("Medicine");
        size3.add("Intestines");
        size3.add("Bait");
        catSize.put(size3,3);

        HashSet<String> size4 = new HashSet<>();
        size4.add("Hide Fresh");
        size4.add("Prepared Animal Hide");
        size4.add("Bone Material");
        size4.add("Coal");
        size4.add("Wool");
        size4.add("Leaf");
        size4.add("Flower");
        size4.add("String");
        size4.add("Berry");
        size4.add("Edible Mushroom");
        size4.add("Spices");
        size4.add("Fruit");
        size4.add("Fruit or Berry");
        size4.add("Mantle");
        size4.add("Seed of Tree or Bush");
        size4.add("Decent-sized Conifer Cone");
        size4.add("Tree Bough");
        size4.add("Forageable");

        catSize.put(size4,4);

        HashSet<String> size5 = new HashSet<>();

        size5.add("Entrails");
        size5.add("Feather");
        size5.add("Fine Feather");
        size5.add(" Meat");
        size5.add("Raw Meat");
        size5.add("Bollock");
        size5.add("Filet of ");
        size5.add("Raw Chevon");
        size5.add("Raw Beef");
        size5.add("Raw Mutton");
        size5.add("Raw Pork");
        size5.add("Raw Horse");
        size5.add("Raw ");
        size5.add("Crab Meat");
        size5.add("Poultry");
        size5.add("Soil");
        size5.add("Nuts");

        catSize.put(size5,5);

        HashSet<String> size10 = new HashSet<>();
        size10.add("Nugget of a Precious Metal");
        size10.add("Nugget of Bronze, Iron or Steel");
        size10.add("Nugget of Any Common Metal");
        size10.add("Nugget of Any Metal");
        catSize.put(size10,10);
    }

    static
    {
        catExceptions.add("Moose Antlers");
        catExceptions.add("Red Deer Antlers");
        catExceptions.add("Reindeer Antlers");
        catExceptions.add("Roe Deer Antlers");
        catExceptions.add("Wolf's Claw");
        catExceptions.add("Lynx Claws");
        catExceptions.add("Silkworm");
        catExceptions.add("Female Silkmoth");
        catExceptions.add("Male Silkmoth");
    }
    private static final NAlias unstackableContainers = new NAlias("Smith's Smelter", "Ore Smelter", "Herbalist Table", "Tub", "Oven", "Steelbox", "Frame", "Kiln", "Smoke Shed");
    public static boolean isStackable(NInventory inv, String name)
    {
        Window win = inv.getparent(Window.class);
        if(win!=null)
        {
            if(NParser.checkName(win.cap,unstackableContainers) || NParser.checkName(name, new NAlias("Lynx Claws")) || name.equals("Silkworm") || catExceptions.contains(name))
            {
                return false;
            }
            else
            {
                ArrayList<String> categories = VSpec.getCategory(name);
                for(String cat: categories)
                {
                    for(HashSet<String> set: catSize.keySet())
                    {
                        if(set.contains(cat))
                        {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public static int getMaxStackSize(String name)
    {
        // Check custom stack sizes first
        if(customStackSizes.containsKey(name))
        {
            return customStackSizes.get(name);
        }
        
        if(catExceptions.contains(name))
        {
            return 1;
        }
        ArrayList<String> categories = VSpec.getCategory(name);
        int maxSize = 1;
        for(String cat: categories)
        {
            for(HashSet<String> set: catSize.keySet())
            {
                if(set.contains(cat))
                {
                    int size = catSize.get(set);
                    if (size > maxSize) {
                        maxSize = size;
                    }
                }
            }
        }
        return maxSize;
    }

    public static boolean isSameExist(NAlias items, NInventory inv) throws InterruptedException {
        if(items.keys.size()>1)
        {
            return false;
        }
        ArrayList<String> categories = VSpec.getCategory(items.getDefault());
        if(categories.contains("Hide Fresh"))
            categories.add("Prepared Animal Hide");
        else if(categories.contains("Prepared Animal Hide"))
            categories.add("Hide Fresh");
        for(String cat: categories)
        {
            ArrayList<String> categoryContent = new ArrayList<>(VSpec.getCategoryContent(cat));
            categoryContent.removeAll(items.keys);
            if(!categoryContent.isEmpty())
            {
                NAlias same = new NAlias(categoryContent);
                if(!inv.getItems(same).isEmpty())
                    return true;
            }
        }
        return false;
    }

    /**
     * Universal method to calculate optimal item capacity considering stacking
     * @param inventory The target inventory
     * @param itemName Name of the item to be placed
     * @param itemSize Size coordinate of the item
     * @param targetCount Maximum desired count
     * @return Optimal number of items that can fit, considering stacking
     */
    public static int getOptimalItemCapacity(NInventory inventory, String itemName, Coord itemSize, int targetCount) throws InterruptedException {
        // Get base free slots
        int freeSlots = inventory.getNumberFreeCoord(itemSize);

        // Check if stacking is globally enabled AND item is stackable in this inventory
        if (((NInventory) NUtils.getGameUI().maininv).bundle.a && isStackable(inventory, itemName)) {
            int maxStackSize = getMaxStackSize(itemName);
            int maxCapacity = freeSlots * maxStackSize;
            return Math.min(targetCount, maxCapacity);
        }

        // Stacking disabled or item not stackable - use original logic
        return Math.min(targetCount, freeSlots);
    }
}
