package nurgling.conf;

import nurgling.NConfig;
import nurgling.NUI;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;


public class NWorldExplorerProp implements JConf
{
    final private String username;
    final private String chrid;
    public boolean clockwise = false;
    public boolean deeper = true;

    public NWorldExplorerProp(String username, String chrid) {
        this.username = username;
        this.chrid = chrid;
    }

    public NWorldExplorerProp(HashMap<String, Object> values)
    {
        chrid = (String) values.get("chrid");
        username = (String) values.get("username");
        if (values.get("clockwise") != null)
            clockwise = (Boolean) values.get("clockwise");
        if (values.get("deeper") != null)
            deeper = (Boolean) values.get("deeper");

    }

    public static void set(NWorldExplorerProp prop)
    {
        ArrayList<NWorldExplorerProp> explorerProps = ((ArrayList<NWorldExplorerProp>) NConfig.get(NConfig.Key.worldexplorerprop));
        if (explorerProps != null)
        {
            for (Iterator<NWorldExplorerProp> i = explorerProps.iterator(); i.hasNext(); )
            {
                NWorldExplorerProp oldprop = i.next();
                if(oldprop.username.equals(prop.username) && oldprop.chrid.equals(prop.chrid))
                {
                    i.remove();
                    break;
                }
            }

        }
        else
        {
            explorerProps = new ArrayList<>();
        }
        explorerProps.add(prop);
        NConfig.set(NConfig.Key.worldexplorerprop, explorerProps);
    }

    @Override
    public String toString()
    {
        return "NWorldExplorer[" + username + "|" + chrid + "]";
    }

    @Override
    public JSONObject toJson()
    {
        JSONObject jexplorer = new JSONObject();
        jexplorer.put("type", "NWorldExplorer");
        jexplorer.put("username", username);
        jexplorer.put("chrid", chrid);
        jexplorer.put("clockwise", clockwise);
        jexplorer.put("deeper", deeper);
        return jexplorer;
    }

    public static NWorldExplorerProp get(NUI.NSessInfo sessInfo)
    {
        ArrayList<NWorldExplorerProp> worldexpProps = ((ArrayList<NWorldExplorerProp>) NConfig.get(NConfig.Key.worldexplorerprop));
        if (worldexpProps == null)
            worldexpProps = new ArrayList<>();
        for (NWorldExplorerProp prop : worldexpProps)
        {
            if (prop.username.equals(sessInfo.username) && prop.chrid.equals(sessInfo.characterInfo.chrid))
            {
                return prop;
            }
        }
        return new NWorldExplorerProp(sessInfo.username, sessInfo.characterInfo.chrid);
    }
}
