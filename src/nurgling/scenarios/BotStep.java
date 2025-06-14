package nurgling.scenarios;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class BotStep {
    private String id;
    private Map<String, Object> settings;

    public BotStep(String id) {
        this.id = id;
        this.settings = new HashMap<>();
    }

    public BotStep(JSONObject obj) {
        this.id = obj.getString("id");
        this.settings = new HashMap<>();
        if (obj.has("params")) {
            JSONObject paramsObj = obj.getJSONObject("params");
            for (String key : paramsObj.keySet()) {
                this.settings.put(key, paramsObj.get(key));
            }
        }
    }

    public String getId() {
        return id;
    }

    public Map<String, Object> getSettings() { return settings; }
    public void setSetting(String key, Object value) {
        if (this.settings == null) this.settings = new HashMap<>();
        this.settings.put(key, value);
    }

    public Object getSetting(String key) {
        return settings.get(key);
    }

    public JSONObject toJson() {
        JSONObject obj = new JSONObject();
        obj.put("id", id);
        obj.put("params", new JSONObject(settings));
        return obj;
    }

    public void setSettings(Map<String, Object> editedSettings) {
        this.settings = editedSettings;
    }
}
