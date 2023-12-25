package nurgling.conf;

import haven.*;
import nurgling.*;
import nurgling.tools.*;
import org.json.*;

import java.util.*;


public class NKinProp implements JConf
{

    public boolean alarm = false;
    public boolean arrow = false;
    public boolean ring = false;
    public boolean hideinlist = false;
    public int id;

    public NKinProp( int id, boolean alarm, boolean arrow, boolean ring, boolean hil)
    {
        this.alarm = alarm;
        this.arrow = arrow;
        this.ring = ring;
        this.hideinlist = hil;
        this.id = id;
    }

    public NKinProp(HashMap<String, Object> values)
    {
        id = (Integer) values.get("id");
        if (values.get("alarm") != null)
            alarm = (Boolean) values.get("alarm");
        if (values.get("arrow") != null)
            arrow = (Boolean) values.get("arrow");
        if (values.get("ring") != null)
            ring = (Boolean) values.get("ring");
        if (values.get("hideinlist") != null)
            hideinlist = (Boolean) values.get("hideinlist");
   }

    public static void set(NKinProp prop)
    {
        ArrayList<NKinProp> kinProps = ((ArrayList<NKinProp>) NConfig.get(NConfig.Key.kinprop));
        if (kinProps != null)
        {
            for (Iterator<NKinProp> i = kinProps.iterator(); i.hasNext(); )
            {
                NKinProp oldprop = i.next();
                if (oldprop.id == prop.id)
                {
                    i.remove();
                    break;
                }
            }

        }
        else
        {
            kinProps = new ArrayList<>();
        }
        kinProps.add(prop);
        NConfig.set(NConfig.Key.kinprop, kinProps);
    }

    @Override
    public String toString()
    {
        return "NKinProp[" + id + "," + arrow + "," + ring + "," + alarm + "," + hideinlist + "]";
    }

    @Override
    public JSONObject toJson()
    {
        JSONObject jkin = new JSONObject();
        jkin.put("type", "NKinProp");
        jkin.put("id", id);
        jkin.put("alarm", alarm);
        jkin.put("arrow", arrow);
        jkin.put("ring", ring);
        jkin.put("hideinlist", hideinlist);
        return jkin;
    }

    public static NKinProp get(int val)
    {
        ArrayList<NKinProp> kinProps = ((ArrayList<NKinProp>) NConfig.get(NConfig.Key.kinprop));
        if (kinProps == null)
            kinProps = new ArrayList<>();
        for (NKinProp prop : kinProps)
        {
            if (prop.id == val)
            {
                return prop;
            }
        }
        return new NKinProp(val, false, false, false, false);
    }
}
