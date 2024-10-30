package nurgling.conf;

import nurgling.NConfig;
import nurgling.NUI;
import nurgling.widgets.bots.FishingTarget;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;


public class NFishingSettings implements JConf
{
    final private String username;
    final private String chrid;
    public String tool = null;
    public String fishline = null;
    public String bait = null;
    public String hook = null;
    public ArrayList<String> targets= new ArrayList<>();
    public boolean repfromcont = false;


    public NFishingSettings(String username, String chrid) {
        this.username = username;
        this.chrid = chrid;
    }

    public NFishingSettings(HashMap<String, Object> values)
    {
        chrid = (String) values.get("chrid");
        username = (String) values.get("username");
        if (values.get("tool") != null)
            tool = (String) values.get("tool");
        if (values.get("fishline") != null)
            fishline = (String) values.get("fishline");
        if (values.get("bait") != null)
            bait = (String) values.get("bait");
        if (values.get("hook") != null)
            hook = (String) values.get("hook");
        if (values.get("repfromcont") != null)
            repfromcont = (Boolean) values.get("repfromcont");
        if (values.get("targets") != null)
            targets.addAll((Collection<? extends String>)values.get("targets"));
    }

    public static void set(NFishingSettings prop)
    {
        ArrayList<NFishingSettings> fishSet = ((ArrayList<NFishingSettings>) NConfig.get(NConfig.Key.fishingsettings));
        if (fishSet != null)
        {
            for (Iterator<NFishingSettings> i = fishSet.iterator(); i.hasNext(); )
            {
                NFishingSettings oldprop = i.next();
                if(oldprop.username.equals(prop.username) && oldprop.chrid.equals(prop.chrid))
                {
                    i.remove();
                    break;
                }
            }

        }
        else
        {
            fishSet = new ArrayList<>();
        }
        fishSet.add(prop);
        NConfig.set(NConfig.Key.fishingsettings, fishSet);
    }

    @Override
    public String toString()
    {
        return "NFishingSettings[" + username + "|" + chrid + "]";
    }

    @Override
    public JSONObject toJson()
    {
        JSONObject jfishSet = new JSONObject();
        jfishSet.put("type", "NFishingSettings");
        jfishSet.put("username", username);
        jfishSet.put("chrid", chrid);
        jfishSet.put("tool", tool);
        jfishSet.put("fishline", fishline);
        jfishSet.put("bait", bait);
        jfishSet.put("hook", hook);
        jfishSet.put("repfromcont", repfromcont);
        JSONArray fish = new JSONArray();
        for(String key : targets)
        {
            fish.put(key);
        }
        jfishSet.put("targets", fish);

        return jfishSet;
    }

    public static NFishingSettings get(NUI.NSessInfo sessInfo)
    {
        ArrayList<NFishingSettings> chopProps = ((ArrayList<NFishingSettings>) NConfig.get(NConfig.Key.fishingsettings));
        if (chopProps == null)
            chopProps = new ArrayList<>();
        for (NFishingSettings prop : chopProps)
        {
            if (prop.username.equals(sessInfo.username) && prop.chrid.equals(sessInfo.characterInfo.chrid))
            {
                return prop;
            }
        }
        return new NFishingSettings(sessInfo.username, sessInfo.characterInfo.chrid);
    }
}
