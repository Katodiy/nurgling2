package nurgling.conf;

import nurgling.NConfig;
import nurgling.NUI;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;


public class NClayDiggerProp implements JConf
{
    final private String username;
    final private String chrid;
    public String shovel = null;

    public NClayDiggerProp(String username, String chrid) {
        this.username = username;
        this.chrid = chrid;
    }

    public NClayDiggerProp(HashMap<String, Object> values)
    {
        chrid = (String) values.get("chrid");
        username = (String) values.get("username");
        if (values.get("shovel") != null)
            shovel = (String) values.get("shovel");
    }

    public static void set(NClayDiggerProp prop)
    {
        ArrayList<NClayDiggerProp> diggerProps = ((ArrayList<NClayDiggerProp>) NConfig.get(NConfig.Key.claydiggerprop));
        if (diggerProps != null)
        {
            for (Iterator<NClayDiggerProp> i = diggerProps.iterator(); i.hasNext(); )
            {
                NClayDiggerProp oldprop = i.next();
                if(oldprop.username.equals(prop.username) && oldprop.chrid.equals(prop.chrid))
                {
                    i.remove();
                    break;
                }
            }

        }
        else
        {
            diggerProps = new ArrayList<>();
        }
        diggerProps.add(prop);
        NConfig.set(NConfig.Key.claydiggerprop, diggerProps);
    }

    @Override
    public String toString()
    {
        return "NClayDiggerProp[" + username + "|" + chrid + "]";
    }

    @Override
    public JSONObject toJson()
    {
        JSONObject jchopper = new JSONObject();
        jchopper.put("type", "NClayDiggerProp");
        jchopper.put("username", username);
        jchopper.put("chrid", chrid);
        jchopper.put("shovel", shovel);
        return jchopper;
    }

    public static NClayDiggerProp get(NUI.NSessInfo sessInfo)
    {
        ArrayList<NClayDiggerProp> chopProps = ((ArrayList<NClayDiggerProp>) NConfig.get(NConfig.Key.claydiggerprop));
        if (chopProps == null)
            chopProps = new ArrayList<>();
        for (NClayDiggerProp prop : chopProps)
        {
            if (prop.username.equals(sessInfo.username) && prop.chrid.equals(sessInfo.characterInfo.chrid))
            {
                return prop;
            }
        }
        return new NClayDiggerProp(sessInfo.username, sessInfo.characterInfo.chrid);
    }
}
