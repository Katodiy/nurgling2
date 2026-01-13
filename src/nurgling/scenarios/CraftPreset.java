package nurgling.scenarios;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents a saved crafting preset that can be used in scenarios.
 * Captures the recipe configuration from NMakewindow for later replay.
 */
public class CraftPreset {
    private String id;
    private String name;
    private String recipeName;
    private String recipeResource;
    private String workstationType;
    private List<InputSpec> inputs;
    private List<OutputSpec> outputs;

    public CraftPreset() {
        this.id = UUID.randomUUID().toString();
        this.inputs = new ArrayList<>();
        this.outputs = new ArrayList<>();
    }

    public CraftPreset(JSONObject obj) {
        this.id = obj.optString("id", UUID.randomUUID().toString());
        this.name = obj.optString("name", "");
        this.recipeName = obj.optString("recipeName", "");
        this.recipeResource = obj.optString("recipeResource", "");
        this.workstationType = obj.optString("workstationType", "");

        this.inputs = new ArrayList<>();
        if (obj.has("inputs")) {
            JSONArray inputsArray = obj.getJSONArray("inputs");
            for (int i = 0; i < inputsArray.length(); i++) {
                inputs.add(new InputSpec(inputsArray.getJSONObject(i)));
            }
        }

        this.outputs = new ArrayList<>();
        if (obj.has("outputs")) {
            JSONArray outputsArray = obj.getJSONArray("outputs");
            for (int i = 0; i < outputsArray.length(); i++) {
                outputs.add(new OutputSpec(outputsArray.getJSONObject(i)));
            }
        }
    }

    public JSONObject toJson() {
        JSONObject obj = new JSONObject();
        obj.put("id", id);
        obj.put("name", name);
        obj.put("recipeName", recipeName);
        obj.put("recipeResource", recipeResource);
        obj.put("workstationType", workstationType);

        JSONArray inputsArray = new JSONArray();
        for (InputSpec input : inputs) {
            inputsArray.put(input.toJson());
        }
        obj.put("inputs", inputsArray);

        JSONArray outputsArray = new JSONArray();
        for (OutputSpec output : outputs) {
            outputsArray.put(output.toJson());
        }
        obj.put("outputs", outputsArray);

        return obj;
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getRecipeName() { return recipeName; }
    public void setRecipeName(String recipeName) { this.recipeName = recipeName; }

    public String getRecipeResource() { return recipeResource; }
    public void setRecipeResource(String recipeResource) { this.recipeResource = recipeResource; }

    public List<InputSpec> getInputs() { return inputs; }
    public void setInputs(List<InputSpec> inputs) { this.inputs = inputs; }

    public List<OutputSpec> getOutputs() { return outputs; }
    public void setOutputs(List<OutputSpec> outputs) { this.outputs = outputs; }

    /**
     * Represents an input ingredient in the recipe.
     */
    public static class InputSpec {
        private String name;
        private String resourcePath;
        private int count;
        private boolean isCategory;
        private String preferredIngredient;
        private boolean isOptional;
        private boolean isIgnored;

        public InputSpec() {}

        public InputSpec(JSONObject obj) {
            this.name = obj.optString("name", "");
            this.resourcePath = obj.optString("resourcePath", "");
            this.count = obj.optInt("count", 1);
            this.isCategory = obj.optBoolean("isCategory", false);
            this.preferredIngredient = obj.optString("preferredIngredient", null);
            this.isOptional = obj.optBoolean("isOptional", false);
            this.isIgnored = obj.optBoolean("isIgnored", false);
        }

        public JSONObject toJson() {
            JSONObject obj = new JSONObject();
            obj.put("name", name);
            obj.put("resourcePath", resourcePath);
            obj.put("count", count);
            obj.put("isCategory", isCategory);
            if (preferredIngredient != null) {
                obj.put("preferredIngredient", preferredIngredient);
            }
            obj.put("isOptional", isOptional);
            obj.put("isIgnored", isIgnored);
            return obj;
        }

        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getResourcePath() { return resourcePath; }
        public void setResourcePath(String resourcePath) { this.resourcePath = resourcePath; }

        public int getCount() { return count; }
        public void setCount(int count) { this.count = count; }

        public boolean isCategory() { return isCategory; }
        public void setCategory(boolean category) { isCategory = category; }

        public String getPreferredIngredient() { return preferredIngredient; }
        public void setPreferredIngredient(String preferredIngredient) { this.preferredIngredient = preferredIngredient; }

        public boolean isOptional() { return isOptional; }
        public void setOptional(boolean optional) { isOptional = optional; }

        public boolean isIgnored() { return isIgnored; }
        public void setIgnored(boolean ignored) { isIgnored = ignored; }
    }

    /**
     * Represents an output item in the recipe.
     */
    public static class OutputSpec {
        private String name;
        private String resourcePath;
        private int count;

        public OutputSpec() {}

        public OutputSpec(JSONObject obj) {
            this.name = obj.optString("name", "");
            this.resourcePath = obj.optString("resourcePath", "");
            this.count = obj.optInt("count", 1);
        }

        public JSONObject toJson() {
            JSONObject obj = new JSONObject();
            obj.put("name", name);
            obj.put("resourcePath", resourcePath);
            obj.put("count", count);
            return obj;
        }

        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getResourcePath() { return resourcePath; }
        public void setResourcePath(String resourcePath) { this.resourcePath = resourcePath; }

        public int getCount() { return count; }
        public void setCount(int count) { this.count = count; }
    }
}
