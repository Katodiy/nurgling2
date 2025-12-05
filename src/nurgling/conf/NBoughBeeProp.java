package nurgling.conf;

import nurgling.NConfig;
import nurgling.NUI;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class NBoughBeeProp implements JConf {
    final private String username;
    final private String chrid;
    public String onPlayerAction = "nothing";
    public String onAnimalAction = "logout";
    public String afterHarvestAction = "nothing";

    public NBoughBeeProp(String username, String chrid) {
        this.username = username;
        this.chrid = chrid;
    }

    public NBoughBeeProp(HashMap<String, Object> values) {
        chrid = (String) values.get("chrid");
        username = (String) values.get("username");
        if (values.get("onPlayerAction") != null)
            onPlayerAction = (String) values.get("onPlayerAction");
        if (values.get("onAnimalAction") != null)
            onAnimalAction = (String) values.get("onAnimalAction");
        if (values.get("afterHarvestAction") != null)
            afterHarvestAction = (String) values.get("afterHarvestAction");
    }

    public static void set(NBoughBeeProp prop) {
        @SuppressWarnings("unchecked")
        ArrayList<NBoughBeeProp> boughBeeProps = ((ArrayList<NBoughBeeProp>) NConfig.get(NConfig.Key.boughbeeprop));
        if (boughBeeProps != null) {
            for (Iterator<NBoughBeeProp> i = boughBeeProps.iterator(); i.hasNext(); ) {
                NBoughBeeProp oldprop = i.next();
                if (oldprop.username.equals(prop.username) && oldprop.chrid.equals(prop.chrid)) {
                    i.remove();
                    break;
                }
            }
        } else {
            boughBeeProps = new ArrayList<>();
        }
        boughBeeProps.add(prop);
        NConfig.set(NConfig.Key.boughbeeprop, boughBeeProps);
    }

    @Override
    public String toString() {
        return "NBoughBeeProp[" + username + "|" + chrid + "]";
    }

    @Override
    public JSONObject toJson() {
        JSONObject jboughbee = new JSONObject();
        jboughbee.put("type", "NBoughBeeProp");
        jboughbee.put("username", username);
        jboughbee.put("chrid", chrid);
        jboughbee.put("onPlayerAction", onPlayerAction);
        jboughbee.put("onAnimalAction", onAnimalAction);
        jboughbee.put("afterHarvestAction", afterHarvestAction);
        return jboughbee;
    }

    public static NBoughBeeProp get(NUI.NSessInfo sessInfo) {
        @SuppressWarnings("unchecked")
        ArrayList<NBoughBeeProp> boughBeeProps = ((ArrayList<NBoughBeeProp>) NConfig.get(NConfig.Key.boughbeeprop));
        if (boughBeeProps == null)
            boughBeeProps = new ArrayList<>();
        for (NBoughBeeProp prop : boughBeeProps) {
            if (prop.username.equals(sessInfo.username) && prop.chrid.equals(sessInfo.characterInfo.chrid)) {
                return prop;
            }
        }
        return new NBoughBeeProp(sessInfo.username, sessInfo.characterInfo.chrid);
    }
}
