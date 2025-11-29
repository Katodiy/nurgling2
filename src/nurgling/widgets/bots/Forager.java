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
    private IButton newPathButton = null;
    
    private List<String> availablePresets = new ArrayList<>();
    private List<String> availablePaths = new ArrayList<>();
    
    private Widget prev;
    
    public Forager() {
        super(new Coord(320, 300), "Forager Bot");
        NForagerProp startprop = NForagerProp.get(NUtils.getUI().sessInfo);
        
        prev = add(new Label("Forager Bot Settings:"));
        
        // Preset selection
        prev = add(new Label("Preset:"), prev.pos("bl").add(UI.scale(0, 10)));
        
        loadAvailablePresets();
        
        Widget presetRow = add(new Widget(new Coord(UI.scale(270), UI.scale(20))), prev.pos("bl").add(UI.scale(0, 5)));
        
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
                g.text(item, Coord.z);
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
        
        prev = presetRow;
        
        // Path selection
        prev = add(new Label("Path:"), prev.pos("bl").add(UI.scale(0, 10)));
        
        loadAvailablePaths();
        
        Widget pathRow = add(new Widget(new Coord(UI.scale(270), UI.scale(20))), prev.pos("bl").add(UI.scale(0, 5)));
        
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
                g.text(item, Coord.z);
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
        
        prev = pathRow;
        
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
            presetDropbox.change(startprop.currentPreset);
            
            // Load path for current preset if exists
            NForagerProp.PresetData preset = startprop.presets.get(startprop.currentPreset);
            if (preset != null && !preset.pathFile.isEmpty()) {
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
        } else if (!availablePresets.isEmpty()) {
            presetDropbox.change(availablePresets.get(0));
        }
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
        prop.currentPreset = presetName;
        
        // Load path for this preset
        NForagerProp.PresetData preset = prop.presets.get(presetName);
        if (preset != null && !preset.pathFile.isEmpty()) {
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
                    
                    NUtils.getGameUI().msg("Path created. Now click on minimap to add waypoints.");
                    
                    // TODO: Enable path creation mode on minimap
                    
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
}
