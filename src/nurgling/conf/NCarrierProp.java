package nurgling.conf;

import nurgling.NConfig;
import nurgling.NUI;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;


public class NCarrierProp implements JConf
{
    final private String username;
    final private String chrid;
    public String object = null;


    public NCarrierProp(String username, String chrid) {
        this.username = username;
        this.chrid = chrid;
    }

    public NCarrierProp(HashMap<String, Object> values)
    {
        chrid = (String) values.get("chrid");
        username = (String) values.get("username");
        if (values.get("object") != null)
            object = (String) values.get("object");
    }

    public static void set(NCarrierProp prop)
    {
        ArrayList<NCarrierProp> carrierProps = ((ArrayList<NCarrierProp>) NConfig.get(NConfig.Key.carrierprop));
        if (carrierProps != null)
        {
            for (Iterator<NCarrierProp> i = carrierProps.iterator(); i.hasNext(); )
            {
                NCarrierProp oldprop = i.next();
                if(oldprop.username.equals(prop.username) && oldprop.chrid.equals(prop.chrid))
                {
                    i.remove();
                    break;
                }
            }

        }
        else
        {
            carrierProps = new ArrayList<>();
        }
        carrierProps.add(prop);
        NConfig.set(NConfig.Key.carrierprop, carrierProps);
    }

    @Override
    public String toString()
    {
        return "NCarrierProp[" + username + "|" + chrid + "]";
    }

    @Override
    public JSONObject toJson()
    {
        JSONObject jcarrier = new JSONObject();
        jcarrier.put("type", "NCarrierProp");
        jcarrier.put("username", username);
        jcarrier.put("chrid", chrid);
        jcarrier.put("object", object);
        return jcarrier;
    }

    public static NCarrierProp get(NUI.NSessInfo sessInfo)
    {
        ArrayList<NCarrierProp> carrierProps = ((ArrayList<NCarrierProp>) NConfig.get(NConfig.Key.carrierprop));
        if (carrierProps == null)
            carrierProps = new ArrayList<>();
        for (NCarrierProp prop : carrierProps)
        {
            if (prop.username.equals(sessInfo.username) && prop.chrid.equals(sessInfo.characterInfo.chrid))
            {
                return prop;
            }
        }
        return new NCarrierProp(sessInfo.username, sessInfo.characterInfo.chrid);
    }
}
