package nurgling.conf;

import nurgling.NConfig;
import nurgling.NUI;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;


public class NChipperProp implements JConf
{
    final private String username;
    final private String chrid;
    public String tool = null;
    public boolean autoeat = false;
    public boolean autorefill = false;
    public boolean plateu = false;
    public boolean nopiles = false;

    public NChipperProp(String username, String chrid) {
        this.username = username;
        this.chrid = chrid;
    }



    public NChipperProp(HashMap<String, Object> values)
    {
        chrid = (String) values.get("chrid");
        username = (String) values.get("username");
        if (values.get("tool") != null)
            tool = (String) values.get("tool");
        if (values.get("autoeat") != null)
            autoeat = (Boolean) values.get("autoeat");
        if (values.get("autorefill") != null)
            autorefill = (Boolean) values.get("autorefill");
        if (values.get("plateu") != null)
            plateu = (Boolean) values.get("plateu");
        if (values.get("nopiles") != null)
            nopiles = (Boolean) values.get("nopiles");
    }

    public static void set(NChipperProp prop)
    {
        ArrayList<NChipperProp> chipperProps = ((ArrayList<NChipperProp>) NConfig.get(NConfig.Key.chipperprop));
        if (chipperProps != null)
        {
            for (Iterator<NChipperProp> i = chipperProps.iterator(); i.hasNext(); )
            {
                NChipperProp oldprop = i.next();
                if(oldprop.username.equals(prop.username) && oldprop.chrid.equals(prop.chrid))
                {
                    i.remove();
                    break;
                }
            }

        }
        else
        {
            chipperProps = new ArrayList<>();
        }
        chipperProps.add(prop);
        NConfig.set(NConfig.Key.chipperprop, chipperProps);
    }

    @Override
    public String toString()
    {
        return "NChipperProp[" + username + "|" + chrid + "]";
    }

    @Override
    public JSONObject toJson()
    {
        JSONObject jchiper = new JSONObject();
        jchiper.put("type", "NChipperProp");
        jchiper.put("username", username);
        jchiper.put("chrid", chrid);
        jchiper.put("tool", tool);
        jchiper.put("autoeat", autoeat);
        jchiper.put("autorefill", autorefill);
        jchiper.put("plateu", plateu);
        jchiper.put("nopiles", nopiles);
        return jchiper;
    }

    public static NChipperProp get(NUI.NSessInfo sessInfo)
    {
        ArrayList<NChipperProp> chipProps = ((ArrayList<NChipperProp>) NConfig.get(NConfig.Key.chipperprop));
        if (chipProps == null)
            chipProps = new ArrayList<>();
        for (NChipperProp prop : chipProps)
        {
            if (prop.username.equals(sessInfo.username) && prop.chrid.equals(sessInfo.characterInfo.chrid))
            {
                return prop;
            }
        }
        return new NChipperProp(sessInfo.username, sessInfo.characterInfo.chrid);
    }
}
