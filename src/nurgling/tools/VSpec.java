package nurgling.tools;

import nurgling.*;
import org.json.*;

import java.util.*;

public class VSpec
{
    public static HashMap<String,ArrayList<JSONObject>> categories = new HashMap<>();
    static {
        ArrayList<JSONObject> spices = new ArrayList<>();
        spices.add(new JSONObject("{\"static\":\"gfx/invobjs/herbs/kvann\",\"name\":\"Kvann\"}"));
        spices.add(new JSONObject("{\"static\":\"gfx/invobjs/seed-juniper\",\"name\":\"Juniper Berries\"}"));
        spices.add(new JSONObject("{\"static\":\"gfx/invobjs/herbs/chives\",\"name\":\"Chives\"}"));
        spices.add(new JSONObject("{\"static\":\"gfx/invobjs/leaf-laurel\",\"name\":\"Laurel Leaves\"}"));
        spices.add(new JSONObject("{\"static\":\"gfx/invobjs/herbs/salvia\",\"name\":\"Sage\"}"));
        spices.add(new JSONObject("{\"static\":\"gfx/invobjs/herbs/thyme\",\"name\":\"Thyme\"}"));
        spices.add(new JSONObject("{\"static\":\"gfx/invobjs/herbs/dill\",\"name\":\"Dill\"}"));

        categories.put("Spices", spices);

        ArrayList<JSONObject> tubers = new ArrayList<>();
        tubers.add(new JSONObject("{\"static\":\"gfx/invobjs/beet\",\"name\":\"Beetroot\"}"));
        tubers.add(new JSONObject("{\"static\":\"gfx/invobjs/carrot\",\"name\":\"Carrot\"}"));
        tubers.add(new JSONObject("{\"static\":\"gfx/invobjs/turnip\",\"name\":\"Turnip\"}"));
        categories.put("Tuber", tubers);

        ArrayList<JSONObject> onions = new ArrayList<>();
        onions.add(new JSONObject("{\"static\":\"gfx/invobjs/yellowonion\",\"name\":\"Yellow Onion\"}"));
        onions.add(new JSONObject("{\"static\":\"gfx/invobjs/redonion\",\"name\":\"Red Onion\"}"));
        onions.add(new JSONObject("{\"static\":\"gfx/invobjs/leek\",\"name\":\"Leek\",\"x\":2,\"y\":1}"));
        categories.put("Onion", onions);


        ArrayList<JSONObject> strings = new ArrayList<>();
        strings.add(new JSONObject("{\"static\":\"gfx/invobjs/flaxfibre\",\"name\":\"Flax Fibres\"}"));
        strings.add(new JSONObject("{\"static\":\"gfx/invobjs/hempfibre\",\"name\":\"Hemp Fibres\"}"));
        strings.add(new JSONObject("{\"static\":\"gfx/invobjs/herbs/spindlytaproot\",\"name\":\"Spindly Taproot\"}"));
        strings.add(new JSONObject("{\"static\":\"gfx/invobjs/herbs/cattailfibre\",\"name\":\"Cattail Fibres\"}"));
        strings.add(new JSONObject("{\"static\":\"gfx/invobjs/herbs/stingingnettle\",\"name\":\"Stinging Nettle\"}"));
        strings.add(new JSONObject("{\"static\":\"gfx/invobjs/hidestrap\",\"name\":\"Hide Strap\"}"));
        strings.add(new JSONObject("{\"static\":\"gfx/invobjs/strawstring\",\"name\":\"Straw Twine\"}"));
        strings.add(new JSONObject("{\"static\":\"gfx/invobjs/barkcordage\",\"name\":\"Bark Cordage\"}"));
        categories.put("String", strings);

        ArrayList<JSONObject> salads = new ArrayList<>();
        salads.add(new JSONObject("{\"static\":\"gfx/invobjs/beetleaves\",\"name\":\"Beetroot Leaves\"}"));
        salads.add(new JSONObject("{\"static\":\"gfx/invobjs/lettuceleaf\",\"name\":\"Lettuce Leaf\"}"));

        ArrayList<JSONObject> eggs = new ArrayList<>();
        eggs.add(new JSONObject("{\"static\":\"gfx/invobjs/egg-chicken\",\"name\":\"Chicken Egg\"}"));

        categories.put("Salad Greens", salads);
        categories.put("Crop Seeds", new ArrayList<>());
        categories.put("Egg", eggs);
        categories.put("Raw", new ArrayList<>());
        categories.put("Edible Mushroom", new ArrayList<>());
        categories.put("Solid Fat", new ArrayList<>());
    }

    public static HashMap<NStyle.Container, Integer> chest_state = new HashMap<>();
    static
    {
        chest_state.put(NStyle.Container.FREE, 3);
        chest_state.put(NStyle.Container.FULL, 28);
    }
}
