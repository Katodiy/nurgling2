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

/**
 * Manages craft presets - saving, loading, and CRUD operations.
 * Presets are stored globally (not per-character).
 */
public class CraftPresetManager {
    private static CraftPresetManager instance;
    private final Map<String, CraftPreset> presets = new LinkedHashMap<>();

    private CraftPresetManager() {
        loadPresets();
    }

    public static CraftPresetManager getInstance() {
        if (instance == null) {
            instance = new CraftPresetManager();
        }
        return instance;
    }

    public void loadPresets() {
        presets.clear();
        File file = new File(NConfig.current.getCraftPresetsPath());
        if (file.exists()) {
            StringBuilder contentBuilder = new StringBuilder();
            try (Stream<String> stream = Files.lines(Paths.get(NConfig.current.getCraftPresetsPath()), StandardCharsets.UTF_8)) {
                stream.forEach(s -> contentBuilder.append(s).append("\n"));
            } catch (IOException ignore) {}

            if (!contentBuilder.toString().isEmpty()) {
                try {
                    JSONObject main = new JSONObject(contentBuilder.toString());
                    JSONArray array = main.getJSONArray("presets");
                    for (int i = 0; i < array.length(); i++) {
                        CraftPreset preset = new CraftPreset(array.getJSONObject(i));
                        presets.put(preset.getId(), preset);
                    }
                } catch (Exception e) {
                    System.err.println("Error loading craft presets: " + e.getMessage());
                }
            }
        }
    }

    public void savePresets() {
        JSONObject main = new JSONObject();
        JSONArray jpresets = new JSONArray();
        for (CraftPreset preset : presets.values()) {
            jpresets.put(preset.toJson());
        }
        main.put("presets", jpresets);

        try {
            FileWriter f = new FileWriter(NConfig.current.getCraftPresetsPath(), StandardCharsets.UTF_8);
            main.write(f);
            f.close();
        } catch (IOException e) {
            System.err.println("Error saving craft presets: " + e.getMessage());
        }
    }

    public void addOrUpdatePreset(CraftPreset preset) {
        presets.put(preset.getId(), preset);
        savePresets();
    }

    public void deletePreset(String presetId) {
        presets.remove(presetId);
        savePresets();
    }

    public Map<String, CraftPreset> getPresets() {
        return presets;
    }

    public CraftPreset getPreset(String id) {
        return presets.get(id);
    }

    public List<CraftPreset> getPresetList() {
        List<CraftPreset> list = new ArrayList<>(presets.values());
        list.sort(Comparator.comparing(CraftPreset::getName));
        return list;
    }
}
