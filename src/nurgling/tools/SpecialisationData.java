package nurgling.tools;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class SpecialisationData {
    public final static HashMap<String, ArrayList<String>> data = new HashMap<>();
    static
    {
        ArrayList<String> crops = new ArrayList<>(Arrays.asList("Flax", "Turnip", "Carrot", "Hemp", "Millet", "Wheat", "Barley", "Poppy", "Beetroot", "Red Onion", "Yellow Onion", "Garlic", "Pipeweed", "Lettuce", "Pumpkin", "Green Kale", "Leek", "Grape", "Hops", "Peppercorn", "Pea", "Cucumber", "String Grass", "Wild Kale", "Wild Onion", "Wild Tuber", "Wild Gourd", "Wild Flower"));
        data.put("crop",crops);
        data.put("seed",crops);
        ArrayList<String> fuel = new ArrayList<>(Arrays.asList("Branch", "Coal", "Block", "Log"));
        data.put("fuel",fuel);
        ArrayList<String> htable = new ArrayList<>(Arrays.asList("Pipeweed", "Green Tea Leaves", "Black Tea Leaves", "Silkworm Egg", "Trees"));
        data.put("htable",htable);
        ArrayList<String> barrel = new ArrayList<>(Arrays.asList("Quicksilver", "Honey", "Pickling Brine"));
        data.put("barrel",barrel);
        data.put("cropQ",crops);
        data.put("seedQ",crops);
        ArrayList<String> cheeseRacks = new ArrayList<>(Arrays.asList("Inside", "Cellar", "Outside", "Mine"));
        data.put("cheeseRacks", cheeseRacks);
        ArrayList<String> cistern = new ArrayList<>(Arrays.asList("Cow Milk", "Goat Milk", "Sheep Milk"));
        data.put("cistern", cistern);
        ArrayList<String> picklingJars = new ArrayList<>(Arrays.asList("Beetroots", "Carrots", "Eggs", "Herring", "Olives", "Cucumbers", "Red Onion", "Yellow Onion"));
        data.put("picklingJars", picklingJars);
        ArrayList<String> gardenPotPlants = new ArrayList<>(Arrays.asList(
            "Blood Stern", "Blueberries", "Cavebulb", "Chantrelles", "Chiming Bluebell",
            "Chives", "Clover", "Dandelion", "Dill", "Heartsease", "Kvann",
            "Lady's Mantle", "Liberty Caps", "Lupine", "Parasol Mushroom", "Sage",
            "Stalagoom", "Stinging Nettle", "Strawberries", "Tansy", "Thyme",
            "Troll Mushroom", "Uncommon Snapdragon", "Yarrow"
        ));
        data.put("plantingGardenPots", gardenPotPlants);
        data.put("gardenPotSeeds", gardenPotPlants);
        ArrayList<String> dframe = new ArrayList<>(Arrays.asList("Hides", "Fish"));
        data.put("dframe", dframe);
        
        // Construction materials subtypes
        ArrayList<String> buildMaterials = new ArrayList<>(Arrays.asList(
            "Block", "Board", "Stone", "String", "Nugget", "Metal Bar", 
            "Clay", "Brick", "Thatch", "Branch", "Bough", "Log", "Fibre"
        ));
        data.put("buildMaterials", buildMaterials);
    }
}
