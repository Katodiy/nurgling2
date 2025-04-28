package nurgling.conf;

import nurgling.NConfig;
import nurgling.NUI;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class NHarvestCropProp implements JConf {
    final private String username;
    final private String chrid;
    public boolean autorefill = false;

    public NHarvestCropProp(String username, String chrid) {
        this.username = username;
        this.chrid = chrid;
    }

    public NHarvestCropProp(HashMap<String, Object> values)
    {
        chrid = (String) values.get("chrid");
        username = (String) values.get("username");
        if (values.get("autorefill") != null)
            autorefill = (Boolean) values.get("autorefill");
    }

    public static void set(NHarvestCropProp prop)
    {
        ArrayList<NHarvestCropProp> harvestCropProps = ((ArrayList<NHarvestCropProp>) NConfig.get(NConfig.Key.harvestCropProp));
        if (harvestCropProps != null)
        {
            for (Iterator<NHarvestCropProp> i = harvestCropProps.iterator(); i.hasNext(); )
            {
                NHarvestCropProp oldprop = i.next();
                if(oldprop.username.equals(prop.username) && oldprop.chrid.equals(prop.chrid))
                {
                    i.remove();
                    break;
                }
            }

        }
        else
        {
            harvestCropProps = new ArrayList<>();
        }
        harvestCropProps.add(prop);
        NConfig.set(NConfig.Key.harvestCropProp, harvestCropProps);
    }

    @Override
    public String toString()
    {
        return "NHarvestCropProp[" + username + "|" + chrid + "]";
    }

    @Override
    public JSONObject toJson()
    {
        JSONObject jharvestCrop = new JSONObject();
        jharvestCrop.put("type", "NHarvestCropProp");
        jharvestCrop.put("username", username);
        jharvestCrop.put("chrid", chrid);
        jharvestCrop.put("autorefill", autorefill);
        return jharvestCrop;
    }

    public static NHarvestCropProp get(NUI.NSessInfo sessInfo)
    {
        ArrayList<NHarvestCropProp> harvestCropProps = ((ArrayList<NHarvestCropProp>) NConfig.get(NConfig.Key.harvestCropProp));
        if (harvestCropProps == null)
            harvestCropProps = new ArrayList<>();
        for (NHarvestCropProp prop : harvestCropProps)
        {
            if (prop.username.equals(sessInfo.username) && prop.chrid.equals(sessInfo.characterInfo.chrid))
            {
                return prop;
            }
        }
        return new NHarvestCropProp(sessInfo.username, sessInfo.characterInfo.chrid);
    }
}
