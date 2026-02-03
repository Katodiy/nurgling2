package nurgling.widgets.bots;

import haven.*;
import nurgling.NUtils;
import nurgling.conf.NTrufflePigProp;
import nurgling.routes.ForagerPath;
import nurgling.routes.ForagerWaypoint;
import nurgling.widgets.TextInputWindow;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class TrufflePigHunter extends Window implements Checkable, PathRecordable {

    private Dropbox<String> presetDropbox = null;
    private Dropbox<String> pathDropbox = null;
    private Label sectionsLabel = null;
    private Button startButton = null;
    private IButton newPresetButton = null;
    private IButton deletePresetButton = null;
    private IButton newPathButton = null;
    private IButton deletePathButton = null;
    private ICheckBox recordPathButton = null;

    private final String pathDataDir = "trufflepig_paths";
    private boolean isRecording = false;
    private ForagerPath currentRecordingPath = null;

    private List<String> availablePresets = new ArrayList<>();
    private List<String> availablePaths = new ArrayList<>();

    private String lastPresetName = null;

    private Widget prev;

    public TrufflePigHunter() {
        super(new Coord(380, 200), "Truffle Pig Hunter");
        NTrufflePigProp startprop = NTrufflePigProp.get(NUtils.getUI().sessInfo);
        if (startprop == null) startprop = new NTrufflePigProp("", "");

        prev = add(new Label("Truffle Pig Hunter Settings"));

        // Preset selection
        prev = add(new Label("Preset:"), prev.pos("bl").add(UI.scale(0, 10)));

        loadAvailablePresets();

        Widget presetRow = add(new Widget(new Coord(UI.scale(300), UI.scale(20))), prev.pos("bl").add(UI.scale(0, 5)));

        presetDropbox = presetRow.add(new Dropbox<String>(UI.scale(230), availablePresets.size(), UI.scale(16)) {
            @Override
            protected String listitem(int i) {
                return availablePresets.get(i);
            }

            @Override
            protected int listitems() {
                return availablePresets.size();
            }

            @Override
            protected void drawitem(GOut g, String item, int i) {
                Text t = Text.render(item);
                Coord sz = t.sz();
                if (sz.x > UI.scale(280)) {
                    g.image(t.tex(), Coord.z, new Coord(UI.scale(280), sz.y));
                } else {
                    g.image(t.tex(), Coord.z);
                }
            }

            @Override
            public void change(String item) {
                super.change(item);
                onPresetChanged(item);
            }
        }, new Coord(0, 0));

        presetRow.add(newPresetButton = new IButton(
            Resource.loadsimg("nurgling/hud/buttons/add/u"),
            Resource.loadsimg("nurgling/hud/buttons/add/d"),
            Resource.loadsimg("nurgling/hud/buttons/add/h")) {
            @Override
            public void click() {
                super.click();
                createNewPreset();
            }
        }, new Coord(UI.scale(245), 0));
        newPresetButton.settip("Create new preset");

        presetRow.add(deletePresetButton = new IButton(
            Resource.loadsimg("nurgling/hud/buttons/remove/u"),
            Resource.loadsimg("nurgling/hud/buttons/remove/d"),
            Resource.loadsimg("nurgling/hud/buttons/remove/h")) {
            @Override
            public void click() {
                super.click();
                deleteCurrentPreset();
            }
        }, new Coord(UI.scale(270), 0));
        deletePresetButton.settip("Delete current preset");

        prev = presetRow;

        // Path selection
        prev = add(new Label("Path:"), prev.pos("bl").add(UI.scale(0, 10)));

        loadAvailablePaths();

        Widget pathRow = add(new Widget(new Coord(UI.scale(300), UI.scale(20))), prev.pos("bl").add(UI.scale(0, 5)));

        pathDropbox = pathRow.add(new Dropbox<String>(UI.scale(230), availablePaths.size(), UI.scale(16)) {
            @Override
            protected String listitem(int i) {
                return availablePaths.get(i);
            }

            @Override
            protected int listitems() {
                return availablePaths.size();
            }

            @Override
            protected void drawitem(GOut g, String item, int i) {
                Text t = Text.render(item);
                Coord sz = t.sz();
                if (sz.x > UI.scale(280)) {
                    g.image(t.tex(), Coord.z, new Coord(UI.scale(280), sz.y));
                } else {
                    g.image(t.tex(), Coord.z);
                }
            }

            @Override
            public void change(String item) {
                super.change(item);
                onPathChanged(item);
            }
        }, new Coord(0, 0));

        pathRow.add(newPathButton = new IButton(
            Resource.loadsimg("nurgling/hud/buttons/add/u"),
            Resource.loadsimg("nurgling/hud/buttons/add/d"),
            Resource.loadsimg("nurgling/hud/buttons/add/h")) {
            @Override
            public void click() {
                super.click();
                createNewPath();
            }
        }, new Coord(UI.scale(245), 0));
        newPathButton.settip("Create new path");

        pathRow.add(deletePathButton = new IButton(
            Resource.loadsimg("nurgling/hud/buttons/remove/u"),
            Resource.loadsimg("nurgling/hud/buttons/remove/d"),
            Resource.loadsimg("nurgling/hud/buttons/remove/h")) {
            @Override
            public void click() {
                super.click();
                deleteCurrentPath();
            }
        }, new Coord(UI.scale(270), 0));
        deletePathButton.settip("Delete current path");

        prev = pathRow;

        // Record path button on separate row
        Widget recordRow = add(new Widget(new Coord(UI.scale(270), UI.scale(20))), prev.pos("bl").add(UI.scale(0, 5)));
        recordRow.add(new Label("Record:"), new Coord(0, UI.scale(2)));
        recordRow.add(recordPathButton = new ICheckBox(
            "nurgling/hud/buttons/record_4states/",
            "u",
            "d",
            "h",
            "dh") {
            @Override
            public void changed(boolean val) {
                super.changed(val);
                if (val) {
                    startRecording();
                } else {
                    stopRecording();
                }
            }
        }, new Coord(UI.scale(60), 0));
        recordPathButton.settip("Record path waypoints");

        prev = recordRow;

        // Sections info
        prev = sectionsLabel = add(new Label("No path loaded"), prev.pos("bl").add(UI.scale(0, 10)));

        // Start button
        prev = startButton = add(new Button(UI.scale(150), "Start") {
            @Override
            public void click() {
                super.click();
                startBot();
            }
        }, prev.pos("bl").add(UI.scale(0, 20)));

        startButton.disable(true);

        pack();

        // Select current preset if exists
        if (!startprop.currentPreset.isEmpty() && availablePresets.contains(startprop.currentPreset)) {
            lastPresetName = startprop.currentPreset;
            presetDropbox.change(startprop.currentPreset);

            NTrufflePigProp.PresetData preset = startprop.presets.get(startprop.currentPreset);
            if (preset != null && !preset.pathFile.isEmpty()) {
                try {
                    preset.foragerPath = ForagerPath.load(preset.pathFile);
                    String fileName = preset.pathFile.substring(preset.pathFile.lastIndexOf("\\") + 1);
                    fileName = fileName.replace(".json", "");
                    if (availablePaths.contains(fileName)) {
                        pathDropbox.change(fileName);
                    }
                    updateSectionsInfo();
                } catch (Exception e) {
                    // Path file not found or corrupted
                }
            }
        } else if (!availablePresets.isEmpty()) {
            lastPresetName = availablePresets.get(0);
            presetDropbox.change(availablePresets.get(0));
        }
    }

    private void loadAvailablePresets() {
        availablePresets.clear();
        prop = NTrufflePigProp.get(NUtils.getUI().sessInfo);
        if (prop != null && prop.presets != null) {
            availablePresets.addAll(prop.presets.keySet());
        }
        if (availablePresets.isEmpty()) {
            availablePresets.add("Default");
        }
    }

    private void loadAvailablePaths() {
        availablePaths.clear();

        Path defaultDir = NUtils.getDataFilePath(pathDataDir);
        File dir = defaultDir.toFile();

        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));
            if (files != null) {
                for (File file : files) {
                    String name = file.getName().replace(".json", "");
                    availablePaths.add(name);
                }
            }
        }

        if (availablePaths.isEmpty()) {
            availablePaths.add("No paths available");
        }
    }

    private void onPresetChanged(String presetName) {
        if (prop == null) {
            prop = NTrufflePigProp.get(NUtils.getUI().sessInfo);
        }

        prop.currentPreset = presetName;
        lastPresetName = presetName;

        NTrufflePigProp.PresetData preset = prop.presets.get(presetName);
        if (preset != null) {
            if (!preset.pathFile.isEmpty()) {
                loadPath(preset.pathFile);
                String fileName = preset.pathFile.substring(preset.pathFile.lastIndexOf("\\") + 1);
                fileName = fileName.replace(".json", "");
                if (availablePaths.contains(fileName)) {
                    pathDropbox.change(fileName);
                }
            } else {
                sectionsLabel.settext("No path loaded");
                startButton.disable(true);
                if (!availablePaths.isEmpty()) {
                    pathDropbox.change(availablePaths.get(0));
                }
            }
        }

        NTrufflePigProp.set(prop);
    }

    private void onPathChanged(String pathName) {
        if (pathName.equals("No paths available")) {
            return;
        }

        String pathFile = NUtils.getDataFile(pathDataDir, pathName + ".json");
        loadPath(pathFile);
    }

    private void createNewPreset() {
        TextInputWindow inputWindow = new TextInputWindow("New Preset", "Enter preset name:", presetName -> {
            if (presetName != null && !presetName.trim().isEmpty()) {
                prop = NTrufflePigProp.get(NUtils.getUI().sessInfo);
                prop.presets.put(presetName.trim(), new NTrufflePigProp.PresetData());
                prop.currentPreset = presetName.trim();
                NTrufflePigProp.set(prop);

                availablePresets.add(presetName.trim());
                presetDropbox.change(presetName.trim());

                sectionsLabel.settext("No path loaded");
                startButton.disable(true);
            }
        });
        NUtils.getGameUI().add(inputWindow, UI.scale(200, 200));
        inputWindow.show();
    }

    private void createNewPath() {
        TextInputWindow inputWindow = new TextInputWindow("New Path", "Enter path name:", pathName -> {
            if (pathName != null && !pathName.trim().isEmpty()) {
                ForagerPath newPath = new ForagerPath(pathName.trim());

                try {
                    Path defaultDir = NUtils.getDataFilePath(pathDataDir);
                    File dir = defaultDir.toFile();
                    if (!dir.exists()) {
                        dir.mkdirs();
                    }

                    newPath.save(defaultDir.toString());
                    String pathFile = defaultDir.resolve(pathName.trim() + ".json").toString();

                    prop = NTrufflePigProp.get(NUtils.getUI().sessInfo);
                    NTrufflePigProp.PresetData preset = prop.presets.get(prop.currentPreset);
                    if (preset != null) {
                        preset.pathFile = pathFile;
                        preset.foragerPath = newPath;
                    }
                    NTrufflePigProp.set(prop);

                    loadAvailablePaths();
                    pathDropbox.change(pathName.trim());

                    updateSectionsInfo();
                } catch (Exception e) {
                    NUtils.getGameUI().error("Failed to create path: " + e.getMessage());
                }
            }
        });
        NUtils.getGameUI().add(inputWindow, UI.scale(200, 200));
        inputWindow.show();
    }

    private void loadPath(String filePath) {
        try {
            ForagerPath path = ForagerPath.load(filePath);
            prop = NTrufflePigProp.get(NUtils.getUI().sessInfo);

            NTrufflePigProp.PresetData preset = prop.presets.get(prop.currentPreset);
            if (preset != null) {
                preset.pathFile = filePath;
                preset.foragerPath = path;
            }
            NTrufflePigProp.set(prop);

            updateSectionsInfo();
            startButton.disable(path.getSectionCount() == 0);

        } catch (Exception e) {
            NUtils.getGameUI().error("Failed to load path: " + e.getMessage());
            sectionsLabel.settext("Failed to load path");
            startButton.disable(true);
        }
    }

    private void updateSectionsInfo() {
        if (prop != null) {
            NTrufflePigProp.PresetData preset = prop.presets.get(prop.currentPreset);
            if (preset != null && preset.foragerPath != null) {
                ForagerPath path = preset.foragerPath;
                sectionsLabel.settext(String.format("Path: %s (%d waypoints, %d sections)",
                    path.name, path.waypoints.size(), path.getSectionCount()));
                startButton.disable(path.getSectionCount() == 0);
                return;
            }
        }
        sectionsLabel.settext("No path loaded");
        startButton.disable(true);
    }

    private void startBot() {
        prop = NTrufflePigProp.get(NUtils.getUI().sessInfo);

        NTrufflePigProp.PresetData preset = prop.presets.get(prop.currentPreset);
        if (preset == null || preset.foragerPath == null || preset.foragerPath.getSectionCount() == 0) {
            NUtils.getGameUI().error("No valid path loaded");
            return;
        }

        NTrufflePigProp.set(prop);
        isReady = true;
    }

    @Override
    public boolean check() {
        return isReady;
    }

    boolean isReady = false;

    @Override
    public void wdgmsg(String msg, Object... args) {
        if (msg.equals("close")) {
            isReady = true;
            hide();
        }
        super.wdgmsg(msg, args);
    }

    public NTrufflePigProp prop = null;

    private void startRecording() {
        if (pathDropbox.sel != null && !pathDropbox.sel.equals("No paths available")) {
            String pathFile = NUtils.getDataFile(pathDataDir, pathDropbox.sel + ".json");
            try {
                currentRecordingPath = ForagerPath.load(pathFile);
                isRecording = true;
            } catch (Exception e) {
                NUtils.getGameUI().error("Failed to load path for recording: " + e.getMessage());
                recordPathButton.a = false;
                return;
            }
        } else {
            NUtils.getGameUI().error("Please create or select a path first");
            recordPathButton.a = false;
            return;
        }
    }

    private void stopRecording() {
        if (currentRecordingPath != null && isRecording) {
            currentRecordingPath.generateSections();

            try {
                Path defaultDir = NUtils.getDataFilePath(pathDataDir);
                currentRecordingPath.save(defaultDir.toString());

                prop = NTrufflePigProp.get(NUtils.getUI().sessInfo);
                NTrufflePigProp.PresetData preset = prop.presets.get(prop.currentPreset);
                if (preset != null) {
                    String pathFile = defaultDir.resolve(currentRecordingPath.name + ".json").toString();
                    preset.pathFile = pathFile;
                    preset.foragerPath = currentRecordingPath;
                    NTrufflePigProp.set(prop);
                    updateSectionsInfo();
                }
            } catch (Exception e) {
                NUtils.getGameUI().error("Failed to save path: " + e.getMessage());
            }
        }
        isRecording = false;
        currentRecordingPath = null;
    }

    @Override
    public boolean isRecording() {
        return isRecording;
    }

    @Override
    public void addWaypointToRecording(ForagerWaypoint wp) {
        if (isRecording && currentRecordingPath != null) {
            currentRecordingPath.addWaypoint(wp);
        }
    }

    public ForagerPath getCurrentRecordingPath() {
        return currentRecordingPath;
    }

    @Override
    public ForagerPath getCurrentLoadedPath() {
        if (isRecording && currentRecordingPath != null) {
            return currentRecordingPath;
        }

        prop = NTrufflePigProp.get(NUtils.getUI().sessInfo);
        if (prop != null && prop.currentPreset != null && !prop.currentPreset.isEmpty()) {
            NTrufflePigProp.PresetData preset = prop.presets.get(prop.currentPreset);
            if (preset != null) {
                return preset.foragerPath;
            }
        }
        return null;
    }

    private void deleteCurrentPreset() {
        if (presetDropbox.sel == null || presetDropbox.sel.isEmpty()) {
            return;
        }

        String presetToDelete = presetDropbox.sel;

        if (availablePresets.size() <= 1) {
            NUtils.getGameUI().error("Cannot delete the last preset");
            return;
        }

        int currentIndex = availablePresets.indexOf(presetToDelete);

        prop = NTrufflePigProp.get(NUtils.getUI().sessInfo);
        prop.presets.remove(presetToDelete);

        availablePresets.remove(presetToDelete);

        int newIndex = Math.max(0, currentIndex - 1);
        String newPreset = availablePresets.get(newIndex);
        prop.currentPreset = newPreset;
        lastPresetName = newPreset;

        NTrufflePigProp.set(prop);

        presetDropbox.change(newPreset);

        NTrufflePigProp.PresetData preset = prop.presets.get(newPreset);
        if (preset != null) {
            if (!preset.pathFile.isEmpty()) {
                try {
                    preset.foragerPath = ForagerPath.load(preset.pathFile);
                    String fileName = preset.pathFile.substring(preset.pathFile.lastIndexOf("\\") + 1);
                    fileName = fileName.replace(".json", "");
                    if (availablePaths.contains(fileName)) {
                        pathDropbox.change(fileName);
                    }
                    updateSectionsInfo();
                } catch (Exception e) {
                    sectionsLabel.settext("No path loaded");
                    startButton.disable(true);
                }
            } else {
                sectionsLabel.settext("No path loaded");
                startButton.disable(true);
            }
        }
    }

    private void deleteCurrentPath() {
        if (pathDropbox.sel == null || pathDropbox.sel.equals("No paths available")) {
            return;
        }

        String pathToDelete = pathDropbox.sel;
        Path pathFile = NUtils.getDataFilePath(pathDataDir, pathToDelete + ".json");

        File file = pathFile.toFile();
        if (file.exists()) {
            if (!file.delete()) {
                NUtils.getGameUI().error("Failed to delete path file");
                return;
            }
        }

        prop = NTrufflePigProp.get(NUtils.getUI().sessInfo);
        NTrufflePigProp.PresetData preset = prop.presets.get(prop.currentPreset);
        if (preset != null) {
            if (preset.pathFile.contains(pathToDelete)) {
                preset.pathFile = "";
                preset.foragerPath = null;
            }
        }
        NTrufflePigProp.set(prop);

        int currentIndex = availablePaths.indexOf(pathToDelete);

        availablePaths.remove(pathToDelete);

        if (availablePaths.isEmpty()) {
            availablePaths.add("No paths available");
            pathDropbox.change("No paths available");
        } else {
            int newIndex = Math.max(0, currentIndex - 1);
            pathDropbox.change(availablePaths.get(newIndex));
        }

        updateSectionsInfo();
    }
}
