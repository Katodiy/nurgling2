package nurgling.widgets.bots;

import haven.*;
import nurgling.NUtils;
import nurgling.conf.NForagerProp;
import nurgling.i18n.L10n;
import nurgling.routes.ForagerAction;
import nurgling.routes.ForagerPath;
import nurgling.widgets.ActionConfigWindow;

import java.util.Collections;

public class Forager extends PathBotWindow {

    // Custom UI elements
    private Listbox<ForagerAction> actionsList = null;
    private IButton addActionButton = null;
    private IButton removeActionButton = null;

    Dropbox<String> onPlayerAction = null;
    Dropbox<String> onAnimalAction = null;
    Dropbox<String> afterFinishAction = null;
    Dropbox<String> onFullInventoryAction = null;
    CheckBox ignoreBatsCheckbox = null;
    CheckBox waterModeCheckbox = null;

    private static final String[] PLAYER_ACTIONS = {"nothing", "logout", "travel hearth"};
    private static final String[] ANIMAL_ACTIONS = {"logout", "travel hearth"};
    private static final String[] AFTER_FINISH_ACTIONS = {"nothing", "logout", "travel hearth"};
    private static final String[] FULL_INVENTORY_ACTIONS = {"nothing", "logout", "travel hearth"};

    public NForagerProp prop = null;
    private String lastPresetName = null;

    public Forager() {
        super(new Coord(380, 300), L10n.get("forager.wnd_title"));

        // Build common UI (preset, path, record, sections)
        prev = buildCommonUI(L10n.get("forager.settings"), L10n.get("forager.preset"), L10n.get("forager.path"));

        // Actions list
        prev = add(new Label(L10n.get("forager.actions")), prev.pos("bl").add(UI.scale(0, 10)));

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
        addActionButton.settip(L10n.get("forager.add_action_tip"));

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
        removeActionButton.settip(L10n.get("forager.remove_action_tip"));

        prev = actionsRow;

        // Player detection reaction
        prev = add(new Label(L10n.get("forager.on_player")), prev.pos("bl").add(UI.scale(0, 10)));
        prev = add(onPlayerAction = createSimpleDropbox(PLAYER_ACTIONS), prev.pos("bl").add(UI.scale(0, 5)));

        // Animal detection reaction
        prev = add(new Label(L10n.get("forager.on_animal")), prev.pos("bl").add(UI.scale(0, 10)));
        prev = add(onAnimalAction = createSimpleDropbox(ANIMAL_ACTIONS), prev.pos("bl").add(UI.scale(0, 5)));

        // After finish action
        prev = add(new Label(L10n.get("forager.after_finish")), prev.pos("bl").add(UI.scale(0, 10)));
        prev = add(afterFinishAction = createSimpleDropbox(AFTER_FINISH_ACTIONS), prev.pos("bl").add(UI.scale(0, 5)));

        // On full inventory action
        prev = add(new Label(L10n.get("forager.on_full_inv")), prev.pos("bl").add(UI.scale(0, 10)));
        prev = add(onFullInventoryAction = createSimpleDropbox(FULL_INVENTORY_ACTIONS), prev.pos("bl").add(UI.scale(0, 5)));

        // Ignore bats checkbox
        prev = add(ignoreBatsCheckbox = new CheckBox(L10n.get("forager.ignore_bats")), prev.pos("bl").add(UI.scale(0, 5)));

        // Water mode checkbox
        prev = add(waterModeCheckbox = new CheckBox(L10n.get("forager.water_mode")), prev.pos("bl").add(UI.scale(0, 5)));

        // Start button
        addStartButton();

        pack();

        // Initialize from config
        initializeFromConfig();
    }

    private Dropbox<String> createSimpleDropbox(String[] items) {
        return new Dropbox<String>(UI.scale(150), items.length, UI.scale(16)) {
            @Override
            protected String listitem(int i) {
                return items[i];
            }

            @Override
            protected int listitems() {
                return items.length;
            }

            @Override
            protected void drawitem(GOut g, String item, int i) {
                g.text(item, Coord.z);
            }
        };
    }

    // ========== Abstract method implementations ==========

    @Override
    protected String getPathDataDir() {
        return "forager_paths";
    }

    @Override
    protected String getNoPathsMessage() {
        return L10n.get("forager.no_paths");
    }

    @Override
    protected String loadPropAndGetCurrentPreset() {
        prop = NForagerProp.get(NUtils.getUI().sessInfo);
        if (prop == null) {
            prop = new NForagerProp("", "");
        }
        return prop.currentPreset;
    }

    @Override
    protected void saveProp() {
        if (prop != null) {
            NForagerProp.set(prop);
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
            lastPresetName = name;
        }
    }

    @Override
    protected String getPresetPathFile(String presetName) {
        if (prop != null && prop.presets != null) {
            NForagerProp.PresetData preset = prop.presets.get(presetName);
            if (preset != null) {
                return preset.pathFile;
            }
        }
        return null;
    }

    @Override
    protected void setPresetPathFile(String presetName, String pathFile) {
        if (prop != null && prop.presets != null) {
            NForagerProp.PresetData preset = prop.presets.get(presetName);
            if (preset != null) {
                preset.pathFile = pathFile;
            }
        }
    }

    @Override
    protected ForagerPath getPresetForagerPath(String presetName) {
        if (prop != null && prop.presets != null) {
            NForagerProp.PresetData preset = prop.presets.get(presetName);
            if (preset != null) {
                return preset.foragerPath;
            }
        }
        return null;
    }

    @Override
    protected void setPresetForagerPath(String presetName, ForagerPath path) {
        if (prop != null && prop.presets != null) {
            NForagerProp.PresetData preset = prop.presets.get(presetName);
            if (preset != null) {
                preset.foragerPath = path;
            }
        }
    }

    @Override
    protected void createPreset(String name) {
        if (prop != null) {
            prop.presets.put(name, new NForagerProp.PresetData());
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
    protected void onPresetLoaded(String presetName) {
        NForagerProp.PresetData preset = prop.presets.get(presetName);
        if (preset != null) {
            updateSafetyDropboxes(preset);
        }
        // Refresh actions list
        if (actionsList != null) {
            actionsList.change(null);
        }
    }

    @Override
    protected void onPresetSaving(String presetName) {
        // Save safety settings to the old preset before switching
        NForagerProp.PresetData preset = prop.presets.get(presetName);
        if (preset != null) {
            if (onPlayerAction.sel != null)
                preset.onPlayerAction = onPlayerAction.sel;
            if (onAnimalAction.sel != null)
                preset.onAnimalAction = onAnimalAction.sel;
            if (afterFinishAction.sel != null)
                preset.afterFinishAction = afterFinishAction.sel;
            if (onFullInventoryAction.sel != null)
                preset.onFullInventoryAction = onFullInventoryAction.sel;
            preset.ignoreBats = ignoreBatsCheckbox.a;
            preset.waterMode = waterModeCheckbox.a;
        }
    }

    @Override
    protected void onStartBot() {
        NForagerProp.PresetData preset = prop.presets.get(prop.currentPreset);
        if (preset != null) {
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
            preset.waterMode = waterModeCheckbox.a;
        }
    }

    // ========== Forager-specific methods ==========

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
        waterModeCheckbox.a = preset.waterMode;
    }

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
