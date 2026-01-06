package nurgling.equipment;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EquipmentPreset {
    private String id;
    private String name;
    private Map<Integer, String> slotConfig;

    public EquipmentPreset(String name) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.slotConfig = new HashMap<>();
    }

    public EquipmentPreset(JSONObject obj) {
        this.id = obj.optString("id", UUID.randomUUID().toString());
        this.name = obj.getString("name");
        this.slotConfig = new HashMap<>();

        if (obj.has("slots")) {
            JSONObject slots = obj.getJSONObject("slots");
            for (String key : slots.keySet()) {
                try {
                    int slot = Integer.parseInt(key);
                    String resName = slots.getString(key);
                    slotConfig.put(slot, resName);
                } catch (NumberFormatException e) {
                    // Skip invalid entries
                }
            }
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<Integer, String> getSlotConfig() {
        return slotConfig;
    }

    public void setSlotConfig(Map<Integer, String> slotConfig) {
        this.slotConfig = slotConfig;
    }

    public void setSlot(int slot, String resName) {
        slotConfig.put(slot, resName);
    }

    public void clearSlot(int slot) {
        slotConfig.remove(slot);
    }

    public JSONObject toJson() {
        JSONObject obj = new JSONObject();
        obj.put("id", id);
        obj.put("name", name);

        JSONObject slots = new JSONObject();
        for (Map.Entry<Integer, String> entry : slotConfig.entrySet()) {
            slots.put(String.valueOf(entry.getKey()), entry.getValue());
        }
        obj.put("slots", slots);

        return obj;
    }
}
