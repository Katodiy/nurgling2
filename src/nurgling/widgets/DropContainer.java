package nurgling.widgets;

import haven.Coord;
import haven.TexI;
import haven.UI;
import haven.res.lib.itemtex.ItemTex;
import nurgling.NConfig;
import nurgling.NGItem;
import nurgling.NStyle;
import nurgling.NUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

public class DropContainer extends BaseIngredientContainer {

    JSONArray jitems = new JSONArray();

    public DropContainer() {
        super("drop");
    }

    public JSONArray getDropJson() {
        return jitems;
    }

    public static HashMap<String, Integer> getDropProps() {
        JSONArray data = new JSONArray((NConfig.get(NConfig.Key.dropConf) instanceof JSONArray)?(JSONArray)NConfig.get(NConfig.Key.dropConf):(ArrayList<HashMap<String,Object>>)NConfig.get(NConfig.Key.dropConf));
        HashMap<String, Integer> props = new HashMap<>();
        for (int i = 0; i < data.length(); i++) {
            JSONObject jsonObject = ((JSONObject)data.get(i));
            String name = jsonObject.getString("name");
            props.put(name,jsonObject.has("th")?jsonObject.getInt("th"):1 );
        }
        return props;
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

        jitems = new JSONArray((ArrayList<HashMap<String,Object>>) NConfig.get(NConfig.Key.dropConf));

        for (int i = 0; i < jitems.length(); i++) {
            addIcon(((JSONObject) jitems.get(i)));
        }

    }

    public void addIcon(JSONObject res) {
        if(res != null && res.get("name") != null) {
            Ingredient ing;
            items.add(ing = new Ingredient((String)res.get("name"), ItemTex.create(res)));
            IconItem it = add(new IconItem(ing.name, ing.image, this), UI.scale(new Coord(35*((items.size()-1)%5),51*((items.size()-1)/5))).add(new Coord(5,5)));
            it.basec = new Coord(it.c);
            maxy = UI.scale(51)*((items.size()-1)/5 - 5);
            cury = Math.min(cury, Math.max(maxy, 0));
            if(res.has("th")) {
                it.isThreshold = true;
                it.val = (Integer)res.get("th");
                it.q = new TexI(NStyle.iiqual.render(String.valueOf(it.val)).img);
            }
            icons.add(it);
        }
    }

    public void setThreshold(String name, int val) {
        for(int i = 0; i < jitems.length(); i++) {
            if(((JSONObject) jitems.get(i)).get("name").equals(name)) {
                ((JSONObject) jitems.get(i)).put("th",val);
                NConfig.needAreasUpdate();
                return;
            }
        }
    }
}