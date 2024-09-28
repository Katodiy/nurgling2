package nurgling.conf;

import nurgling.NConfig;
import nurgling.NUI;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;


public class NPrepBlocksProp implements JConf
{
    final private String username;
    final private String chrid;
    public String tool = null;

    public NPrepBlocksProp(String username, String chrid) {
        this.username = username;
        this.chrid = chrid;
    }

    public NPrepBlocksProp(HashMap<String, Object> values)
    {
        chrid = (String) values.get("chrid");
        username = (String) values.get("username");
        if (values.get("tool") != null)
            tool = (String) values.get("tool");
    }

    public static void set(NPrepBlocksProp prop)
    {
        ArrayList<NPrepBlocksProp> prepblocksProps = ((ArrayList<NPrepBlocksProp>) NConfig.get(NConfig.Key.prepblockprop));
        if (prepblocksProps != null)
        {
            for (Iterator<NPrepBlocksProp> i = prepblocksProps.iterator(); i.hasNext(); )
            {
                NPrepBlocksProp oldprop = i.next();
                if(oldprop.username.equals(prop.username) && oldprop.chrid.equals(prop.chrid))
                {
                    i.remove();
                    break;
                }
            }

        }
        else
        {
            prepblocksProps = new ArrayList<>();
        }
        prepblocksProps.add(prop);
        NConfig.set(NConfig.Key.prepblockprop, prepblocksProps);
    }

    @Override
    public String toString()
    {
        return "NPrepBProp[" + username + "|" + chrid + "]";
    }

    @Override
    public JSONObject toJson()
    {
        JSONObject jprepblocks = new JSONObject();
        jprepblocks.put("type", "NPrepBProp");
        jprepblocks.put("username", username);
        jprepblocks.put("chrid", chrid);
        jprepblocks.put("tool", tool);
        return jprepblocks;
    }

    public static NPrepBlocksProp get(NUI.NSessInfo sessInfo)
    {
        ArrayList<NPrepBlocksProp> chopProps = ((ArrayList<NPrepBlocksProp>) NConfig.get(NConfig.Key.prepblockprop));
        if (chopProps == null)
            chopProps = new ArrayList<>();
        for (NPrepBlocksProp prop : chopProps)
        {
            if (prop.username.equals(sessInfo.username) && prop.chrid.equals(sessInfo.characterInfo.chrid))
            {
                return prop;
            }
        }
        return new NPrepBlocksProp(sessInfo.username, sessInfo.characterInfo.chrid);
    }
}
