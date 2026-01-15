package nurgling.widgets.options;

import haven.*;
import haven.Button;
import haven.Label;
import nurgling.*;
import nurgling.areas.NArea;
import nurgling.conf.QuickActionPreset;
import nurgling.widgets.TextInputWindow;
import nurgling.widgets.nsettings.Panel;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class QuickActions extends Panel {
    public ArrayList<ActionsItem> patterns = new ArrayList<>();
    ActionList al;
    TextEntry newPattern;
    int width = UI.scale(210);
    private HSlider rangeSlider;
    private Label dpy;
    private CheckBox visitorCheck;
    private CheckBox doorCheck;

    // Preset management
    private Dropbox<QuickActionPreset> presetDropbox;
    private ArrayList<QuickActionPreset> presets = new ArrayList<>();
    private QuickActionPreset currentPreset;
    private KeyMatch.Capture keybindButton;
    private Label keybindLabel;
    private IButton addPresetButton;
    private IButton removePresetButton;

    public QuickActions() {
        final int margin = UI.scale(10);

        // Preset selection row
        Label presetLabel = add(new Label("Preset:"), new Coord(margin, margin));
        
        presetDropbox = add(new Dropbox<QuickActionPreset>(UI.scale(120), 10, UI.scale(16)) {
            @Override
            protected QuickActionPreset listitem(int i) {
                return presets.get(i);
            }

            @Override
            protected int listitems() {
                return presets.size();
            }

            @Override
            protected void drawitem(GOut g, QuickActionPreset item, int i) {
                g.text(item.name, Coord.z);
            }

            @Override
            public void change(QuickActionPreset item) {
                super.change(item);
                if (item != null) {
                    switchToPreset(item);
                }
            }
        }, new Coord(presetLabel.pos("ur").x + UI.scale(5), margin + UI.scale(2)));

        // Add preset button (+)
        addPresetButton = add(new IButton(
            Resource.loadsimg("nurgling/hud/buttons/add/u"),
            Resource.loadsimg("nurgling/hud/buttons/add/d"),
            Resource.loadsimg("nurgling/hud/buttons/add/h")) {
            @Override
            public void click() {
                createNewPreset();
            }
        }, new Coord(presetDropbox.pos("ur").x + UI.scale(5), margin ));
        addPresetButton.settip("Create new preset");

        // Remove preset button (-)
        removePresetButton = add(new IButton(
            Resource.loadsimg("nurgling/hud/buttons/remove/u"),
            Resource.loadsimg("nurgling/hud/buttons/remove/d"),
            Resource.loadsimg("nurgling/hud/buttons/remove/h")) {
            @Override
            public void click() {
                deleteCurrentPreset();
            }
        }, new Coord(addPresetButton.pos("ur").x + UI.scale(2), margin));
        removePresetButton.settip("Delete selected preset (cannot delete Default)");

        // Keybind row
        keybindLabel = add(new Label("Keybind:"), new Coord(margin, presetDropbox.pos("bl").y + UI.scale(30)));
        keybindButton = add(new KeyMatch.Capture(UI.scale(120), KeyMatch.nil) {
            @Override
            public void set(KeyMatch key) {
                super.set(key);
                if (currentPreset != null) {
                    currentPreset.keybind = KeyMatch.reduce(key);
                }
            }
        }, new Coord(keybindLabel.pos("ur").x + UI.scale(5), keybindLabel.c.y + (keybindLabel.sz.y - UI.scale(30)) / 2));

        // Actions list
        prev = add(al = new ActionList(new Coord(width, UI.scale(220))), new Coord(margin, keybindButton.pos("bl").y + UI.scale(10)));

        // Add pattern row
        prev = add(newPattern = new TextEntry(UI.scale(175), ""), prev.pos("bl").adds(0, 10));
        IButton addPatternButton = add(new IButton(
            Resource.loadsimg("nurgling/hud/buttons/add/u"),
            Resource.loadsimg("nurgling/hud/buttons/add/d"),
            Resource.loadsimg("nurgling/hud/buttons/add/h")) {
            @Override
            public void click() {
                if (!newPattern.text().isEmpty()) {
                    ActionsItem ai = new ActionsItem(newPattern.text());
                    ai.isEnabled.a = true;
                    patterns.add(ai);
                    newPattern.settext("");
                }
            }
        }, new Coord(newPattern.pos("ur").x + UI.scale(5), newPattern.c.y + (newPattern.sz.y - UI.scale(18)) / 2));
        addPatternButton.settip("Add pattern");

        dpy = new Label("");
        rangeSlider = new HSlider(UI.scale(160), 1, 10, 1) {
            protected void added() {
                updateDpyLabel();
            }
            public void changed() {
                updateDpyLabel();
            }
        };

        // Correctly update prev for chaining layout
        addhlp(prev.pos("bl").adds(0, UI.scale(10)), UI.scale(5), rangeSlider, dpy);
        prev = rangeSlider;
        prev.settip("Set range of quick actions in tiles.", true);

        prev = visitorCheck = add(new CheckBox("Disable opening/closing visitor gates"), prev.pos("bl").adds(0, 5));
        prev = doorCheck = add(new CheckBox("Walking into doors in basic mode"), prev.pos("bl").adds(0, 5));
        pack();
        load();
    }

    private void createNewPreset() {
        TextInputWindow inputWindow = new TextInputWindow("New Preset", "Enter preset name:", presetName -> {
            if (presetName != null && !presetName.trim().isEmpty()) {
                String name = presetName.trim();
                // Check if preset with this name already exists
                if (getPresetByName(name) != null) {
                    NUtils.getGameUI().error("Preset with this name already exists");
                    return;
                }
                QuickActionPreset newPreset = new QuickActionPreset(name);
                presets.add(newPreset);
                switchToPreset(newPreset);
                presetDropbox.change(newPreset);
            }
        });
        NUtils.getGameUI().add(inputWindow, UI.scale(200, 200));
        inputWindow.show();
    }

    private void deleteCurrentPreset() {
        if (currentPreset == null) return;
        
        // Cannot delete Default preset
        if ("Default".equals(currentPreset.name)) {
            NUtils.getGameUI().error("Cannot delete the Default preset");
            return;
        }
        
        // Cannot delete if it's the only preset
        if (presets.size() <= 1) {
            NUtils.getGameUI().error("Cannot delete the last preset");
            return;
        }
        
        presets.remove(currentPreset);
        
        // Switch to first available preset
        if (!presets.isEmpty()) {
            switchToPreset(presets.get(0));
            presetDropbox.change(presets.get(0));
        }
    }

    private QuickActionPreset getPresetByName(String name) {
        for (QuickActionPreset preset : presets) {
            if (preset.name.equals(name)) {
                return preset;
            }
        }
        return null;
    }

    private void switchToPreset(QuickActionPreset preset) {
        // Save current preset patterns before switching
        if (currentPreset != null) {
            saveCurrentPresetPatterns();
        }

        currentPreset = preset;
        NConfig.set(NConfig.Key.q_current_preset, preset.name);

        // Load patterns from new preset
        patterns.clear();
        for (HashMap<String, Object> item : preset.patterns) {
            ActionsItem aitem = new ActionsItem((String) item.get("name"));
            aitem.isEnabled.a = (Boolean) item.getOrDefault("enabled", true);
            patterns.add(aitem);
        }

        // Update keybind button
        KeyMatch key = KeyMatch.restore(preset.keybind);
        keybindButton.set(key != null ? key : KeyMatch.nil);

        if (al != null) {
            al.update();
        }
    }

    private void saveCurrentPresetPatterns() {
        if (currentPreset == null) return;

        currentPreset.patterns.clear();
        for (ActionsItem pattern : patterns) {
            HashMap<String, Object> map = new HashMap<>();
            map.put("type", "NPattern");
            map.put("name", pattern.text());
            map.put("enabled", pattern.isEnabled.a);
            currentPreset.patterns.add(map);
        }
        currentPreset.keybind = KeyMatch.reduce(keybindButton.key);
    }

    private void updateDpyLabel() {
        if (dpy != null && rangeSlider != null)
            dpy.settext(rangeSlider.val + " tiles");
    }

    @Override
    @SuppressWarnings("unchecked")
    public void load() {
        presets.clear();
        patterns.clear();

        // Load presets from config
        Object presetsObj = NConfig.get(NConfig.Key.q_presets);
        if (presetsObj instanceof ArrayList) {
            ArrayList<?> presetsList = (ArrayList<?>) presetsObj;
            for (Object obj : presetsList) {
                if (obj instanceof QuickActionPreset) {
                    presets.add((QuickActionPreset) obj);
                } else if (obj instanceof HashMap) {
                    presets.add(new QuickActionPreset((HashMap<String, Object>) obj));
                }
            }
        }

        // Ensure at least the default preset exists
        if (presets.isEmpty()) {
            presets.add(QuickActionPreset.createDefault());
        }

        // Get current preset name from config
        String currentPresetName = (String) NConfig.get(NConfig.Key.q_current_preset);
        if (currentPresetName == null) {
            currentPresetName = "Default";
        }

        // Find and switch to current preset
        QuickActionPreset presetToSelect = getPresetByName(currentPresetName);
        if (presetToSelect == null) {
            presetToSelect = presets.get(0);
        }

        // Migration: if old q_pattern exists and Default preset is empty, migrate it
        if ("Default".equals(presetToSelect.name) && presetToSelect.patterns.isEmpty()) {
            Object oldPatterns = NConfig.get(NConfig.Key.q_pattern);
            if (oldPatterns instanceof ArrayList) {
                ArrayList<HashMap<String, Object>> oldPatternList = (ArrayList<HashMap<String, Object>>) oldPatterns;
                presetToSelect.patterns.addAll(oldPatternList);
            }
        }

        currentPreset = null; // Reset to trigger full load in switchToPreset
        switchToPreset(presetToSelect);
        presetDropbox.change(presetToSelect);

        int range = 1;
        Object rv = NConfig.get(NConfig.Key.q_range);
        if (rv instanceof Integer)
            range = (Integer) rv;
        rangeSlider.val = range;
        updateDpyLabel();
        visitorCheck.a = getBool(NConfig.Key.q_visitor);
        doorCheck.a = getBool(NConfig.Key.q_door);
        if (al != null)
            al.update();
    }

    @Override
    public void save() {
        // Save current preset patterns
        saveCurrentPresetPatterns();

        // Save all presets to config
        ArrayList<QuickActionPreset> presetsCopy = new ArrayList<>(presets);
        NConfig.set(NConfig.Key.q_presets, presetsCopy);
        NConfig.set(NConfig.Key.q_current_preset, currentPreset != null ? currentPreset.name : "Default");

        // Also save patterns to old q_pattern for backward compatibility
        ArrayList<HashMap<String, Object>> plist = new ArrayList<>();
        for (ActionsItem pattern : patterns) {
            HashMap<String, Object> map = new HashMap<>();
            map.put("name", pattern.text());
            map.put("enabled", pattern.isEnabled.a);
            plist.add(map);
        }
        NConfig.set(NConfig.Key.q_pattern, plist);
        NConfig.set(NConfig.Key.q_range, rangeSlider.val);
        NConfig.set(NConfig.Key.q_visitor, visitorCheck.a);
        NConfig.set(NConfig.Key.q_door, doorCheck.a);
        NConfig.needUpdate();
    }

    /**
     * Get the currently selected preset
     */
    public QuickActionPreset getCurrentPreset() {
        return currentPreset;
    }

    /**
     * Get all presets
     */
    public ArrayList<QuickActionPreset> getPresets() {
        return presets;
    }

    class ActionList extends SListBox<ActionsItem, Widget> {
        ActionList(Coord sz) {
            super(sz, UI.scale(22));
        }

        protected List<ActionsItem> items() {
            return patterns;
        }

        @Override
        public void resize(Coord sz) {
            super.resize(new Coord(width - UI.scale(6), sz.y));
        }

        protected Widget makeitem(ActionsItem item, int idx, Coord sz) {
            return new ItemWidget<ActionsItem>(this, sz, item) {
                {
                    add(item);
                    item.resize(sz);
                }

                @Override
                public void resize(Coord sz) {
                    super.resize(sz);
                    item.resize(sz);
                }
            };
        }

        @Override
        public void wdgmsg(String msg, Object... args) {
            super.wdgmsg(msg, args);
        }

        Color bg = new Color(30, 40, 40, 160);

        @Override
        public void draw(GOut g) {
            g.chcolor(bg);
            g.frect(Coord.z, g.sz());
            g.chcolor();
            super.draw(g);
        }
    }

    public class ActionsItem extends Widget {
        Label text;
        IButton remove;
        public CheckBox isEnabled;

        public NArea area;

        @Override
        public void resize(Coord sz) {
            if (isEnabled != null)
                isEnabled.move(new Coord(isEnabled.c.x, (sz.y - isEnabled.sz.y) / 2));
            if (text != null)
                text.move(new Coord(text.c.x, (sz.y - text.sz.y) / 2));
            if (remove != null)
                remove.move(new Coord(sz.x - NStyle.removei[0].sz().x - UI.scale(5),
                        (sz.y - remove.sz.y) / 2));
            super.resize(sz);
        }

        public ActionsItem(String text) {
            prev = isEnabled = add(new CheckBox("") {
                public void set(boolean val) {
                    a = val;
                }
            });
            this.text = add(new Label(text), prev.pos("ur").add(UI.scale(2), 0));
            remove = add(new IButton(NStyle.removei[0].back, NStyle.removei[1].back, NStyle.removei[2].back) {
                @Override
                public void click() {
                    patterns.remove(ActionsItem.this);
                }
            }, new Coord(al.sz.x - NStyle.removei[0].sz().x, 0).sub(UI.scale(5), UI.scale(1)));
            remove.settip(Resource.remote().loadwait("nurgling/hud/buttons/removeItem/u").flayer(Resource.tooltip).text());

            pack();
        }

        public String text() {
            return text.text();
        }
    }

    private boolean getBool(NConfig.Key key) {
        Object val = NConfig.get(key);
        return val instanceof Boolean ? (Boolean) val : false;
    }
}
