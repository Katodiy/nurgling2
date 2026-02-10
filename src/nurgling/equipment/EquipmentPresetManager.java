package nurgling.equipment;

import nurgling.NConfig;
import nurgling.NUtils;
import nurgling.actions.bots.EquipmentBot;
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

public class EquipmentPresetManager {
    private final Map<String, EquipmentPreset> presets = new LinkedHashMap<>();

    public EquipmentPresetManager() {
        loadPresets();
    }

    public void loadPresets() {
        presets.clear();
        File file = new File(NConfig.current.getEquipmentPresetsPath());
        if (file.exists()) {
            StringBuilder contentBuilder = new StringBuilder();
            try (Stream<String> stream = Files.lines(Paths.get(NConfig.current.getEquipmentPresetsPath()), StandardCharsets.UTF_8)) {
                stream.forEach(s -> contentBuilder.append(s).append("\n"));
            } catch (IOException ignore) {}

            if (!contentBuilder.toString().isEmpty()) {
                JSONObject main = new JSONObject(contentBuilder.toString());
                JSONArray array = main.getJSONArray("presets");
                for (int i = 0; i < array.length(); i++) {
                    EquipmentPreset preset = new EquipmentPreset(array.getJSONObject(i));
                    presets.put(preset.getId(), preset);
                }
            }
        }
    }

    public void writePresets(String customPath) {
        JSONObject main = new JSONObject();
        JSONArray jpresets = new JSONArray();
        for (EquipmentPreset preset : presets.values()) {
            jpresets.put(preset.toJson());
        }
        main.put("presets", jpresets);

        try {
            FileWriter f = new FileWriter(customPath == null ? NConfig.current.getEquipmentPresetsPath() : customPath, StandardCharsets.UTF_8);
            main.write(f);
            f.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void addOrUpdatePreset(EquipmentPreset preset) {
        presets.put(preset.getId(), preset);
    }

    public void deletePreset(String presetId) {
        presets.remove(presetId);
    }

    public Map<String, EquipmentPreset> getPresets() {
        return presets;
    }

    public EquipmentPreset getPreset(String id) {
        return presets.get(id);
    }

    public void executePreset(String presetId) {
        EquipmentPreset preset = presets.get(presetId);
        if (preset == null) {
            NUtils.getGameUI().error("Equipment preset not found");
            return;
        }
        executePreset(preset);
    }

    public void executePreset(EquipmentPreset preset) {
        Thread t = new Thread(() -> {
            try {
                EquipmentBot bot = new EquipmentBot(preset);
                bot.run(NUtils.getGameUI());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "EquipmentBot-" + preset.getName());

        NUtils.getGameUI().biw.addObserve(t);
        t.start();
    }
}
