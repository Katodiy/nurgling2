package nurgling.widgets.bots;

import haven.*;
import nurgling.NUtils;
import nurgling.conf.NTrufflePigProp;
import nurgling.routes.ForagerPath;

import java.util.ArrayList;
import java.util.Collections;

public class TrufflePigHunter extends PathBotWindow {

    public NTrufflePigProp prop = null;

    public TrufflePigHunter() {
        super(new Coord(380, 200), "Truffle Pig Hunter");

        // Build common UI
        prev = buildCommonUI("Truffle Pig Hunter Settings", "Preset:", "Path:");

        // Add start button
        addStartButton();

        pack();

        // Initialize from config
        initializeFromConfig();
    }

    // ========== Abstract method implementations ==========

    @Override
    protected String getPathDataDir() {
        return "trufflepig_paths";
    }

    @Override
    protected String getNoPathsMessage() {
        return "No paths available";
    }

    @Override
    protected String loadPropAndGetCurrentPreset() {
        prop = NTrufflePigProp.get(NUtils.getUI().sessInfo);
        if (prop == null) {
            prop = new NTrufflePigProp("", "");
        }
        return prop.currentPreset;
    }

    @Override
    protected void saveProp() {
        if (prop != null) {
            NTrufflePigProp.set(prop);
        }
    }

    @Override
    protected String getCurrentPresetName() {
        return prop != null ? prop.currentPreset : null;
    }

    @Override
    protected void setCurrentPresetName(String name) {
        if (prop != null) {
            prop.currentPreset = name;
        }
    }

    @Override
    protected String getPresetPathFile(String presetName) {
        if (prop != null && prop.presets != null) {
            NTrufflePigProp.PresetData preset = prop.presets.get(presetName);
            if (preset != null) {
                return preset.pathFile;
            }
        }
        return null;
    }

    @Override
    protected void setPresetPathFile(String presetName, String pathFile) {
        if (prop != null && prop.presets != null) {
            NTrufflePigProp.PresetData preset = prop.presets.get(presetName);
            if (preset != null) {
                preset.pathFile = pathFile;
            }
        }
    }

    @Override
    protected ForagerPath getPresetForagerPath(String presetName) {
        if (prop != null && prop.presets != null) {
            NTrufflePigProp.PresetData preset = prop.presets.get(presetName);
            if (preset != null) {
                return preset.foragerPath;
            }
        }
        return null;
    }

    @Override
    protected void setPresetForagerPath(String presetName, ForagerPath path) {
        if (prop != null && prop.presets != null) {
            NTrufflePigProp.PresetData preset = prop.presets.get(presetName);
            if (preset != null) {
                preset.foragerPath = path;
            }
        }
    }

    @Override
    protected void createPreset(String name) {
        if (prop != null) {
            prop.presets.put(name, new NTrufflePigProp.PresetData());
        }
    }

    @Override
    protected void removePreset(String name) {
        if (prop != null && prop.presets != null) {
            prop.presets.remove(name);
        }
    }

    @Override
    protected Iterable<String> getPresetNames() {
        if (prop != null && prop.presets != null) {
            return prop.presets.keySet();
        }
        return Collections.emptyList();
    }

    @Override
    protected void onStartBot() {
        // TrufflePigHunter has no additional settings to save
    }
}
