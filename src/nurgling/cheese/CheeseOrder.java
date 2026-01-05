package nurgling.cheese;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.*;

public class CheeseOrder {
    private final int id;
    private String cheeseType;
    private int count;
    private List<StepStatus> status;

    public CheeseOrder(int id, String cheeseType, int count, List<StepStatus> status) {
        this.id = id;
        this.cheeseType = cheeseType;
        this.count = count;
        this.status = status;
    }

    public CheeseOrder(JSONObject json) {
        this.id = json.getInt("id");
        this.cheeseType = json.getString("cheeseType");
        this.count = json.getInt("count");
        this.status = new ArrayList<>();
        if (json.has("status")) {
            JSONArray arr = json.getJSONArray("status");
            for (int i = 0; i < arr.length(); i++)
                status.add(new StepStatus(arr.getJSONObject(i)));
        }
    }

    public JSONObject toJson() {
        JSONObject obj = new JSONObject();
        obj.put("id", id);
        obj.put("cheeseType", cheeseType);
        obj.put("count", count);
        if (status != null) {
            JSONArray arr = new JSONArray();
            for (StepStatus s : status) arr.put(s.toJson());
            obj.put("status", arr);
        }
        return obj;
    }

    public int getId() { return id; }
    public String getCheeseType() { return cheeseType; }
    public int getCount() { return count; }
    public void addToCount(int amount) { this.count += amount; }
    public List<StepStatus> getStatus() { return status; }

    // Status for each step in the chain (optional)
    public static class StepStatus {
        public final String name;
        public final String place;
        public int left;

        public StepStatus(String name, String place, int left) {
            this.name = name;
            this.place = place;
            this.left = left;
        }
        public StepStatus(JSONObject json) {
            this.name = json.getString("name");
            this.place = json.getString("place");
            this.left = json.getInt("left");
        }
        public JSONObject toJson() {
            JSONObject obj = new JSONObject();
            obj.put("name", name);
            obj.put("place", place);
            obj.put("left", left);
            return obj;
        }
    }
}
