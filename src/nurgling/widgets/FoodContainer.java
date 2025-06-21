package nurgling.widgets;

import haven.*;
import haven.res.lib.itemtex.*;
import nurgling.*;
import org.json.*;

import java.awt.image.*;
import java.util.*;

public class FoodContainer extends BaseIngredientContainer {

    JSONArray jitems = new JSONArray();

    public FoodContainer() {
        super("food");
    }

    public JSONArray getFoodsJson() {
        return jitems;
    }

    public static ArrayList<String> getFoodNames() {
        JSONArray data = new JSONArray((NConfig.get(NConfig.Key.eatingConf) instanceof JSONArray)?(JSONArray)NConfig.get(NConfig.Key.eatingConf):(ArrayList<HashMap<String,Object>>)NConfig.get(NConfig.Key.eatingConf));
        ArrayList<String> names = new ArrayList<>();
        for (int i = 0; i < data.length(); i++) {
            String name = ((JSONObject)data.get(i)).getString("name");
            if(name.contains("Sizzling")) {
                name = name.replace("Sizzling ", "");
            }
            if(name.contains("Spitroast"))
            {
                names.add("Sizzling " + name);
            }
            names.add(name);
        }
        return names;
    }

    @Override
    public void addItem(String name, JSONObject res) {
        if (res != null) {
            res.put("name", name);
            addIcon(res);
            jitems.put(res);
        }
    }

    @Override
    public void delete(String name) {
        super.delete(name);
        for(int i = 0; i < jitems.length(); i++) {
            if (((JSONObject)jitems.get(i)).get("name").equals(name)) {
                jitems.remove(i);
                break;
            }
        }
        items.clear();
        for(IconItem it : icons) {
            it.destroy();
        }
        icons.clear();
        for (int i = 0; i < jitems.length(); i++) {
            addIcon(((JSONObject) jitems.get(i)));
        }
    }

    @Override
    public void deleteAll() {
        super.deleteAll();
        jitems.clear();
    }

    @Override
    public boolean drop(Drop ev) {
        String name = ((NGItem) ev.src.item).name();
        JSONObject res = ItemTex.save(((NGItem) ev.src.item).spr);
        addItem(name, res);
        return super.drop(ev);
    }

    public void load() {
        items.clear();
        for(IconItem it : icons) {
            it.destroy();
        }
        icons.clear();
        jitems.clear();

        jitems = new JSONArray((ArrayList<HashMap<String,Object>>) NConfig.get(NConfig.Key.eatingConf));

        for (int i = 0; i < jitems.length(); i++) {
            addIcon(((JSONObject) jitems.get(i)));
        }

    }
}