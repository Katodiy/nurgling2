package nurgling.widgets.bots;

import haven.*;
import nurgling.NUtils;
import nurgling.conf.NForagerProp;
import nurgling.routes.ForagerAction;
import nurgling.routes.ForagerPath;

import nurgling.widgets.ActionConfigWindow;
import nurgling.widgets.TextInputWindow;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Forager extends Window implements Checkable {

    private Dropbox<String> presetDropbox = null;
    private Dropbox<String> pathDropbox = null;
    private Label sectionsLabel = null;
    private Listbox<ForagerAction> actionsList = null;
    private IButton addActionButton = null;
    private IButton removeActionButton = null;
    private Button startButton = null;
    private IButton newPresetButton = null;
    private IButton deletePresetButton = null;
    private IButton newPathButton = null;
    private IButton deletePathButton = null;
    private ICheckBox recordPathButton = null;
    
    Dropbox<String> onPlayerAction = null;
    Dropbox<String> onAnimalAction = null;
    Dropbox<String> afterFinishAction = null;
    Dropbox<String> onFullInventoryAction = null;
    CheckBox ignoreBatsCheckbox = null;
    
    private static final String[] PLAYER_ACTIONS = {"nothing", "logout", "travel hearth"};
    private static final String[] ANIMAL_ACTIONS = {"logout", "travel hearth"};
    private static final String[] AFTER_FINISH_ACTIONS = {"nothing", "logout", "travel hearth"};
    private static final String[] FULL_INVENTORY_ACTIONS = {"nothing", "logout", "travel hearth"};
    
    // Recording state
    private boolean isRecording = false;
    private ForagerPath currentRecordingPath = null;
    
    private List<String> availablePresets = new ArrayList<>();
    private List<String> availablePaths = new ArrayList<>();
    
    private String lastPresetName = null;
    
    private Widget prev;
    
    public Forager() {
        super(new Coord(380, 300), "Forager Bot");
        NForagerProp startprop = NForagerProp.get(NUtils.getUI().sessInfo);
        if (startprop == null) startprop = new NForagerProp("", "");
        
        prev = add(new Label("Forager Bot Settings:"));
        
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
                // Clip text to dropbox width to prevent overflow
                Text t = Text.render(item);
                Coord sz = t.sz();
                if(sz.x > UI.scale(280)) {
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
                // Clip text to dropbox width to prevent overflow
                Text t = Text.render(item);
                Coord sz = t.sz();
                if(sz.x > UI.scale(280)) {
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
        recordPathButton.settip("Record path by clicking on minimap");
        
        prev = recordRow;
        
        // Sections info
        prev = sectionsLabel = add(new Label("No path loaded"), prev.pos("bl").add(UI.scale(0, 10)));
        
        // Actions list
        prev = add(new Label("Actions:"), prev.pos("bl").add(UI.scale(0, 10)));
        
        Widget actionsRow = add(new Widget(new Coord(UI.scale(270), UI.scale(120))), prev.pos("bl").add(UI.scale(0, 5)));
        
        actionsRow.add(actionsList = new Listbox<ForagerAction>(UI.scale(230), 6, UI.scale(20)) {
            @Override
            protected ForagerAction listitem(int i) {
                if (prop != null) {
                    NForagerProp.PresetData preset = prop.presets.get(prop.currentPreset);
                    if (preset != null && i < preset.actions.size()) {
                        return preset.actions.get(i);
                    }
                }
                return null;
            }
            
            @Override
            protected int listitems() {
                if (prop != null) {
                    NForagerProp.PresetData preset = prop.presets.get(prop.currentPreset);
                    if (preset != null) {
                        return preset.actions.size();
                    }
                }
                return 0;
            }
            
            @Override
            protected void drawitem(GOut g, ForagerAction item, int i) {
                if (item != null) {
                    String text = item.targetObjectPattern + " - " + item.actionType.name();
                    g.text(text, Coord.z);
                }
            }
        }, new Coord(0, 0));
        
        Widget actionsButtonsCol = actionsRow.add(new Widget(new Coord(UI.scale(30), UI.scale(120))), new Coord(UI.scale(245), 0));
        
        actionsButtonsCol.add(addActionButton = new IButton(
            Resource.loadsimg("nurgling/hud/buttons/add/u"),
            Resource.loadsimg("nurgling/hud/buttons/add/d"),
            Resource.loadsimg("nurgling/hud/buttons/add/h")) {
            @Override
            public void click() {
                super.click();
                addAction();
            }
        }, new Coord(0, 0));
        addActionButton.settip("Add action");
        
        actionsButtonsCol.add(removeActionButton = new IButton(
            Resource.loadsimg("nurgling/hud/buttons/remove/u"),
            Resource.loadsimg("nurgling/hud/buttons/remove/d"),
            Resource.loadsimg("nurgling/hud/buttons/remove/h")) {
            @Override
            public void click() {
                super.click();
                removeAction();
            }
        }, new Coord(0, UI.scale(30)));
        removeActionButton.settip("Remove action");
        
        prev = actionsRow;
        
        // Player detection reaction
        prev = add(new Label("On unknown player:"), prev.pos("bl").add(UI.scale(0, 10)));
        prev = add(onPlayerAction = new Dropbox<String>(UI.scale(150), PLAYER_ACTIONS.length, UI.scale(16)) {
            @Override
            protected String listitem(int i) {
                return PLAYER_ACTIONS[i];
            }

            @Override
            protected int listitems() {
                return PLAYER_ACTIONS.length;
            }

            @Override
            protected void drawitem(GOut g, String item, int i) {
                g.text(item, Coord.z);
            }
            
            @Override
            public void change(String item) {
                super.change(item);
            }
        }, prev.pos("bl").add(UI.scale(0, 5)));
        
        // Animal detection reaction
        prev = add(new Label("On dangerous animal:"), prev.pos("bl").add(UI.scale(0, 10)));
        prev = add(onAnimalAction = new Dropbox<String>(UI.scale(150), ANIMAL_ACTIONS.length, UI.scale(16)) {
            @Override
            protected String listitem(int i) {
                return ANIMAL_ACTIONS[i];
            }

            @Override
            protected int listitems() {
                return ANIMAL_ACTIONS.length;
            }

            @Override
            protected void drawitem(GOut g, String item, int i) {
                g.text(item, Coord.z);
            }
            
            @Override
            public void change(String item) {
                super.change(item);
            }
        }, prev.pos("bl").add(UI.scale(0, 5)));

        // After finish action
        prev = add(new Label("After finish:"), prev.pos("bl").add(UI.scale(0, 10)));
        prev = add(afterFinishAction = new Dropbox<String>(UI.scale(150), AFTER_FINISH_ACTIONS.length, UI.scale(16)) {
            @Override
            protected String listitem(int i) {
                return AFTER_FINISH_ACTIONS[i];
            }

            @Override
            protected int listitems() {
                return AFTER_FINISH_ACTIONS.length;
            }

            @Override
            protected void drawitem(GOut g, String item, int i) {
                g.text(item, Coord.z);
            }
            
            @Override
            public void change(String item) {
                super.change(item);
            }
        }, prev.pos("bl").add(UI.scale(0, 5)));

        // On full inventory action
        prev = add(new Label("On full inventory:"), prev.pos("bl").add(UI.scale(0, 10)));
        prev = add(onFullInventoryAction = new Dropbox<String>(UI.scale(150), FULL_INVENTORY_ACTIONS.length, UI.scale(16)) {
            @Override
            protected String listitem(int i) {
                return FULL_INVENTORY_ACTIONS[i];
            }

            @Override
            protected int listitems() {
                return FULL_INVENTORY_ACTIONS.length;
            }

            @Override
            protected void drawitem(GOut g, String item, int i) {
                g.text(item, Coord.z);
            }
            
            @Override
            public void change(String item) {
                super.change(item);
            }
        }, prev.pos("bl").add(UI.scale(0, 5)));
        
        // Ignore bats checkbox
        prev = add(ignoreBatsCheckbox = new CheckBox("Ignore bats"), prev.pos("bl").add(UI.scale(0, 5)));
        
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
            
            // Load path for current preset if exists
            NForagerProp.PresetData preset = startprop.presets.get(startprop.currentPreset);
            if (preset != null) {
                updateSafetyDropboxes(preset);
                
                if (!preset.pathFile.isEmpty()) {
                    try {
                        preset.foragerPath = ForagerPath.load(preset.pathFile);
                        
                        // Extract just the filename without extension
                        String fileName = preset.pathFile.substring(preset.pathFile.lastIndexOf("\\") + 1);
                        fileName = fileName.replace(".json", "");
                        
                        // Select in dropbox if exists
                        if (availablePaths.contains(fileName)) {
                            pathDropbox.change(fileName);
                        }
                        
                        updateSectionsInfo();
                    } catch (Exception e) {
                        // Path file not found or corrupted
                    }
                }
            }
        } else if (!availablePresets.isEmpty()) {
            lastPresetName = availablePresets.get(0);
            presetDropbox.change(availablePresets.get(0));
        }
    }
    
    private void updateSafetyDropboxes(NForagerProp.PresetData preset) {
        for (int i = 0; i < PLAYER_ACTIONS.length; i++) {
            if (PLAYER_ACTIONS[i].equals(preset.onPlayerAction)) {
                onPlayerAction.change(PLAYER_ACTIONS[i]);
                break;
            }
        }
        
        for (int i = 0; i < ANIMAL_ACTIONS.length; i++) {
            if (ANIMAL_ACTIONS[i].equals(preset.onAnimalAction)) {
                onAnimalAction.change(ANIMAL_ACTIONS[i]);
                break;
            }
        }
        
        for (int i = 0; i < AFTER_FINISH_ACTIONS.length; i++) {
            if (AFTER_FINISH_ACTIONS[i].equals(preset.afterFinishAction)) {
                afterFinishAction.change(AFTER_FINISH_ACTIONS[i]);
                break;
            }
        }
        
        for (int i = 0; i < FULL_INVENTORY_ACTIONS.length; i++) {
            if (FULL_INVENTORY_ACTIONS[i].equals(preset.onFullInventoryAction)) {
                onFullInventoryAction.change(FULL_INVENTORY_ACTIONS[i]);
                break;
            }
        }
        
        ignoreBatsCheckbox.a = preset.ignoreBats;
    }
    
    private void loadAvailablePresets() {
        availablePresets.clear();
        prop = NForagerProp.get(NUtils.getUI().sessInfo);
        if (prop != null && prop.presets != null) {
            availablePresets.addAll(prop.presets.keySet());
        }
        if (availablePresets.isEmpty()) {
            availablePresets.add("Default");
        }
    }
    
    private void loadAvailablePaths() {
        availablePaths.clear();
        
        String defaultDir = ((HashDirCache) ResCache.global).base + "\\..\\forager_paths";
        File dir = new File(defaultDir);
        
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
            prop = NForagerProp.get(NUtils.getUI().sessInfo);
        }
        
        // Save current safety settings to the old preset before switching
        if (lastPresetName != null && !lastPresetName.equals(presetName)) {
            NForagerProp.PresetData oldPreset = prop.presets.get(lastPresetName);
            if (oldPreset != null) {
                if (onPlayerAction.sel != null)
                    oldPreset.onPlayerAction = onPlayerAction.sel;
                if (onAnimalAction.sel != null)
                    oldPreset.onAnimalAction = onAnimalAction.sel;
                if (afterFinishAction.sel != null)
                    oldPreset.afterFinishAction = afterFinishAction.sel;
                if (onFullInventoryAction.sel != null)
                    oldPreset.onFullInventoryAction = onFullInventoryAction.sel;
                oldPreset.ignoreBats = ignoreBatsCheckbox.a;
            }
        }
        
        prop.currentPreset = presetName;
        lastPresetName = presetName;
        
        // Load path for this preset
        NForagerProp.PresetData preset = prop.presets.get(presetName);
        if (preset != null) {
            updateSafetyDropboxes(preset);
            
            if (!preset.pathFile.isEmpty()) {
                loadPath(preset.pathFile);
                
                // Update path dropbox to show the path for this preset
                String fileName = preset.pathFile.substring(preset.pathFile.lastIndexOf("\\") + 1);
                fileName = fileName.replace(".json", "");
                if (availablePaths.contains(fileName)) {
                    pathDropbox.change(fileName);
                }
            } else {
                sectionsLabel.settext("No path loaded");
                startButton.disable(true);
                // Reset path dropbox
                if (!availablePaths.isEmpty()) {
                    pathDropbox.change(availablePaths.get(0));
                }
            }
        }
        
        // Refresh actions list
        if (actionsList != null) {
            actionsList.change(null);
        }
        
        NForagerProp.set(prop);
    }
    
    private void onPathChanged(String pathName) {
        if (pathName.equals("No paths available")) {
            return;
        }
        
        String defaultDir = ((HashDirCache) ResCache.global).base + "\\..\\forager_paths";
        String pathFile = defaultDir + "\\" + pathName + ".json";
        
        // Load and save to current preset
        loadPath(pathFile);
    }
    
    private void createNewPreset() {
        TextInputWindow inputWindow = new TextInputWindow("New Preset", "Enter preset name:", presetName -> {
            if (presetName != null && !presetName.trim().isEmpty()) {
                prop = NForagerProp.get(NUtils.getUI().sessInfo);
                prop.presets.put(presetName.trim(), new NForagerProp.PresetData());
                prop.currentPreset = presetName.trim();
                NForagerProp.set(prop);
                
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
                
                // Save empty path
                try {
                    String defaultDir = ((HashDirCache) ResCache.global).base + "\\..\\forager_paths";
                    File dir = new File(defaultDir);
                    if (!dir.exists()) {
                        dir.mkdirs();
                    }
                    
                    newPath.save(defaultDir);
                    String pathFile = defaultDir + "\\" + pathName.trim() + ".json";
                    
                    // Save to current preset
                    prop = NForagerProp.get(NUtils.getUI().sessInfo);
                    NForagerProp.PresetData preset = prop.presets.get(prop.currentPreset);
                    if (preset != null) {
                        preset.pathFile = pathFile;
                        preset.foragerPath = newPath;
                    }
                    NForagerProp.set(prop);
                    
                    // Reload available paths and select new one
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
            prop = NForagerProp.get(NUtils.getUI().sessInfo);
            
            // Save to current preset
            NForagerProp.PresetData preset = prop.presets.get(prop.currentPreset);
            if (preset != null) {
                preset.pathFile = filePath;
                preset.foragerPath = path;
            }
            NForagerProp.set(prop);
            
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
            NForagerProp.PresetData preset = prop.presets.get(prop.currentPreset);
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
        prop = NForagerProp.get(NUtils.getUI().sessInfo);
        
        NForagerProp.PresetData preset = prop.presets.get(prop.currentPreset);
        if (preset == null || preset.foragerPath == null || preset.foragerPath.getSectionCount() == 0) {
            NUtils.getGameUI().error("No valid path loaded");
            return;
        }
        
        if (onPlayerAction.sel != null)
            preset.onPlayerAction = onPlayerAction.sel;
        else
            preset.onPlayerAction = "nothing";
            
        if (onAnimalAction.sel != null)
            preset.onAnimalAction = onAnimalAction.sel;
        else
            preset.onAnimalAction = "logout";
            
        if (afterFinishAction.sel != null)
            preset.afterFinishAction = afterFinishAction.sel;
        else
            preset.afterFinishAction = "nothing";
            
        if (onFullInventoryAction.sel != null)
            preset.onFullInventoryAction = onFullInventoryAction.sel;
        else
            preset.onFullInventoryAction = "nothing";
            
        preset.ignoreBats = ignoreBatsCheckbox.a;
        
        NForagerProp.set(prop);
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
    
    public NForagerProp prop = null;
    
    private void addAction() {
        ActionConfigWindow configWindow = new ActionConfigWindow(action -> {
            if (action != null) {
                prop = NForagerProp.get(NUtils.getUI().sessInfo);
                NForagerProp.PresetData preset = prop.presets.get(prop.currentPreset);
                if (preset != null) {
                    preset.actions.add(action);
                    NForagerProp.set(prop);
                    actionsList.change(null);
                }
            }
        });
        NUtils.getGameUI().add(configWindow, UI.scale(200, 200));
        configWindow.show();
    }
    
    private void removeAction() {
        ForagerAction selected = actionsList.sel;
        if (selected != null) {
            prop = NForagerProp.get(NUtils.getUI().sessInfo);
            NForagerProp.PresetData preset = prop.presets.get(prop.currentPreset);
            if (preset != null) {
                preset.actions.remove(selected);
                NForagerProp.set(prop);
                actionsList.change(null);
            }
        }
    }
    
    private void startRecording() {
        // Check if there's a current path selected
        if (pathDropbox.sel != null && !pathDropbox.sel.equals("No paths available")) {
            // Load existing path for editing
            String defaultDir = ((HashDirCache) ResCache.global).base + "\\..\\forager_paths";
            String pathFile = defaultDir + "\\" + pathDropbox.sel + ".json";
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
            // Generate sections from waypoints
            currentRecordingPath.generateSections();
            
            // Save the path
            try {
                String defaultDir = ((HashDirCache) ResCache.global).base + "\\..\\forager_paths";
                currentRecordingPath.save(defaultDir);
                
                // Update the preset with the saved path
                prop = NForagerProp.get(NUtils.getUI().sessInfo);
                NForagerProp.PresetData preset = prop.presets.get(prop.currentPreset);
                if (preset != null) {
                    String pathFile = defaultDir + "\\" + currentRecordingPath.name + ".json";
                    preset.pathFile = pathFile;
                    preset.foragerPath = currentRecordingPath;
                    NForagerProp.set(prop);
                    updateSectionsInfo();
                }
            } catch (Exception e) {
                NUtils.getGameUI().error("Failed to save path: " + e.getMessage());
            }
        }
        isRecording = false;
        currentRecordingPath = null;
    }
    
    public boolean isRecording() {
        return isRecording;
    }
    
    public void addWaypointToRecording(nurgling.routes.ForagerWaypoint wp) {
        if (isRecording && currentRecordingPath != null) {
            currentRecordingPath.addWaypoint(wp);
        }
    }
    
    public ForagerPath getCurrentRecordingPath() {
        return currentRecordingPath;
    }
    
    public ForagerPath getCurrentLoadedPath() {
        if (isRecording && currentRecordingPath != null) {
            return currentRecordingPath;
        }
        
        // Get path from current preset
        prop = NForagerProp.get(NUtils.getUI().sessInfo);
        if (prop != null && prop.currentPreset != null && !prop.currentPreset.isEmpty()) {
            NForagerProp.PresetData preset = prop.presets.get(prop.currentPreset);
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
        
        // Don't allow deleting if it's the last preset
        if (availablePresets.size() <= 1) {
            NUtils.getGameUI().error("Cannot delete the last preset");
            return;
        }
        
        // Find index of current preset
        int currentIndex = availablePresets.indexOf(presetToDelete);
        
        // Remove from prop
        prop = NForagerProp.get(NUtils.getUI().sessInfo);
        prop.presets.remove(presetToDelete);
        
        // Remove from local list
        availablePresets.remove(presetToDelete);
        
        // Select previous preset (or first if we deleted the first one)
        int newIndex = Math.max(0, currentIndex - 1);
        String newPreset = availablePresets.get(newIndex);
        prop.currentPreset = newPreset;
        lastPresetName = newPreset;
        
        NForagerProp.set(prop);
        
        // Update UI
        presetDropbox.change(newPreset);
        
        // Refresh actions list
        if (actionsList != null) {
            actionsList.change(null);
        }
        
        // Update path info for new preset
        NForagerProp.PresetData preset = prop.presets.get(newPreset);
        if (preset != null) {
            updateSafetyDropboxes(preset);
            
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
        String defaultDir = ((HashDirCache) ResCache.global).base + "\\..\\forager_paths";
        String pathFile = defaultDir + "\\" + pathToDelete + ".json";
        
        // Delete the file
        File file = new File(pathFile);
        if (file.exists()) {
            if (!file.delete()) {
                NUtils.getGameUI().error("Failed to delete path file");
                return;
            }
        }
        
        // Clear path from current preset
        prop = NForagerProp.get(NUtils.getUI().sessInfo);
        NForagerProp.PresetData preset = prop.presets.get(prop.currentPreset);
        if (preset != null) {
            // Only clear if this preset was using the deleted path
            if (preset.pathFile.contains(pathToDelete)) {
                preset.pathFile = "";
                preset.foragerPath = null;
            }
        }
        NForagerProp.set(prop);
        
        // Find index of current path
        int currentIndex = availablePaths.indexOf(pathToDelete);
        
        // Remove from local list
        availablePaths.remove(pathToDelete);
        
        // Update UI - select previous path or show "No paths available"
        if (availablePaths.isEmpty()) {
            availablePaths.add("No paths available");
            pathDropbox.change("No paths available");
        } else {
            int newIndex = Math.max(0, currentIndex - 1);
            pathDropbox.change(availablePaths.get(newIndex));
        }
        
        // Update sections info (this will clear the path display on minimap)
        updateSectionsInfo();
    }
}
