package nurgling.scenarios;

import nurgling.actions.bots.registry.BotDescriptor;
import nurgling.actions.bots.registry.BotRegistry;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class BotStep {
    private String botKey; // e.g. "carrot"
    private Map<String, Object> settings;

    public BotStep(String botKey) {
        this.botKey = botKey;
        this.settings = new HashMap<>();
    }

    public BotStep(JSONObject obj) {
        this.botKey = obj.getString("botKey");
        this.settings = new HashMap<>();
        if (obj.has("params")) {
            JSONObject paramsObj = obj.getJSONObject("params");
            for (String key : paramsObj.keySet()) {
                this.settings.put(key, paramsObj.get(key));
            }
        }
    }

    public String getBotKey() { return botKey; }
    public void setBotKey(String botKey) { this.botKey = botKey; }

    public Map<String, Object> getSettings() { return settings; }
    public void setSetting(String key, Object value) {
        if (this.settings == null) this.settings = new HashMap<>();
        this.settings.put(key, value);
    }

    public boolean hasSettings() {
        BotDescriptor desc = BotRegistry.getDescriptor(botKey);
        return desc != null && desc.factory.requiredSettings().size() > 0;
    }

    public Object getSetting(String key) {
        return settings.get(key);
    }

    public JSONObject toJson() {
        JSONObject obj = new JSONObject();
        obj.put("botKey", botKey);
        obj.put("params", new JSONObject(settings));
        return obj;
    }

    public void setSettings(Map<String, Object> editedSettings) {
        this.settings = editedSettings;
    }
}