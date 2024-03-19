package nurgling.conf;

import haven.*;
import nurgling.*;
import org.json.*;

import java.awt.event.*;
import java.util.*;

public class NToolBeltProp implements JConf
{
    String name;
    boolean isVertiacal = false;

    ArrayList<KeyBinding> kb = new ArrayList<>(Arrays.asList(new KeyBinding[12]));
    public HashMap<Integer, String> custom = new HashMap<>();
    public ArrayList<KeyBinding> getKb()
    {
        return kb;
    }

    public NToolBeltProp(String name, boolean isVertiacal, ArrayList<KeyBinding> kb)
    {
        this.kb = kb;
        this.isVertiacal = isVertiacal;
        this.name = name;
        for(int i = 0 ; i < kb.size() ; i ++)
        {
            if(this.kb.get(i)==null)
                this.kb.set(i, KeyBinding.get (name + i, KeyMatch.nil));
        }
    }

    public NToolBeltProp(HashMap<String, Object> values)
    {
        name = (String) values.get("name");
        if (values.get("isVertiacal") != null)
            isVertiacal = (Boolean) values.get("isVertiacal");
        int i = 0;
        for(HashMap<String, Object> obj: (ArrayList<HashMap<String, Object>>)values.get("kb"))
        {
            if (obj.get("code") != null)
            {
                kb.set(i, KeyBinding.get (name + i, KeyMatch.forcode((Integer) obj.get("code"),(Integer) obj.get("mod"))));
            }
            else
            {
                kb.set(i, KeyBinding.get (name + i, KeyMatch.nil));
            }
            i++;
        }

        if(values.get("custom")!=null) {
            for (HashMap<String, Object> obj : (ArrayList<HashMap<String, Object>>) values.get("custom")) {
                if (obj.get("path") != null) {
                    custom.put((Integer) obj.get("id"), (String) obj.get("path"));
                }
            }
        }
    }

    @Override
    public JSONObject toJson()
    {
        JSONObject jobj = new JSONObject();
        jobj.put("type", "NToolBeltProp");
        jobj.put("name", name);
        jobj.put("isVertiacal", isVertiacal);
        JSONArray jkba = new JSONArray();
        for(KeyBinding hotKey : kb)
        {
            JSONObject jkb = new JSONObject();
            if(hotKey.key!=null)
            {
                jkb.put("mod", hotKey.key.modmatch);
                jkb.put("code", hotKey.key.code);
            }
            jkba.put(jkb);
        }
        jobj.put("kb", jkba);

        if(!custom.isEmpty()) {
            JSONArray jcustoms = new JSONArray();
            for (Integer id : custom.keySet()) {
                JSONObject jcustom = new JSONObject();
                jcustom.put("id", id);
                jcustom.put("path", custom.get(id));
                jcustoms.put(jcustom);
            }
            jobj.put("custom", jcustoms);
        }

        return jobj;
    }

    public static NToolBeltProp get(String val)
    {
        ArrayList<NToolBeltProp> toolBelts = ((ArrayList<NToolBeltProp>) NConfig.get(NConfig.Key.toolbelts));
        if (toolBelts == null)
            toolBelts = new ArrayList<>();
        for (NToolBeltProp prop : toolBelts)
        {
            if (prop.name.equals(val))
            {
                return prop;
            }
        }
        NToolBeltProp res = new NToolBeltProp(val, false, new ArrayList<>(Arrays.asList(new KeyBinding[12])));
        toolBelts.add(res);
        NConfig.set(NConfig.Key.toolbelts,toolBelts);
        return res;
    }

    public static void set(String val, NToolBeltProp prop)
    {
        ArrayList<NToolBeltProp> toolBelts = ((ArrayList<NToolBeltProp>) NConfig.get(NConfig.Key.toolbelts));
        if (toolBelts != null)
        {
            for (Iterator<NToolBeltProp> i = toolBelts.iterator(); i.hasNext(); )
            {
                NToolBeltProp oldprop = i.next();
                if (oldprop.name.equals(prop.name))
                {
                    i.remove();
                    break;
                }
            }
        }
        else
        {
            toolBelts = new ArrayList<>();
        }
        toolBelts.add(prop);
        NConfig.set(NConfig.Key.toolbelts, toolBelts);
    }
}
