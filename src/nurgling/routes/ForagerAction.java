package nurgling.routes;

import org.json.JSONObject;

public class ForagerAction {
    
    public enum ActionType {
        PICK,
        FLOWER_ACTION,
        CHAT_NOTIFY
    }
    
    public enum NotifyTarget {
        DISCORD,
        CHAT
    }
    
    public String targetObjectPattern;
    public ActionType actionType;
    public String actionName;  // For FLOWER_ACTION
    
    // For CHAT_NOTIFY
    public NotifyTarget notifyTarget;
    public String chatChannelName;  // For CHAT notify
    
    public ForagerAction(String targetObjectPattern, ActionType actionType, String actionName, 
                         NotifyTarget notifyTarget, String chatChannelName) {
        this.targetObjectPattern = targetObjectPattern;
        this.actionType = actionType;
        this.actionName = actionName;
        this.notifyTarget = notifyTarget;
        this.chatChannelName = chatChannelName;
    }
    
    public ForagerAction(String targetObjectPattern, ActionType actionType, String actionName) {
        this(targetObjectPattern, actionType, actionName, null, null);
    }
    
    public ForagerAction(String targetObjectPattern, ActionType actionType) {
        this(targetObjectPattern, actionType, null, null, null);
    }
    
    public ForagerAction(JSONObject json) {
        this.targetObjectPattern = json.getString("targetObjectPattern");
        this.actionType = ActionType.valueOf(json.getString("actionType"));
        if (json.has("actionName")) {
            this.actionName = json.getString("actionName");
        }
        if (json.has("notifyTarget")) {
            this.notifyTarget = NotifyTarget.valueOf(json.getString("notifyTarget"));
        }
        if (json.has("chatChannelName")) {
            this.chatChannelName = json.getString("chatChannelName");
        }
    }
    
    public ForagerAction(java.util.HashMap<String, Object> map) {
        this.targetObjectPattern = (String) map.get("targetObjectPattern");
        this.actionType = ActionType.valueOf((String) map.get("actionType"));
        if (map.containsKey("actionName")) {
            this.actionName = (String) map.get("actionName");
        }
        if (map.containsKey("notifyTarget")) {
            this.notifyTarget = NotifyTarget.valueOf((String) map.get("notifyTarget"));
        }
        if (map.containsKey("chatChannelName")) {
            this.chatChannelName = (String) map.get("chatChannelName");
        }
    }
    
    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.put("targetObjectPattern", targetObjectPattern);
        json.put("actionType", actionType.name());
        if (actionName != null) {
            json.put("actionName", actionName);
        }
        if (notifyTarget != null) {
            json.put("notifyTarget", notifyTarget.name());
        }
        if (chatChannelName != null) {
            json.put("chatChannelName", chatChannelName);
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
