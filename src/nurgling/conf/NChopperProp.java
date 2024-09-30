package nurgling.conf;

import nurgling.NConfig;
import nurgling.NUI;
import nurgling.NUtils;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;


public class NChopperProp implements JConf
{
    final private String username;
    final private String chrid;
    public String tool = null;
    public String shovel = null;
    public boolean autoeat = false;
    public boolean autorefill = false;
    public boolean ngrowth = false;
    public boolean stumps = false;
    public boolean bushes = false;

    public NChopperProp(String username, String chrid) {
        this.username = username;
        this.chrid = chrid;
    }

    public NChopperProp(HashMap<String, Object> values)
    {
        chrid = (String) values.get("chrid");
        username = (String) values.get("username");
        if (values.get("tool") != null)
            tool = (String) values.get("tool");
        if (values.get("shovel") != null)
            shovel = (String) values.get("shovel");
        if (values.get("autoeat") != null)
            autoeat = (Boolean) values.get("autoeat");
        if (values.get("autorefill") != null)
            autorefill = (Boolean) values.get("autorefill");
        if (values.get("ngrowth") != null)
            ngrowth = (Boolean) values.get("ngrowth");
        if (values.get("stumps") != null)
            stumps = (Boolean) values.get("stumps");
        if (values.get("bushes") != null)
            bushes = (Boolean) values.get("bushes");
    }

    public static void set(NChopperProp prop)
    {
        ArrayList<NChopperProp> chopperProps = ((ArrayList<NChopperProp>) NConfig.get(NConfig.Key.chopperprop));
        if (chopperProps != null)
        {
            for (Iterator<NChopperProp> i = chopperProps.iterator(); i.hasNext(); )
            {
                NChopperProp oldprop = i.next();
                if(oldprop.username.equals(prop.username) && oldprop.chrid.equals(prop.chrid))
                {
                    i.remove();
                    break;
                }
            }

        }
        else
        {
            chopperProps = new ArrayList<>();
        }
        chopperProps.add(prop);
        NConfig.set(NConfig.Key.chopperprop, chopperProps);
    }

    @Override
    public String toString()
    {
        return "NChopperProp[" + username + "|" + chrid + "]";
    }

    @Override
    public JSONObject toJson()
    {
        JSONObject jchopper = new JSONObject();
        jchopper.put("type", "NChopperProp");
        jchopper.put("username", username);
        jchopper.put("chrid", chrid);
        jchopper.put("tool", tool);
        jchopper.put("shovel", shovel);
        jchopper.put("autoeat", autoeat);
        jchopper.put("autorefill", autorefill);
        jchopper.put("ngrowth", ngrowth);
        jchopper.put("stumps", stumps);
        jchopper.put("bushes", bushes);
        return jchopper;
    }

    public static NChopperProp get(NUI.NSessInfo sessInfo)
    {
        ArrayList<NChopperProp> chopProps = ((ArrayList<NChopperProp>) NConfig.get(NConfig.Key.chopperprop));
        if (chopProps == null)
            chopProps = new ArrayList<>();
        for (NChopperProp prop : chopProps)
        {
            if (prop.username.equals(sessInfo.username) && prop.chrid.equals(sessInfo.characterInfo.chrid))
            {
                return prop;
            }
        }
        return new NChopperProp(sessInfo.username, sessInfo.characterInfo.chrid);
    }
}
