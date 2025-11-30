package nurgling.conf;

import nurgling.NConfig;
import nurgling.NUI;
import nurgling.routes.ForagerAction;
import nurgling.routes.ForagerPath;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class NForagerProp implements JConf {
    
    private final String username;
    private final String chrid;
    public String currentPreset = "Default";
    public HashMap<String, PresetData> presets = new HashMap<>();
    
    public static class PresetData {
        public String pathFile = "";
        public transient ForagerPath foragerPath = null;
        public ArrayList<ForagerAction> actions = new ArrayList<>();
        
        public String onPlayerAction = "nothing";
        public String onAnimalAction = "logout";
        public String afterFinishAction = "nothing";
        public boolean freeInventory = false;
        public boolean ignoreBats = true;
        
        public PresetData() {}
        
        public PresetData(String pathFile) {
            this.pathFile = pathFile;
        }
    }
    
    public NForagerProp(String username, String chrid) {
        this.username = username;
        this.chrid = chrid;
        presets.put("Default", new PresetData());
    }
    
    @SuppressWarnings("unchecked")
    public NForagerProp(HashMap<String, Object> values) {
        chrid = (String) values.get("chrid");
        username = (String) values.get("username");
        if (values.get("currentPreset") != null)
            currentPreset = (String) values.get("currentPreset");
        
        presets = new HashMap<>();
        if (values.get("presets") != null) {
            HashMap<String, HashMap<String, Object>> presetsMap = 
                (HashMap<String, HashMap<String, Object>>) values.get("presets");
            for (Map.Entry<String, HashMap<String, Object>> entry : presetsMap.entrySet()) {
                PresetData pd = new PresetData();
                if (entry.getValue().get("pathFile") != null)
                    pd.pathFile = (String) entry.getValue().get("pathFile");
                
                if (entry.getValue().get("actions") != null) {
                    ArrayList<HashMap<String, Object>> actionsData = 
                        (ArrayList<HashMap<String, Object>>) entry.getValue().get("actions");
                    for (HashMap<String, Object> actionMap : actionsData) {
                        pd.actions.add(new ForagerAction(actionMap));
                    }
                }
                
                if (entry.getValue().get("onPlayerAction") != null)
                    pd.onPlayerAction = (String) entry.getValue().get("onPlayerAction");
                if (entry.getValue().get("onAnimalAction") != null)
                    pd.onAnimalAction = (String) entry.getValue().get("onAnimalAction");
                if (entry.getValue().get("afterFinishAction") != null)
                    pd.afterFinishAction = (String) entry.getValue().get("afterFinishAction");
                if (entry.getValue().get("freeInventory") != null)
                    pd.freeInventory = (Boolean) entry.getValue().get("freeInventory");
                if (entry.getValue().get("ignoreBats") != null)
                    pd.ignoreBats = (Boolean) entry.getValue().get("ignoreBats");
                
                presets.put(entry.getKey(), pd);
            }
        }
        
        if (presets.isEmpty()) {
            presets.put("Default", new PresetData());
        }
    }
    
    public static void set(NForagerProp prop) {
        @SuppressWarnings("unchecked")
        ArrayList<NForagerProp> foragerProps = ((ArrayList<NForagerProp>) NConfig.get(NConfig.Key.foragerprop));
        if (foragerProps != null) {
            for (Iterator<NForagerProp> i = foragerProps.iterator(); i.hasNext(); ) {
                NForagerProp oldprop = i.next();
                if (oldprop.username.equals(prop.username) && oldprop.chrid.equals(prop.chrid)) {
                    i.remove();
                    break;
                }
            }
        } else {
            foragerProps = new ArrayList<>();
        }
        foragerProps.add(prop);
        NConfig.set(NConfig.Key.foragerprop, foragerProps);
    }
    
    @Override
    public String toString() {
        return "NForagerProp[" + username + "|" + chrid + "]";
    }
    
    @Override
    public JSONObject toJson() {
        JSONObject jforager = new JSONObject();
        jforager.put("type", "NForagerProp");
        jforager.put("username", username);
        jforager.put("chrid", chrid);
        jforager.put("currentPreset", currentPreset);
        
        JSONObject presetsJson = new JSONObject();
        for (Map.Entry<String, PresetData> entry : presets.entrySet()) {
            JSONObject presetJson = new JSONObject();
            presetJson.put("pathFile", entry.getValue().pathFile);
            
            JSONArray actionsJson = new JSONArray();
            for (ForagerAction action : entry.getValue().actions) {
                actionsJson.put(action.toJson());
            }
            presetJson.put("actions", actionsJson);
            
            presetJson.put("onPlayerAction", entry.getValue().onPlayerAction);
            presetJson.put("onAnimalAction", entry.getValue().onAnimalAction);
            presetJson.put("afterFinishAction", entry.getValue().afterFinishAction);
            presetJson.put("freeInventory", entry.getValue().freeInventory);
            presetJson.put("ignoreBats", entry.getValue().ignoreBats);
            
            presetsJson.put(entry.getKey(), presetJson);
        }
        jforager.put("presets", presetsJson);
        
        return jforager;
    }
    
    public static NForagerProp get(NUI.NSessInfo sessInfo) {
        @SuppressWarnings("unchecked")
        ArrayList<NForagerProp> foragerProps = ((ArrayList<NForagerProp>) NConfig.get(NConfig.Key.foragerprop));
        if (foragerProps == null)
            foragerProps = new ArrayList<>();
        for (NForagerProp prop : foragerProps) {
            if (prop.username.equals(sessInfo.username) && prop.chrid.equals(sessInfo.characterInfo.chrid)) {
                return prop;
            }
        }
        return new NForagerProp(sessInfo.username, sessInfo.characterInfo.chrid);
    }
}
