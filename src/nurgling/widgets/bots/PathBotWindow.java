package nurgling.widgets.bots;

import haven.*;
import nurgling.NUtils;
import nurgling.routes.ForagerPath;
import nurgling.routes.ForagerWaypoint;
import nurgling.widgets.TextInputWindow;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Abstract base class for bot windows that use path-based navigation.
 * Handles common UI elements: preset dropdown, path dropdown, record button, sections label, start button.
 * Subclasses must implement prop-specific operations and can add custom UI elements.
 */
public abstract class PathBotWindow extends Window implements Checkable, PathRecordable {

    // Common UI elements
    protected Dropbox<String> presetDropbox = null;
    protected Dropbox<String> pathDropbox = null;
    protected Label sectionsLabel = null;
    protected Button startButton = null;
    protected IButton newPresetButton = null;
    protected IButton deletePresetButton = null;
    protected IButton newPathButton = null;
    protected IButton deletePathButton = null;
    protected ICheckBox recordPathButton = null;

    // Recording state
    protected boolean isRecording = false;
    protected ForagerPath currentRecordingPath = null;

    // Available options
    protected List<String> availablePresets = new ArrayList<>();
    protected List<String> availablePaths = new ArrayList<>();

    // Ready state
    protected boolean isReady = false;
    protected Widget prev;

    // ========== Abstract methods that subclasses must implement ==========

    /** Returns the directory name for storing path files (e.g., "forager_paths") */
    protected abstract String getPathDataDir();

    /** Returns the "no paths available" message for this bot */
    protected abstract String getNoPathsMessage();

    /** Loads the prop from config and returns current preset name, or null if not found */
    protected abstract String loadPropAndGetCurrentPreset();

    /** Saves the prop to config */
    protected abstract void saveProp();

    /** Gets current preset name from prop */
    protected abstract String getCurrentPresetName();

    /** Sets current preset name in prop */
    protected abstract void setCurrentPresetName(String name);

    /** Gets preset path file */
    protected abstract String getPresetPathFile(String presetName);

    /** Sets preset path file */
    protected abstract void setPresetPathFile(String presetName, String pathFile);

    /** Gets preset forager path */
    protected abstract ForagerPath getPresetForagerPath(String presetName);

    /** Sets preset forager path */
    protected abstract void setPresetForagerPath(String presetName, ForagerPath path);

    /** Creates a new preset with the given name */
    protected abstract void createPreset(String name);

    /** Removes a preset by name */
    protected abstract void removePreset(String name);

    /** Returns all preset names */
    protected abstract Iterable<String> getPresetNames();

    /** Called when the start button is clicked. Subclasses should save any additional settings. */
    protected abstract void onStartBot();

    /** Called when preset changes. Subclasses can update additional UI elements. */
    protected void onPresetLoaded(String presetName) {}

    /** Called before switching away from a preset. Subclasses can save additional settings. */
    protected void onPresetSaving(String presetName) {}

    // ========== Constructor ==========

    public PathBotWindow(Coord sz, String title) {
        super(sz, title);
    }

    /**
     * Builds the common UI. Call this from subclass constructor.
     * Returns the last widget added, for positioning additional UI.
     */
    protected Widget buildCommonUI(String settingsLabel, String presetLabel, String pathLabel) {
        prev = add(new Label(settingsLabel));

        // Preset selection
        prev = add(new Label(presetLabel), prev.pos("bl").add(UI.scale(0, 10)));

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
                handlePresetChanged(item);
            }
        }, new Coord(0, 0));

        presetRow.add(newPresetButton = new IButton(
            Resource.loadsimg("nurgling/hud/buttons/add/u"),
            Resource.loadsimg("nurgling/hud/buttons/add/d"),
            Resource.loadsimg("nurgling/hud/buttons/add/h")) {
            @Override
            public void click() {
                super.click();
                handleCreateNewPreset();
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
                handleDeleteCurrentPreset();
            }
        }, new Coord(UI.scale(270), 0));
        deletePresetButton.settip("Delete current preset");

        prev = presetRow;

        // Path selection
        prev = add(new Label(pathLabel), prev.pos("bl").add(UI.scale(0, 10)));

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
                handlePathChanged(item);
            }
        }, new Coord(0, 0));

        pathRow.add(newPathButton = new IButton(
            Resource.loadsimg("nurgling/hud/buttons/add/u"),
            Resource.loadsimg("nurgling/hud/buttons/add/d"),
            Resource.loadsimg("nurgling/hud/buttons/add/h")) {
            @Override
            public void click() {
                super.click();
                handleCreateNewPath();
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
                handleDeleteCurrentPath();
            }
        }, new Coord(UI.scale(270), 0));
        deletePathButton.settip("Delete current path");

        prev = pathRow;

        // Record path button
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

        return prev;
    }

    /** Adds the start button. Call after adding any custom UI. */
    protected void addStartButton() {
        prev = startButton = add(new Button(UI.scale(150), "Start") {
            @Override
            public void click() {
                super.click();
                handleStartBot();
            }
        }, prev.pos("bl").add(UI.scale(0, 20)));

        startButton.disable(true);
    }

    /** Initializes the UI with the current preset. Call at end of constructor. */
    protected void initializeFromConfig() {
        String currentPreset = loadPropAndGetCurrentPreset();

        if (currentPreset != null && !currentPreset.isEmpty() && availablePresets.contains(currentPreset)) {
            presetDropbox.change(currentPreset);

            String pathFile = getPresetPathFile(currentPreset);
            if (pathFile != null && !pathFile.isEmpty()) {
                try {
                    ForagerPath path = ForagerPath.load(pathFile);
                    setPresetForagerPath(currentPreset, path);

                    String fileName = Paths.get(pathFile).getFileName().toString().replace(".json", "");
                    if (availablePaths.contains(fileName)) {
                        pathDropbox.change(fileName);
                    }
                    updateSectionsInfo();
                } catch (Exception e) {
                    // Path file not found or corrupted
                }
            }
        } else if (!availablePresets.isEmpty()) {
            presetDropbox.change(availablePresets.get(0));
        }
    }

    // ========== Common implementations ==========

    protected void loadAvailablePresets() {
        availablePresets.clear();
        for (String name : getPresetNames()) {
            availablePresets.add(name);
        }
        if (availablePresets.isEmpty()) {
            availablePresets.add("Default");
        }
    }

    protected void loadAvailablePaths() {
        availablePaths.clear();

        Path defaultDir = NUtils.getDataFilePath(getPathDataDir());
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
            availablePaths.add(getNoPathsMessage());
        }
    }

    protected void handlePresetChanged(String presetName) {
        // Let subclass save any additional settings for old preset
        String oldPreset = getCurrentPresetName();
        if (oldPreset != null && !oldPreset.equals(presetName)) {
            onPresetSaving(oldPreset);
        }

        setCurrentPresetName(presetName);

        String pathFile = getPresetPathFile(presetName);
        if (pathFile != null && !pathFile.isEmpty()) {
            loadPath(pathFile);
            String fileName = Paths.get(pathFile).getFileName().toString().replace(".json", "");
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

        // Let subclass update additional UI
        onPresetLoaded(presetName);

        saveProp();
    }

    protected void handlePathChanged(String pathName) {
        if (pathName.equals(getNoPathsMessage())) {
            return;
        }

        String pathFile = NUtils.getDataFile(getPathDataDir(), pathName + ".json");
        loadPath(pathFile);
    }

    protected void handleCreateNewPreset() {
        TextInputWindow inputWindow = new TextInputWindow("New Preset", "Enter preset name:", presetName -> {
            if (presetName != null && !presetName.trim().isEmpty()) {
                createPreset(presetName.trim());
                setCurrentPresetName(presetName.trim());
                saveProp();

                availablePresets.add(presetName.trim());
                presetDropbox.change(presetName.trim());

                sectionsLabel.settext("No path loaded");
                startButton.disable(true);
            }
        });
        NUtils.getGameUI().add(inputWindow, UI.scale(200, 200));
        inputWindow.show();
    }

    protected void handleCreateNewPath() {
        TextInputWindow inputWindow = new TextInputWindow("New Path", "Enter path name:", pathName -> {
            if (pathName != null && !pathName.trim().isEmpty()) {
                ForagerPath newPath = new ForagerPath(pathName.trim());

                try {
                    Path defaultDir = NUtils.getDataFilePath(getPathDataDir());
                    File dir = defaultDir.toFile();
                    if (!dir.exists()) {
                        dir.mkdirs();
                    }

                    newPath.save(defaultDir.toString());
                    String pathFile = defaultDir.resolve(pathName.trim() + ".json").toString();

                    String currentPreset = getCurrentPresetName();
                    setPresetPathFile(currentPreset, pathFile);
                    setPresetForagerPath(currentPreset, newPath);
                    saveProp();

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

    protected void loadPath(String filePath) {
        try {
            ForagerPath path = ForagerPath.load(filePath);

            String currentPreset = getCurrentPresetName();
            setPresetPathFile(currentPreset, filePath);
            setPresetForagerPath(currentPreset, path);
            saveProp();

            updateSectionsInfo();
            startButton.disable(path.getSectionCount() == 0);

        } catch (Exception e) {
            NUtils.getGameUI().error("Failed to load path: " + e.getMessage());
            sectionsLabel.settext("Failed to load path");
            startButton.disable(true);
        }
    }

    protected void updateSectionsInfo() {
        String currentPreset = getCurrentPresetName();
        ForagerPath path = getPresetForagerPath(currentPreset);

        if (path != null) {
            sectionsLabel.settext(String.format("Path: %s (%d waypoints, %d sections)",
                path.name, path.waypoints.size(), path.getSectionCount()));
            startButton.disable(path.getSectionCount() == 0);
            return;
        }
        sectionsLabel.settext("No path loaded");
        startButton.disable(true);
    }

    protected void handleStartBot() {
        String currentPreset = getCurrentPresetName();
        ForagerPath path = getPresetForagerPath(currentPreset);

        if (path == null || path.getSectionCount() == 0) {
            NUtils.getGameUI().error("No valid path loaded");
            return;
        }

        onStartBot();
        saveProp();
        isReady = true;
    }

    protected void handleDeleteCurrentPreset() {
        if (presetDropbox.sel == null || presetDropbox.sel.isEmpty()) {
            return;
        }

        String presetToDelete = presetDropbox.sel;

        if (availablePresets.size() <= 1) {
            NUtils.getGameUI().error("Cannot delete the last preset");
            return;
        }

        int currentIndex = availablePresets.indexOf(presetToDelete);

        removePreset(presetToDelete);
        availablePresets.remove(presetToDelete);

        int newIndex = Math.max(0, currentIndex - 1);
        String newPreset = availablePresets.get(newIndex);
        setCurrentPresetName(newPreset);

        saveProp();

        presetDropbox.change(newPreset);

        String pathFile = getPresetPathFile(newPreset);
        if (pathFile != null && !pathFile.isEmpty()) {
            try {
                ForagerPath path = ForagerPath.load(pathFile);
                setPresetForagerPath(newPreset, path);

                String fileName = Paths.get(pathFile).getFileName().toString().replace(".json", "");
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

    protected void handleDeleteCurrentPath() {
        if (pathDropbox.sel == null || pathDropbox.sel.equals(getNoPathsMessage())) {
            return;
        }

        String pathToDelete = pathDropbox.sel;
        Path pathFile = NUtils.getDataFilePath(getPathDataDir(), pathToDelete + ".json");

        File file = pathFile.toFile();
        if (file.exists()) {
            if (!file.delete()) {
                NUtils.getGameUI().error("Failed to delete path file");
                return;
            }
        }

        String currentPreset = getCurrentPresetName();
        String currentPathFile = getPresetPathFile(currentPreset);
        if (currentPathFile != null && currentPathFile.contains(pathToDelete)) {
            setPresetPathFile(currentPreset, "");
            setPresetForagerPath(currentPreset, null);
        }
        saveProp();

        int currentIndex = availablePaths.indexOf(pathToDelete);
        availablePaths.remove(pathToDelete);

        if (availablePaths.isEmpty()) {
            availablePaths.add(getNoPathsMessage());
            pathDropbox.change(getNoPathsMessage());
        } else {
            int newIndex = Math.max(0, currentIndex - 1);
            pathDropbox.change(availablePaths.get(newIndex));
        }

        updateSectionsInfo();
    }

    // ========== Recording ==========

    protected void startRecording() {
        if (pathDropbox.sel != null && !pathDropbox.sel.equals(getNoPathsMessage())) {
            String pathFile = NUtils.getDataFile(getPathDataDir(), pathDropbox.sel + ".json");
            try {
                currentRecordingPath = ForagerPath.load(pathFile);
                isRecording = true;
            } catch (Exception e) {
                NUtils.getGameUI().error("Failed to load path for recording: " + e.getMessage());
                recordPathButton.a = false;
            }
        } else {
            NUtils.getGameUI().error("Please create or select a path first");
            recordPathButton.a = false;
        }
    }

    protected void stopRecording() {
        if (currentRecordingPath != null && isRecording) {
            currentRecordingPath.generateSections();

            try {
                Path defaultDir = NUtils.getDataFilePath(getPathDataDir());
                currentRecordingPath.save(defaultDir.toString());

                String currentPreset = getCurrentPresetName();
                String pathFile = defaultDir.resolve(currentRecordingPath.name + ".json").toString();
                setPresetPathFile(currentPreset, pathFile);
                setPresetForagerPath(currentPreset, currentRecordingPath);
                saveProp();
                updateSectionsInfo();
            } catch (Exception e) {
                NUtils.getGameUI().error("Failed to save path: " + e.getMessage());
            }
        }
        isRecording = false;
        currentRecordingPath = null;
    }

    // ========== PathRecordable implementation ==========

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

    @Override
    public ForagerPath getCurrentLoadedPath() {
        if (isRecording && currentRecordingPath != null) {
            return currentRecordingPath;
        }

        String currentPreset = getCurrentPresetName();
        if (currentPreset != null && !currentPreset.isEmpty()) {
            return getPresetForagerPath(currentPreset);
        }
        return null;
    }

    // ========== Checkable implementation ==========

    @Override
    public boolean check() {
        return isReady;
    }

    @Override
    public void wdgmsg(String msg, Object... args) {
        if (msg.equals("close")) {
            isReady = true;
            hide();
        }
        super.wdgmsg(msg, args);
    }
}
