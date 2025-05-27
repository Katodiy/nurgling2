package nurgling.tools;

import haven.Window;
import nurgling.NInventory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class StackSupporter {
    private static final HashMap<HashSet<String>,Integer> catSize = new HashMap<>();
    static {
        HashSet<String> size3 = new HashSet<>();
        size3.add("Spices");
        size3.add("Tuber");
        size3.add("Onion");
        size3.add("String");
        size3.add("Salad Greens");
        size3.add("Malted Grains");
        size3.add("Millable Seed");
        size3.add("Crop Seeds");
        size3.add("Egg");
        size3.add("Gellant");
        size3.add("Stuffing");
        size3.add("Yarn");
        size3.add("Dried Fruit");
        size3.add("Edible Mushroom");
        size3.add("Nuts");
        size3.add("Decent-sized Conifer Cone");
        size3.add("Berry");
        size3.add("Fruit");
        size3.add("Fruit or Berry");
        size3.add("Flour");
        size3.add("Giant Ant");
        size3.add("Royal Ant");
        size3.add("Bug");
        size3.add("Fishline");
        size3.add("Sweetener");
        size3.add("Leaf");
        size3.add("Tree Bough");
        size3.add("Thatching Material");
        size3.add("Vegetable Oil");
        size3.add("Flower");
        size3.add("Solid Fat");
        size3.add("Cured Tea");
        size3.add("Snail");
        size3.add("Clean Animal Carcass");
        size3.add("Edible Seashell");
        size3.add("Poultry");

        size3.add("Candle");
        size3.add("Pearl");
        size3.add("Wool");
        size3.add("Finer Plant Fibre");
        size3.add("Wicker");
        size3.add("Cloth");
        size3.add("Nugget of a Precious Metal");
        size3.add("Coal");
        size3.add("Nugget of Bronze, Iron or Steel");
        size3.add("Nugget of Any Common Metal");
        size3.add("Nugget of Any Metal");
        size3.add("Wax");
        size3.add("Pigment");
        size3.add("Fine Clay");
        size3.add("Any Brick");
        size3.add("Clay");
        size3.add("Casting Material");
        size3.add("Glass");
        size3.add("Hide Fresh");
        size3.add("Prepared Animal Hide");
        size3.add("Board");
        size3.add("Block of Wood");
        size3.add("Ore");
        size3.add("Stone");
        size3.add("Seed of Tree or Bush");
        size3.add("Fish Fresh Water");
        size3.add("Fish Ocean");
        size3.add("Fish Cave");
        size3.add("Fish");
        size3.add("Lures");
        size3.add("Hooks");
        size3.add("Dried Fish");
        size3.add("Medicine");
        catSize.put(size3,3);

        HashSet<String> size5 = new HashSet<>();
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

        catSize.put(size5,5);
    }
    private static final NAlias unstackableContainers = new NAlias("Smith's Smelter", "Ore Smelter", "Herbalist Table", "Tanning Tub", "Oven", "Steelbox");
    public static boolean isStackable(NInventory inv, String name)
    {
        Window win = inv.getparent(Window.class);
        if(win!=null)
        {
            if(NParser.checkName(win.cap,unstackableContainers))
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
        ArrayList<String> categories = VSpec.getCategory(name);
        for(String cat: categories)
        {
            for(HashSet<String> set: catSize.keySet())
            {
                if(set.contains(cat))
                {
                    return catSize.get(set);
                }
            }
        }
        return 1;
    }
}
