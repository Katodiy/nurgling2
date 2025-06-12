package nurgling.scenarios;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

public class Scenario {
    private int id;
    private String name;
    private ArrayList<BotStep> steps;

    public Scenario(int id, String name) {
        this.id = id;
        this.name = name;
        this.steps = new ArrayList<>();
    }

    public Scenario(JSONObject obj) {
        this.id = obj.optInt("id", 0);
        this.name = obj.getString("name");
        this.steps = new ArrayList<>();

        if (obj.has("steps")) {
            JSONArray arr = obj.getJSONArray("steps");
            for (int i = 0; i < arr.length(); i++) {
                JSONObject stepObj = arr.getJSONObject(i);
                this.steps.add(new BotStep(stepObj));
            }
        }
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public ArrayList<BotStep> getSteps() { return steps; }
    public void setSteps(ArrayList<BotStep> steps) { this.steps = steps; }

    public void addStep(BotStep step) {
        this.steps.add(step);
    }

    public void removeStep(BotStep step) {
        this.steps.remove(step);
    }

    // Serialization
    public JSONObject toJson() {
        JSONObject obj = new JSONObject();
        obj.put("id", id);
        obj.put("name", name);

        JSONArray arr = new JSONArray();
        for (BotStep step : steps) {
            arr.put(step.toJson());
        }
        obj.put("steps", arr);

        return obj;
    }
}


