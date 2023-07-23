package nurgling.conf;

import haven.*;
import nurgling.*;
import nurgling.tools.*;
import org.json.*;

import java.util.*;

public class NResizeProp implements JConf
{
    public Coord sz;
    public String name;

    public NResizeProp(Coord sz, String name)
    {
        this.sz = sz;
        this.name = name;
    }

    public NResizeProp(HashMap<String, Object> values)
    {
        name = (String) values.get("name");
        if (values.get("coord") != null)
            sz = NParser.str2coord((String) values.get("coord"));
          }

    @Override
    public JSONObject toJson()
    {
        JSONObject jobj = new JSONObject();
        jobj.put("type", "NResizeProp");
        jobj.put("name", name);
        jobj.put("coord", sz.toString());
        return jobj;
    }

    public static NResizeProp get(String val)
    {
        ArrayList<NResizeProp> resizeProps = ((ArrayList<NResizeProp>) NConfig.get(NConfig.Key.resizeprop));
        if (resizeProps == null)
            resizeProps = new ArrayList<>();
        for (NResizeProp prop : resizeProps)
        {
            if (prop.name.equals(val))
            {
                return prop;
            }
        }
        return new NResizeProp(Coord.z, val);
    }

    public static void set(String val, NResizeProp prop)
    {
        ArrayList<NResizeProp> resizeProps = ((ArrayList<NResizeProp>) NConfig.get(NConfig.Key.resizeprop));
        if (resizeProps != null)
        {
            for (Iterator<NResizeProp> i = resizeProps.iterator(); i.hasNext(); )
            {
                NResizeProp oldprop = i.next();
                if (oldprop.name.equals(prop.name))
                {
                    i.remove();
                    break;
                }
            }

        }
        else
        {
            resizeProps = new ArrayList<>();
        }
        resizeProps.add(prop);
        NConfig.set(NConfig.Key.resizeprop, resizeProps);
    }

    public static Coord find(String name)
    {
        ArrayList<NResizeProp> resizeProps = ((ArrayList<NResizeProp>) NConfig.get(NConfig.Key.resizeprop));
        if (resizeProps == null)
            resizeProps = new ArrayList<>();
        for (NResizeProp prop : resizeProps)
        {
            if (prop.name.equals(name))
            {
                return prop.sz;
            }
        }
        return null;
    }
}
