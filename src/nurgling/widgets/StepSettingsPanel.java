package nurgling.widgets;

import haven.*;
import nurgling.NUtils;
import nurgling.areas.NArea;
import nurgling.actions.bots.WaitBot;
import nurgling.actions.bots.registry.BotDescriptor;
import nurgling.actions.bots.registry.BotRegistry;
import nurgling.conf.NForagerProp;
import nurgling.equipment.EquipmentPreset;
import nurgling.scenarios.BotStep;
import nurgling.scenarios.CraftPreset;
import nurgling.scenarios.CraftPresetManager;
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

            NDropbox<NArea> areaDropdown = new NDropbox<NArea>(
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
                NDropbox<String> presetDropdown = new NDropbox<String>(
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
        if (desc.id.equals("equipment_bot")) {
            hasAnySetting = true;
            add(new Label("Select Preset:"), new Coord(UI.scale(8), y));
            y += UI.scale(24);

            // Load presets from EquipmentPresetManager
            Map<String, EquipmentPreset> presetsMap = NUtils.getUI().core.equipmentPresetManager.getPresets();
            List<EquipmentPreset> presetList = new ArrayList<>(presetsMap.values());
            presetList.sort(Comparator.comparing(EquipmentPreset::getName));

            if (presetList.isEmpty()) {
                add(new Label("No presets available."), new Coord(UI.scale(8), y));
                add(new Label("Create presets in Equipment"), new Coord(UI.scale(8), y + UI.scale(18)));
                add(new Label("Bot settings first."), new Coord(UI.scale(8), y + UI.scale(36)));
                y += UI.scale(60);
            } else {
                String currentId = (String) step.getSetting("presetId");
                EquipmentPreset selectedPreset = null;
                if (currentId != null) {
                    selectedPreset = presetsMap.get(currentId);
                }
                if (selectedPreset == null && !presetList.isEmpty()) {
                    selectedPreset = presetList.get(0);
                }

                final List<EquipmentPreset> finalPresetList = presetList;
                Dropbox<EquipmentPreset> presetDropdown = new Dropbox<EquipmentPreset>(
                        UI.scale(160),
                        Math.min(presetList.size(), 10),
                        UI.scale(22)
                ) {
                    @Override
                    protected EquipmentPreset listitem(int i) { return finalPresetList.get(i); }
                    @Override
                    protected int listitems() { return finalPresetList.size(); }
                    @Override
                    protected void drawitem(GOut g, EquipmentPreset item, int i) {
                        g.text(item.getName(), Coord.z);
                    }
                    @Override
                    public void change(EquipmentPreset item) {
                        super.change(item);
                        if (item != null) {
                            step.setSetting("presetId", item.getId());
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
        if (desc.id.equals("wait_bot")) {
            hasAnySetting = true;
            add(new Label("Wait Duration (hh:mm:ss):"), new Coord(UI.scale(8), y));
            y += UI.scale(24);

            Object currentMs = step.getSetting("waitDurationMs");
            String currentTime = "00:00:10";  // Default 10 seconds
            if (currentMs != null) {
                long ms;
                if (currentMs instanceof Long) {
                    ms = (Long) currentMs;
                } else if (currentMs instanceof Integer) {
                    ms = ((Integer) currentMs).longValue();
                } else if (currentMs instanceof Number) {
                    ms = ((Number) currentMs).longValue();
                } else {
                    ms = 10000;
                }
                currentTime = WaitBot.formatMsToTime(ms);
            }

            TextEntry timeEntry = new TextEntry(UI.scale(100), currentTime) {
                @Override
                protected void changed() {
                    long ms = WaitBot.parseTimeToMs(text());
                    if (ms > 0) {
                        step.setSetting("waitDurationMs", ms);
                    }
                }
            };
            // Set initial value
            long initialMs = WaitBot.parseTimeToMs(currentTime);
            if (initialMs > 0) {
                step.setSetting("waitDurationMs", initialMs);
            }

            add(timeEntry, new Coord(UI.scale(8), y));
            y += UI.scale(30);

            add(new Label("Format: hh:mm:ss, mm:ss, or ss"), new Coord(UI.scale(8), y));
            y += UI.scale(20);
            add(new Label("Examples: 01:30:00, 05:00, 30"), new Coord(UI.scale(8), y));
        }
        if (desc.id.equals("autocraft_bot")) {
            hasAnySetting = true;
            add(new Label("Select Craft Preset:"), new Coord(UI.scale(8), y));
            y += UI.scale(24);

            List<CraftPreset> presetList = CraftPresetManager.getInstance().getPresetList();

            if (presetList.isEmpty()) {
                add(new Label("No presets available."), new Coord(UI.scale(8), y));
                add(new Label("Open a crafting window and"), new Coord(UI.scale(8), y + UI.scale(18)));
                add(new Label("click 'Save Preset' first."), new Coord(UI.scale(8), y + UI.scale(36)));
                y += UI.scale(60);
            } else {
                String currentPresetId = (String) step.getSetting("presetId");
                CraftPreset selectedPreset = null;
                if (currentPresetId != null) {
                    selectedPreset = CraftPresetManager.getInstance().getPreset(currentPresetId);
                }
                if (selectedPreset == null && !presetList.isEmpty()) {
                    selectedPreset = presetList.get(0);
                }

                final List<CraftPreset> finalPresetList = presetList;
                NDropbox<CraftPreset> presetDropdown = new NDropbox<CraftPreset>(
                        UI.scale(160),
                        Math.min(presetList.size(), 10),
                        UI.scale(22)
                ) {
                    @Override
                    protected CraftPreset listitem(int i) { return finalPresetList.get(i); }
                    @Override
                    protected int listitems() { return finalPresetList.size(); }
                    @Override
                    protected void drawitem(GOut g, CraftPreset item, int i) {
                        g.text(item.getName(), Coord.z);
                    }
                    @Override
                    public void change(CraftPreset item) {
                        super.change(item);
                        if (item != null) {
                            step.setSetting("presetId", item.getId());
                        }
                    }
                };
                if (selectedPreset != null) {
                    presetDropdown.change(selectedPreset);
                }

                add(presetDropdown, new Coord(UI.scale(8), y));
                y += UI.scale(40);
            }

            // Quantity input
            add(new Label("Quantity (crafts):"), new Coord(UI.scale(8), y));
            y += UI.scale(24);

            Object currentQty = step.getSetting("quantity");
            int qty = 1;
            if (currentQty != null) {
                if (currentQty instanceof Integer) {
                    qty = (Integer) currentQty;
                } else if (currentQty instanceof Long) {
                    qty = ((Long) currentQty).intValue();
                } else if (currentQty instanceof Number) {
                    qty = ((Number) currentQty).intValue();
                }
            }

            TextEntry qtyEntry = new TextEntry(UI.scale(60), String.valueOf(qty)) {
                @Override
                protected void changed() {
                    try {
                        int q = Integer.parseInt(text().trim());
                        if (q > 0) {
                            step.setSetting("quantity", q);
                        }
                    } catch (NumberFormatException e) {
                        // Ignore invalid input
                    }
                }
            };
            step.setSetting("quantity", qty);

            add(qtyEntry, new Coord(UI.scale(8), y));
            y += UI.scale(30);
        }
        if (desc.id.equals("maintain_stock_bot")) {
            hasAnySetting = true;

            // Preset dropdown
            add(new Label("Select Craft Preset:"), new Coord(UI.scale(8), y));
            y += UI.scale(24);

            List<CraftPreset> presetList = CraftPresetManager.getInstance().getPresetList();

            if (presetList.isEmpty()) {
                add(new Label("No presets available."), new Coord(UI.scale(8), y));
                add(new Label("Save a craft preset first."), new Coord(UI.scale(8), y + UI.scale(18)));
                y += UI.scale(42);
            } else {
                String currentPresetId = (String) step.getSetting("presetId");
                CraftPreset selectedPreset = null;
                if (currentPresetId != null) {
                    selectedPreset = CraftPresetManager.getInstance().getPreset(currentPresetId);
                }
                if (selectedPreset == null && !presetList.isEmpty()) {
                    selectedPreset = presetList.get(0);
                }

                // We'll need to store selected preset to filter areas
                final CraftPreset[] currentPresetHolder = new CraftPreset[] { selectedPreset };
                final Widget[] areaDropdownHolder = new Widget[] { null };
                final int[] areaYHolder = new int[] { 0 };

                final List<CraftPreset> finalPresetList = presetList;
                NDropbox<CraftPreset> presetDropdown = new NDropbox<CraftPreset>(
                        UI.scale(160),
                        Math.min(presetList.size(), 10),
                        UI.scale(22)
                ) {
                    @Override
                    protected CraftPreset listitem(int i) { return finalPresetList.get(i); }
                    @Override
                    protected int listitems() { return finalPresetList.size(); }
                    @Override
                    protected void drawitem(GOut g, CraftPreset item, int i) {
                        g.text(item.getName(), Coord.z);
                    }
                    @Override
                    public void change(CraftPreset item) {
                        super.change(item);
                        if (item != null) {
                            step.setSetting("presetId", item.getId());
                            currentPresetHolder[0] = item;
                            // Rebuild area dropdown when preset changes
                            rebuildMaintainStockAreaDropdown(step, currentPresetHolder[0], areaDropdownHolder, areaYHolder[0]);
                        }
                    }
                };
                if (selectedPreset != null) {
                    presetDropdown.change(selectedPreset);
                }

                add(presetDropdown, new Coord(UI.scale(8), y));
                y += UI.scale(40);

                // Area dropdown (filtered to areas with PUT for output item)
                add(new Label("Select Stock Area:"), new Coord(UI.scale(8), y));
                y += UI.scale(24);
                areaYHolder[0] = y;

                rebuildMaintainStockAreaDropdown(step, currentPresetHolder[0], areaDropdownHolder, y);
                y += UI.scale(40);

                // Target quantity input
                add(new Label("Target Quantity:"), new Coord(UI.scale(8), y));
                y += UI.scale(24);

                Object currentQty = step.getSetting("targetQuantity");
                int qty = 10;
                if (currentQty != null) {
                    if (currentQty instanceof Integer) {
                        qty = (Integer) currentQty;
                    } else if (currentQty instanceof Long) {
                        qty = ((Long) currentQty).intValue();
                    } else if (currentQty instanceof Number) {
                        qty = ((Number) currentQty).intValue();
                    }
                }

                TextEntry qtyEntry = new TextEntry(UI.scale(60), String.valueOf(qty)) {
                    @Override
                    protected void changed() {
                        try {
                            int q = Integer.parseInt(text().trim());
                            if (q > 0) {
                                step.setSetting("targetQuantity", q);
                            }
                        } catch (NumberFormatException e) {
                            // Ignore invalid input
                        }
                    }
                };
                step.setSetting("targetQuantity", qty);

                add(qtyEntry, new Coord(UI.scale(8), y));
                y += UI.scale(30);
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

    /**
     * Rebuilds the area dropdown for maintain_stock_bot, filtered to areas
     * that have the preset's output item as a PUT item.
     */
    private void rebuildMaintainStockAreaDropdown(BotStep step, CraftPreset preset, Widget[] dropdownHolder, int y) {
        // Remove old dropdown if exists
        if (dropdownHolder[0] != null) {
            dropdownHolder[0].destroy();
            dropdownHolder[0] = null;
        }

        // Get output item name from preset
        String outputItemName = null;
        if (preset != null && !preset.getOutputs().isEmpty()) {
            outputItemName = preset.getOutputs().get(0).getName();
        }

        // Get all areas and filter to those with PUT for output item
        List<NArea> filteredAreas = new ArrayList<>();
        if (NUtils.getGameUI() != null && NUtils.getGameUI().map != null &&
            NUtils.getGameUI().map.glob != null && NUtils.getGameUI().map.glob.map != null) {
            for (NArea area : NUtils.getGameUI().map.glob.map.areas.values()) {
                if (area.isVisible() && outputItemName != null && area.containOut(outputItemName)) {
                    filteredAreas.add(area);
                }
            }
        }

        if (filteredAreas.isEmpty()) {
            Label noAreaLabel = new Label("No areas with PUT for this item");
            add(noAreaLabel, new Coord(UI.scale(8), y));
            dropdownHolder[0] = noAreaLabel;
            return;
        }

        // Get currently selected area
        Object currentAreaId = step.getSetting("areaId");
        NArea selectedArea = null;
        if (currentAreaId != null) {
            int areaId;
            if (currentAreaId instanceof Integer) {
                areaId = (Integer) currentAreaId;
            } else if (currentAreaId instanceof Long) {
                areaId = ((Long) currentAreaId).intValue();
            } else {
                areaId = ((Number) currentAreaId).intValue();
            }
            for (NArea area : filteredAreas) {
                if (area.id == areaId) {
                    selectedArea = area;
                    break;
                }
            }
        }
        if (selectedArea == null && !filteredAreas.isEmpty()) {
            selectedArea = filteredAreas.get(0);
        }

        final List<NArea> finalAreaList = filteredAreas;
        NDropbox<NArea> areaDropdown = new NDropbox<NArea>(
                UI.scale(160),
                Math.min(filteredAreas.size(), 10),
                UI.scale(22)
        ) {
            @Override
            protected NArea listitem(int i) { return finalAreaList.get(i); }
            @Override
            protected int listitems() { return finalAreaList.size(); }
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
        dropdownHolder[0] = areaDropdown;
    }
}
