package nurgling.scenarios;

import nurgling.NConfig;
import nurgling.NGameUI;
import nurgling.NUtils;
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
        File file = new File(NConfig.current.getScenariosPath());
        if (file.exists()) {
            StringBuilder contentBuilder = new StringBuilder();
            try (Stream<String> stream = Files.lines(Paths.get(NConfig.current.getScenariosPath()), StandardCharsets.UTF_8)) {
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
            FileWriter f = new FileWriter(customPath == null ? NConfig.current.getScenariosPath() : customPath, StandardCharsets.UTF_8);
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

    public void executeScenarioByName(String scenarioName, NGameUI gui) {
        for(Scenario scenario : this.getScenarios().values()) {
            if(scenario.getName().equals(scenarioName)) {
                Thread t = new Thread(() -> {
                    try {
                        nurgling.actions.bots.ScenarioRunner runner = new nurgling.actions.bots.ScenarioRunner(scenario);
                        runner.run(gui);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } catch (Exception e) {
                        NUtils.getGameUI().error("Scenario execution failed: " + e.getMessage());

                    }
                }, "ScenarioRunner-" + scenarioName);

                NUtils.getGameUI().biw.addObserve(t);
                t.start();
                return;
            }
        }
        NUtils.getGameUI().error("Scenario not found: " + scenarioName);
    }
}
