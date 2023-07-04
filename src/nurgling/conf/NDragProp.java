package nurgling.conf;

import haven.*;
import nurgling.*;
import nurgling.tools.*;
import org.json.*;

import java.util.*;

public class NDragProp implements JConf
{
    public Coord c;
    public boolean locked;
    public String name;
    public Coord2d pos_koef = Coord2d.z;

    public NDragProp(Coord c, boolean locked, String name)
    {
        this.c = c;
        this.locked = locked;
        this.name = name;
    }

    public NDragProp(Coord c, boolean locked, String name, Coord2d pos_koef)
    {
        this.c = c;
        this.locked = locked;
        this.name = name;
        this.pos_koef = pos_koef;
    }

    public NDragProp(HashMap<String, Object> values)
    {
        name = (String) values.get("name");
        if (values.get("locked") != null)
            locked = (Boolean) values.get("locked");
        if (values.get("coord") != null)
            c = NParser.str2coord((String) values.get("coord"));
        if (values.get("pos_koef") != null)
            pos_koef = NParser.str2coord2((String) values.get("pos_koef"));
    }

    @Override
    public JSONObject toJson()
    {
        JSONObject jobj = new JSONObject();
        jobj.put("type", "NDragProp");
        jobj.put("name", name);
        jobj.put("locked", locked);
        jobj.put("coord", c.toString());
        jobj.put("pos_koef", pos_koef.toString());
        return jobj;
    }

    public static NDragProp get(String val)
    {
        ArrayList<NDragProp> dragProps = ((ArrayList<NDragProp>) NConfig.get(NConfig.Key.dragprop));
        if (dragProps == null)
            dragProps = new ArrayList<>();
        for (NDragProp prop : dragProps)
        {
            if (prop.name.equals(val))
            {
                return prop;
            }
        }
        return new NDragProp(Coord.z, false, val);
    }

    public static void set(String val, NDragProp prop)
    {
        ArrayList<NDragProp> dragProps = ((ArrayList<NDragProp>) NConfig.get(NConfig.Key.dragprop));
        if (dragProps != null)
        {
            for (Iterator<NDragProp> i = dragProps.iterator(); i.hasNext(); )
            {
                NDragProp oldprop = i.next();
                if (oldprop.name.equals(prop.name))
                {
                    i.remove();
                    break;
                }
            }

        }
        else
        {
            dragProps = new ArrayList<>();
        }
        dragProps.add(prop);
        NConfig.set(NConfig.Key.dragprop, dragProps);
    }
}
