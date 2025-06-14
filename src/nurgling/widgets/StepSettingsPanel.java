package nurgling.widgets;

import haven.*;
import nurgling.NUtils;
import nurgling.areas.NArea;
import nurgling.actions.bots.registry.BotDescriptor;
import nurgling.actions.bots.registry.BotRegistry;
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
