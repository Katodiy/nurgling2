package nurgling.tools;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class SpecialisationData {
    public final static HashMap<String, ArrayList<String>> data = new HashMap<>();
    static
    {
        ArrayList<String> crops = new ArrayList<>(Arrays.asList("Flax", "Turnip", "Carrot"));
        data.put("crop",crops);
        data.put("seed",crops);
    }
}
