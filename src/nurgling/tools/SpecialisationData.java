package nurgling.tools;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class SpecialisationData {
    public final static HashMap<String, ArrayList<String>> data = new HashMap<>();
    static
    {
        ArrayList<String> crops = new ArrayList<>(Arrays.asList("Flax", "Turnip", "Carrot", "Hemp", "Millet", "Wheat", "Barley", "Poppy"));
        data.put("crop",crops);
        data.put("seed",crops);
        ArrayList<String> fuel = new ArrayList<>(Arrays.asList("Branch", "Coal", "Block", "Log"));
        data.put("fuel",fuel);
        ArrayList<String> htable = new ArrayList<>(Arrays.asList("Pipeweed", "Green Tea Leaves", "Black Tea Leaves"));
        data.put("htable",htable);
        ArrayList<String> barrel = new ArrayList<>(Arrays.asList("Quicksilver", "Honey"));
        data.put("barrel",barrel);
    }
}
