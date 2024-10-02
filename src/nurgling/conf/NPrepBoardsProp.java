package nurgling.conf;

import nurgling.NConfig;
import nurgling.NUI;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;


public class NPrepBoardsProp implements JConf
{
    final private String username;
    final private String chrid;
    public String tool = null;

    public NPrepBoardsProp(String username, String chrid) {
        this.username = username;
        this.chrid = chrid;
    }

    public NPrepBoardsProp(HashMap<String, Object> values)
    {
        chrid = (String) values.get("chrid");
        username = (String) values.get("username");
        if (values.get("tool") != null)
            tool = (String) values.get("tool");
    }

    public static void set(NPrepBoardsProp prop)
    {
        ArrayList<NPrepBoardsProp> prepboardsProps = ((ArrayList<NPrepBoardsProp>) NConfig.get(NConfig.Key.prepboardprop));
        if (prepboardsProps != null)
        {
            for (Iterator<NPrepBoardsProp> i = prepboardsProps.iterator(); i.hasNext(); )
            {
                NPrepBoardsProp oldprop = i.next();
                if(oldprop.username.equals(prop.username) && oldprop.chrid.equals(prop.chrid))
                {
                    i.remove();
                    break;
                }
            }

        }
        else
        {
            prepboardsProps = new ArrayList<>();
        }
        prepboardsProps.add(prop);
        NConfig.set(NConfig.Key.prepboardprop, prepboardsProps);
    }

    @Override
    public String toString()
    {
        return "NPrepBoardProp[" + username + "|" + chrid + "]";
    }

    @Override
    public JSONObject toJson()
    {
        JSONObject jprepboards = new JSONObject();
        jprepboards.put("type", "NPrepBoardProp");
        jprepboards.put("username", username);
        jprepboards.put("chrid", chrid);
        jprepboards.put("tool", tool);
        return jprepboards;
    }

    public static NPrepBoardsProp get(NUI.NSessInfo sessInfo)
    {
        ArrayList<NPrepBoardsProp> chopProps = ((ArrayList<NPrepBoardsProp>) NConfig.get(NConfig.Key.prepboardprop));
        if (chopProps == null)
            chopProps = new ArrayList<>();
        for (NPrepBoardsProp prop : chopProps)
        {
            if (prop.username.equals(sessInfo.username) && prop.chrid.equals(sessInfo.characterInfo.chrid))
            {
                return prop;
            }
        }
        return new NPrepBoardsProp(sessInfo.username, sessInfo.characterInfo.chrid);
    }
}
