package nurgling.conf;

import nurgling.NConfig;
import nurgling.NUI;
import nurgling.routes.ForagerPath;
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
