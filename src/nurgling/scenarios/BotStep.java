package nurgling.scenarios;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class BotStep {
    private String botKey; // e.g. "carrot"
    private Map<String, Object> params;

    public BotStep(String botKey) {
        this.botKey = botKey;
        this.params = new HashMap<>();
    }

    public BotStep(JSONObject obj) {
        this.botKey = obj.getString("botKey");
        this.params = new HashMap<>();
        if (obj.has("params")) {
            JSONObject paramsObj = obj.getJSONObject("params");
            for (String key : paramsObj.keySet()) {
                this.params.put(key, paramsObj.get(key));
            }
        }
    }

    public String getBotKey() { return botKey; }
    public void setBotKey(String botKey) { this.botKey = botKey; }

    public Map<String, Object> getParams() { return params; }
    public void setParams(Map<String, Object> params) { this.params = params; }

    public JSONObject toJson() {
        JSONObject obj = new JSONObject();
        obj.put("botKey", botKey);
        obj.put("params", new JSONObject(params));
        return obj;
    }
}