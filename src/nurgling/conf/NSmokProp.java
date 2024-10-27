package nurgling.conf;

import haven.res.lib.itemtex.ItemTex;
import nurgling.NConfig;
import nurgling.NUI;
import nurgling.widgets.IconItem;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
//import java.util.HexFormat;
import java.util.Iterator;


public class NSmokProp implements JConf
{
    boolean isSelected = false;
    public String iconName = null;
    ArrayList<String> layers = null;
    String res = null;
    public String fuel;

    public NSmokProp(IconItem iconItem, String fuel, boolean isSelected) {
        JSONObject jicon = iconItem.toJson();
        this.iconName = jicon.getString("name");
        if(jicon.has("layer")) {
            layers = new ArrayList<>();
            for(Object object : (JSONArray) jicon.get("layer")) {
                layers.add((String)object);
            }
        }
        if(jicon.has("res")) {
            res = jicon.getString("res");
        }
        this.fuel = fuel;
        this.isSelected = isSelected;
    }

    public NSmokProp(HashMap<String, Object> values) {
        HashMap<String, Object> data = (HashMap<String, Object>) values.get("data");
        HashMap<String, Object> icon = (HashMap<String, Object>) data.get("icon");
        iconName = icon.get("name").toString();
        if(icon.get("res")!=null) {
            res = icon.get("res").toString();
        }
        else
        {
            if(icon.get("layer")!=null) {
                layers = new ArrayList<>();
                for (Object object : (ArrayList<Object>) icon.get("layer")) {
                    layers.add((String) object);
                }
            }
        }
        fuel = (String) data.get("fuel");
        if (data.get("isSelected") != null)
            isSelected = (Boolean) data.get("isSelected");
    }

    public static void set(NSmokProp prop)
    {
        ArrayList<NSmokProp> smokeProps = ((ArrayList<NSmokProp>) NConfig.get(NConfig.Key.smokeprop));
        if (smokeProps == null) {
            smokeProps = new ArrayList<>();
        }
        String name = ((String)((JSONObject)((JSONObject)prop.toJson().get("data")).get("icon")).get("name"));
        for (Iterator<NSmokProp> i = smokeProps.iterator(); i.hasNext(); )
        {
            NSmokProp oldprop = i.next();
            if(((String)((JSONObject)((JSONObject)oldprop.toJson().get("data")).get("icon")).get("name")).startsWith(name))
            {
                i.remove();
                break;
            }
        }
        smokeProps.add(prop);
        NConfig.set(NConfig.Key.smokeprop, smokeProps);
    }

    @Override
    public String toString()
    {
        return "NSmokeProp[" + toJson().toString() + "]";
    }

    @Override
    public JSONObject toJson()
    {
        JSONObject data = new JSONObject();
        data.put("isSelected", isSelected);
        JSONObject icon = new JSONObject();
        icon.put("name", iconName);
        icon.put("type", "CONTAINER");
        if(res!=null)
        {
            icon.put("res", res);
        }
        else if(layers!=null)
        {
            icon.put("layer", layers);
        }
        data.put("icon", icon);
        data.put("fuel", fuel);

        JSONObject smokProp = new JSONObject();
        smokProp.put("data", data);
        smokProp.put("type", "NSmokeProp");
        return smokProp;

    }

}
