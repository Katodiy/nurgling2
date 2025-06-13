package nurgling.scenarios;

import nurgling.NConfig;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

public class ScenarioManager {
    private final Map<Integer, Scenario> scenarios = new HashMap<>();
    private boolean needsUpdate = false;

    public ScenarioManager() {
        loadScenarios();
    }

    public void loadScenarios() {
        scenarios.clear();
        File file = new File(NConfig.current.path_scenarios);
        if (file.exists()) {
            StringBuilder contentBuilder = new StringBuilder();
            try (Stream<String> stream = Files.lines(Paths.get(NConfig.current.path_scenarios), StandardCharsets.UTF_8)) {
                stream.forEach(s -> contentBuilder.append(s).append("\n"));
            } catch (IOException ignore) {}

            if (!contentBuilder.toString().isEmpty()) {
                JSONObject main = new JSONObject(contentBuilder.toString());
                JSONArray array = main.getJSONArray("scenarios");
                for (int i = 0; i < array.length(); i++) {
                    Scenario scenario = new Scenario(array.getJSONObject(i));
                    scenarios.put(scenario.getId(), scenario);
                }
                needsUpdate = false;
            }
        }
    }

    public void writeScenarios(String customPath) {
        JSONObject main = new JSONObject();
        JSONArray jscenarios = new JSONArray();
        for (Scenario scenario : scenarios.values()) {
            jscenarios.put(scenario.toJson());
        }
        main.put("scenarios", jscenarios);

        try {
            FileWriter f = new FileWriter(customPath == null ? NConfig.current.path_scenarios : customPath, StandardCharsets.UTF_8);
            main.write(f);
            f.close();
            needsUpdate = false;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void addOrUpdateScenario(Scenario scenario) {
        scenarios.put(scenario.getId(), scenario);
        needsUpdate = true;
    }

    public void deleteScenario(int scenarioId) {
        scenarios.remove(scenarioId);
        needsUpdate = true;
    }

    public Map<Integer, Scenario> getScenarios() {
        return scenarios;
    }
}
