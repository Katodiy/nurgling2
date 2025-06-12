package nurgling.widgets;

import haven.*;
import haven.Button;
import haven.Label;
import haven.Window;
import nurgling.actions.bots.registry.BotDescriptor;
import nurgling.actions.bots.registry.BotRegistry;
import nurgling.actions.bots.registry.Setting;
import nurgling.scenarios.BotStep;
import nurgling.areas.NArea;
import nurgling.NUtils;

import java.awt.*;
import java.util.*;
import java.util.List;

public class ScenarioStepSettingsWindow extends Window {
    private final BotStep step;
    private final BotDescriptor descriptor;
    private final Map<String, Object> editedSettings = new HashMap<>();
    private AreaList areaListWidget;
    private NArea selectedArea = null;

    public ScenarioStepSettingsWindow(BotStep step) {
        super(new Coord(350, 420), "Step Settings");
        this.step = step;
        this.descriptor = BotRegistry.getDescriptor(step.getBotKey());

        int y = UI.scale(20);

        // Only handle areaId for now (extend as needed)
        for (Setting setting : descriptor.factory.requiredSettings()) {
            String key = setting.name;
            Class<?> type = setting.type;

            if (key.equals("areaId") && type == Integer.class) {
                add(new Label("Select Area:"), new Coord(UI.scale(20), y));
                y += UI.scale(28);

                // Gather and sort areas
                List<NArea> areaList = new ArrayList<>(NUtils.getGameUI().map.glob.map.areas.values());
                areaList.sort(Comparator.comparing(a -> a.name));

                Integer currentAreaId = (Integer) step.getSetting("areaId");
                if (currentAreaId != null) {
                    for (NArea a : areaList)
                        if (a.id == currentAreaId)
                            selectedArea = a;
                }
                if (selectedArea == null && !areaList.isEmpty())
                    selectedArea = areaList.get(0);

                areaListWidget = new AreaList(new Coord(260, 260), areaList);
                add(areaListWidget, new Coord(UI.scale(30), y));
                y += areaListWidget.sz.y + UI.scale(10);
            }
        }

        add(new Button(UI.scale(100), "Save", this::saveSettings), new Coord(UI.scale(40), y));
        add(new Button(UI.scale(100), "Cancel", this::hide), new Coord(UI.scale(160), y));
        pack();
    }

    private void saveSettings() {
        // Only areaId for now
        if (selectedArea != null)
            editedSettings.put("areaId", selectedArea.id);
        step.setSettings(editedSettings);
        hide();
    }

    // ----------- AreaList (SListBox) ----------
    private class AreaList extends SListBox<NArea, Widget> {
        private final List<NArea> areas;

        public AreaList(Coord sz, List<NArea> areas) {
            super(sz, UI.scale(24));
            this.areas = areas;
            // Set initial selection
            if (selectedArea != null)
                change(selectedArea);
            else if (!areas.isEmpty())
                change(areas.get(0));
        }

        @Override
        protected List<NArea> items() {
            return areas;
        }

        @Override
        protected Widget makeitem(NArea area, int idx, Coord sz) {
            return new ItemWidget<NArea>(this, sz, area) {{
                add(new Label(area.name + " [ID " + area.id + "]"));
            }
                // Selection logic
                @Override
                public boolean mousedown(MouseDownEvent ev) {
                    if (ev.b == 1) {
                        areaListWidget.change(area); // select
                        return true;
                    }
                    return super.mousedown(ev);
                }
                // Optional: handle right-click here if you want a menu
            };
        }

        Color bg = new Color(30, 40, 40, 160);

        @Override
        public void draw(GOut g) {
            g.chcolor(bg);
            g.frect(Coord.z, g.sz());
            super.draw(g);
        }

        @Override
        public void change(NArea area) {
            super.change(area);
            selectedArea = area;
        }
    }
}
