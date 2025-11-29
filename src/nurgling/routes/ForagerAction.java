package nurgling.routes;

import org.json.JSONObject;

public class ForagerAction {
    
    public enum ActionType {
        PICK,
        FLOWER_ACTION,
        CHAT_NOTIFY
    }
    
    public String targetObjectPattern;
    public ActionType actionType;
    public String actionName;
    
    public ForagerAction(String targetObjectPattern, ActionType actionType, String actionName) {
        this.targetObjectPattern = targetObjectPattern;
        this.actionType = actionType;
        this.actionName = actionName;
    }
    
    public ForagerAction(String targetObjectPattern, ActionType actionType) {
        this(targetObjectPattern, actionType, null);
    }
    
    public ForagerAction(JSONObject json) {
        this.targetObjectPattern = json.getString("targetObjectPattern");
        this.actionType = ActionType.valueOf(json.getString("actionType"));
        if (json.has("actionName")) {
            this.actionName = json.getString("actionName");
        }
    }
    
    public ForagerAction(java.util.HashMap<String, Object> map) {
        this.targetObjectPattern = (String) map.get("targetObjectPattern");
        this.actionType = ActionType.valueOf((String) map.get("actionType"));
        if (map.containsKey("actionName")) {
            this.actionName = (String) map.get("actionName");
        }
    }
    
    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.put("targetObjectPattern", targetObjectPattern);
        json.put("actionType", actionType.name());
        if (actionName != null) {
            json.put("actionName", actionName);
        }
        return json;
    }
    
    @Override
    public String toString() {
        return String.format("ForagerAction[%s, %s%s]", 
            targetObjectPattern, 
            actionType,
            actionName != null ? ", " + actionName : "");
    }
}
