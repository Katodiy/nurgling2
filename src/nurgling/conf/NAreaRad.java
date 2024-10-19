package nurgling.conf;

import nurgling.NConfig;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class NAreaRad implements JConf
{
    public String name;
    public boolean vis;
    public int radius;

    public NAreaRad(String name, int radius) {
        this.name = name;
        this.vis = true;
        this.radius = radius;
    }

    public NAreaRad(HashMap<String, Object> values)
    {
        name = (String) values.get("name");
        if (values.get("vis") != null)
            vis = (Boolean) values.get("vis");
        if (values.get("radius") != null)
            radius = (Integer) values.get("radius");
    }

    @Override
    public JSONObject toJson()
    {
        JSONObject jobj = new JSONObject();
        jobj.put("type", "NAreaRad");
        jobj.put("name", name);
        jobj.put("vis", vis);
        jobj.put("radius", radius);
        return jobj;
    }

    public static NAreaRad get(String val)
    {
        ArrayList<NAreaRad> radProps = ((ArrayList<NAreaRad>) NConfig.get(NConfig.Key.animalrad));
        if (radProps == null)
            radProps = new ArrayList<>();
        for (NAreaRad prop : radProps)
        {
            if (prop.name.equals(val))
            {
                return prop;
            }
        }
        return null;
    }

    public static void set(String val, NAreaRad prop)
    {
        ArrayList<NAreaRad> radProps = ((ArrayList<NAreaRad>) NConfig.get(NConfig.Key.animalrad));
        if (radProps != null)
        {
            for (Iterator<NAreaRad> i = radProps.iterator(); i.hasNext(); )
            {
                NAreaRad oldprop = i.next();
                if (oldprop.name.equals(prop.name))
                {
                    i.remove();
                    break;
                }
            }

        }
        else
        {
            radProps = new ArrayList<>();
        }
        radProps.add(prop);
        NConfig.set(NConfig.Key.animalrad, radProps);
    }
}
