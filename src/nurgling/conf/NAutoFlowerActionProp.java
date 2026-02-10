package nurgling.conf;

import nurgling.NConfig;
import nurgling.NUI;
import nurgling.NUtils;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;

public class NAutoFlowerActionProp implements JConf {
    final private String username;
    final private String chrid;
    public String action = "";
    public boolean transfer = false;
    public List<String> actionHistory = new ArrayList<>();
    private static final int MAX_HISTORY_SIZE = 30;

    public NAutoFlowerActionProp(String username, String chrid) {
        this.username = username;
        this.chrid = chrid;
    }

    public NAutoFlowerActionProp(HashMap<String, Object> values) {
        chrid = (String) values.get("chrid");
        username = (String) values.get("username");
        if (values.get("action") != null)
            action = (String) values.get("action");
        if (values.get("transfer") != null)
            transfer = (Boolean) values.get("transfer");
        if (values.get("actionHistory") != null)
            actionHistory = (List<String>) values.get("actionHistory");
    }

    public static void set(NAutoFlowerActionProp prop) {
        ArrayList<NAutoFlowerActionProp> props = ((ArrayList<NAutoFlowerActionProp>) NConfig.get(NConfig.Key.autofloweractionprop));
        if (props != null) {
            for (Iterator<NAutoFlowerActionProp> i = props.iterator(); i.hasNext(); ) {
                NAutoFlowerActionProp oldprop = i.next();
                if (oldprop.username.equals(prop.username) && oldprop.chrid.equals(prop.chrid)) {
                    i.remove();
                    break;
                }
            }
        } else {
            props = new ArrayList<>();
        }
        props.add(prop);
        NConfig.set(NConfig.Key.autofloweractionprop, props);
    }

    @Override
    public String toString() {
        return "NAutoFlowerActionProp[" + username + "|" + chrid + "]";
    }

    public void addToHistory(String actionName) {
        if (actionName == null || actionName.trim().isEmpty()) {
            return;
        }
        
        LinkedHashSet<String> uniqueHistory = new LinkedHashSet<>(actionHistory);
        uniqueHistory.remove(actionName);
        
        List<String> newHistory = new ArrayList<>();
        newHistory.add(actionName);
        newHistory.addAll(uniqueHistory);
        
        if (newHistory.size() > MAX_HISTORY_SIZE) {
            newHistory = newHistory.subList(0, MAX_HISTORY_SIZE);
        }
        
        actionHistory = newHistory;
    }

    @Override
    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.put("type", "NAutoFlowerActionProp");
        json.put("username", username);
        json.put("chrid", chrid);
        json.put("action", action);
        json.put("transfer", transfer);
        json.put("actionHistory", actionHistory);
        return json;
    }

    public static NAutoFlowerActionProp get(NUI.NSessInfo sessInfo) {
        if (sessInfo == null || NUtils.getGameUI() == null || NUtils.getGameUI().getCharInfo() == null)
            return null;
        String chrid = NUtils.getGameUI().getCharInfo().chrid;
        ArrayList<NAutoFlowerActionProp> props = ((ArrayList<NAutoFlowerActionProp>) NConfig.get(NConfig.Key.autofloweractionprop));
        if (props == null)
            props = new ArrayList<>();
        for (NAutoFlowerActionProp prop : props) {
            if (prop.username.equals(sessInfo.username) && prop.chrid.equals(chrid)) {
                return prop;
            }
        }
        return new NAutoFlowerActionProp(sessInfo.username, chrid);
    }
}
