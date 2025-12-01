package nurgling.widgets;

import haven.*;
import nurgling.NUtils;
import nurgling.areas.NArea;
import nurgling.actions.bots.registry.BotDescriptor;
import nurgling.actions.bots.registry.BotRegistry;
import nurgling.conf.NForagerProp;
import nurgling.scenarios.BotStep;
import java.util.*;

public class StepSettingsPanel extends Widget {
    private BotStep step;

    public StepSettingsPanel(Coord sz, BotStep step) {
        super(sz);
        this.step = step;
        updatePanel();
    }

    public void setStep(BotStep step) {
        this.step = step;
        clearChildren(this);
        updatePanel();
        pack();
    }

    private void updatePanel() {
        if (step == null) {
            String[] lines = {
                    "Select a step to view settings.",
                    "",
                    "Steps marked with ✪ have ",
                    "additional settings that ",
                    "can be changed."
            };
            int y = UI.scale(10);
            for (String line : lines) {
                add(new Label(line), new Coord(UI.scale(8), y));
                y += UI.scale(18);
            }
            return;
        }

        BotDescriptor desc = BotRegistry.byId(step.getId());
        if (desc == null) {
            add(new Label("Unknown bot."), new Coord(UI.scale(8), UI.scale(10)));
            return;
        }
        int y = UI.scale(10);
        boolean hasAnySetting = false;
        if (desc.id.equals("goto_area")) {
            hasAnySetting = true;
            add(new Label("Select Area:"), new Coord(UI.scale(8), y));
            y += UI.scale(24);

            List<NArea> areaList = new ArrayList<>(NUtils.getGameUI().map.glob.map.areas.values());
            areaList.sort(Comparator.comparing(a -> a.name));

            Integer current = (Integer) step.getSetting("areaId");
            NArea selectedArea = null;
            if (current != null && current > 0) {
                for (NArea area : areaList) {
                    if (area.id == current) {
                        selectedArea = area;
                        break;
                    }
                }
            }
            if (selectedArea == null && !areaList.isEmpty()) {
                selectedArea = areaList.get(0);
            }

            Dropbox<NArea> areaDropdown = new Dropbox<NArea>(
                    UI.scale(160),
                    Math.min(areaList.size(), 10),
                    UI.scale(22)
            ) {
                @Override
                protected NArea listitem(int i) { return areaList.get(i); }
                @Override
                protected int listitems() { return areaList.size(); }
                @Override
                protected void drawitem(GOut g, NArea item, int i) {
                    g.text(item.name + " [ID " + item.id + "]", Coord.z);
                }
                @Override
                public void change(NArea item) {
                    super.change(item);
                    if (item != null) {
                        step.setSetting("areaId", item.id);
                    }
                }
            };
            if (selectedArea != null) {
                areaDropdown.change(selectedArea);
            }

            add(areaDropdown, new Coord(UI.scale(8), y));
            y += UI.scale(40);
        }
        if (desc.id.equals("forager")) {
            hasAnySetting = true;
            add(new Label("Select Preset:"), new Coord(UI.scale(8), y));
            y += UI.scale(24);

            // Load presets from NForagerProp for current character
            NForagerProp foragerProp = NForagerProp.get(NUtils.getUI().sessInfo);
            List<String> presetNames = new ArrayList<>();
            if (foragerProp != null && foragerProp.presets != null) {
                presetNames.addAll(foragerProp.presets.keySet());
            }
            Collections.sort(presetNames);

            if (presetNames.isEmpty()) {
                add(new Label("No presets available."), new Coord(UI.scale(8), y));
                add(new Label("Create presets in the"), new Coord(UI.scale(8), y + UI.scale(18)));
                add(new Label("Forager bot first."), new Coord(UI.scale(8), y + UI.scale(36)));
                y += UI.scale(60);
            } else {
                String currentPreset = (String) step.getSetting("presetName");
                String selectedPreset = null;
                if (currentPreset != null && presetNames.contains(currentPreset)) {
                    selectedPreset = currentPreset;
                } else if (!presetNames.isEmpty()) {
                    selectedPreset = presetNames.get(0);
                }

                final List<String> finalPresetNames = presetNames;
                Dropbox<String> presetDropdown = new Dropbox<String>(
                        UI.scale(160),
                        Math.min(presetNames.size(), 10),
                        UI.scale(22)
                ) {
                    @Override
                    protected String listitem(int i) { return finalPresetNames.get(i); }
                    @Override
                    protected int listitems() { return finalPresetNames.size(); }
                    @Override
                    protected void drawitem(GOut g, String item, int i) {
                        g.text(item, Coord.z);
                    }
                    @Override
                    public void change(String item) {
                        super.change(item);
                        if (item != null) {
                            step.setSetting("presetName", item);
                        }
                    }
                };
                if (selectedPreset != null) {
                    presetDropdown.change(selectedPreset);
                }

                add(presetDropdown, new Coord(UI.scale(8), y));
                y += UI.scale(40);
            }
        }
        if (!hasAnySetting) {
            add(new Label("No settings for this step."), new Coord(UI.scale(8), y));
        }
    }

    private void clearChildren(Widget parent) {
        Widget child = parent.child;
        while (child != null) {
            Widget next = child.next;
            child.destroy();
            child = next;
        }
    }
}
