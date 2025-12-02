package nurgling.conf;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Represents a quick action preset with its own name, keybinding, and patterns.
 */
public class QuickActionPreset implements JConf {
    public String name;
    public String keybind; // Reduced KeyMatch string
    public ArrayList<HashMap<String, Object>> patterns;

    public QuickActionPreset() {
        this.name = "Default";
        this.keybind = "";
        this.patterns = new ArrayList<>();
    }

    public QuickActionPreset(String name) {
        this.name = name;
        this.keybind = "";
        this.patterns = new ArrayList<>();
    }

    @SuppressWarnings("unchecked")
    public QuickActionPreset(HashMap<String, Object> map) {
        this.name = (String) map.getOrDefault("name", "Default");
        this.keybind = (String) map.getOrDefault("keybind", "");
        Object patternsObj = map.get("patterns");
        if (patternsObj instanceof ArrayList) {
            this.patterns = (ArrayList<HashMap<String, Object>>) patternsObj;
        } else {
            this.patterns = new ArrayList<>();
        }
    }

    @Override
    public JSONObject toJson() {
        JSONObject obj = new JSONObject();
        obj.put("type", "QuickActionPreset");
        obj.put("name", name);
        obj.put("keybind", keybind);
        
        JSONArray patternsArray = new JSONArray();
        for (HashMap<String, Object> pattern : patterns) {
            JSONObject patternObj = new JSONObject(pattern);
            patternsArray.put(patternObj);
        }
        obj.put("patterns", patternsArray);
        
        return obj;
    }

    public void addPattern(String patternName, boolean enabled) {
        HashMap<String, Object> pattern = new HashMap<>();
        pattern.put("type", "NPattern");
        pattern.put("name", patternName);
        pattern.put("enabled", enabled);
        patterns.add(pattern);
    }

    public static QuickActionPreset createDefault() {
        QuickActionPreset preset = new QuickActionPreset("Default");
        // Default preset has no keybind - uses legacy Q key binding from NMapView
        preset.keybind = "";
        preset.addPattern(".*cart", true);
        preset.addPattern("gfx/kritter/.*", true);
        preset.addPattern("gfx/terobjs/herbs.*", true);
        return preset;
    }
}
