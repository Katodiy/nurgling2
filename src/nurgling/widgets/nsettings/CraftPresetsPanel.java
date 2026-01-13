package nurgling.widgets.nsettings;

import haven.*;
import nurgling.scenarios.CraftPreset;
import nurgling.scenarios.CraftPresetManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Panel for managing craft presets in the settings window.
 * Allows viewing and deleting saved craft presets.
 */
public class CraftPresetsPanel extends Panel {
    private final int margin = UI.scale(10);
    private Scrollport presetListContainer;
    private Widget presetListContent;
    private final Set<String> expandedPresets = new HashSet<>();

    public CraftPresetsPanel() {
        super("");

        int contentWidth = sz.x - margin * 2;
        int listHeight = UI.scale(420);

        add(new Label("Craft Presets:"), new Coord(margin, margin));
        add(new Label("Save presets from crafting windows using the 'Save' button."),
            new Coord(margin, margin + UI.scale(18)));

        int listWidth = contentWidth - margin * 2;

        // Create scrollable container
        presetListContainer = add(new Scrollport(new Coord(listWidth, listHeight)),
            new Coord(margin, margin + UI.scale(45)));
        presetListContent = new Widget(new Coord(listWidth, UI.scale(50))) {
            @Override
            public void pack() {
                resize(contentsz());
            }
        };
        presetListContainer.cont.add(presetListContent, Coord.z);

        rebuildPresetList();
    }

    private void rebuildPresetList() {
        // Clear existing widgets
        for (Widget child : new ArrayList<>(presetListContent.children())) {
            child.destroy();
        }

        List<CraftPreset> presets = CraftPresetManager.getInstance().getPresetList();

        int y = 0;
        int contentWidth = presetListContainer.cont.sz.x;

        for (CraftPreset preset : presets) {
            Widget presetWidget = createPresetWidget(preset, new Coord(contentWidth, UI.scale(32)));
            presetListContent.add(presetWidget, new Coord(0, y));
            y += presetWidget.sz.y + UI.scale(2);
        }

        if (presets.isEmpty()) {
            presetListContent.add(new Label("No presets saved yet."), new Coord(margin, UI.scale(10)));
        }

        presetListContent.pack();
        presetListContainer.cont.update();
    }

    private Widget createPresetWidget(CraftPreset preset, Coord sz) {
        boolean isExpanded = expandedPresets.contains(preset.getId());

        // Calculate height
        int baseHeight = UI.scale(32);
        int expandedHeight = UI.scale(50);
        int totalHeight = isExpanded ? baseHeight + expandedHeight : baseHeight;

        Widget w = new Widget(new Coord(sz.x, totalHeight));

        // Column positions
        int expandBtnX = margin;
        int expandBtnW = UI.scale(30);
        int nameX = expandBtnX + expandBtnW + UI.scale(8);
        int btnW = UI.scale(60);
        int delBtnX = sz.x - margin - btnW;

        // Main row background
        w.add(new Widget(new Coord(sz.x - margin * 2, baseHeight)) {
            public void draw(GOut g) {
                g.chcolor(255, 255, 255, 12);
                g.frect(Coord.z, sz);
                g.chcolor();
            }
        }, new Coord(margin, 0));

        // Expand/collapse button
        String expandBtnText = isExpanded ? "▼" : "▶";
        Button expandBtn = new Button(expandBtnW, expandBtnText, () -> {
            if (expandedPresets.contains(preset.getId())) {
                expandedPresets.remove(preset.getId());
            } else {
                expandedPresets.add(preset.getId());
            }
            rebuildPresetList();
        });
        w.add(expandBtn, new Coord(expandBtnX, (baseHeight - expandBtn.sz.y) / 2));

        // Preset name
        Label nameLabel = new Label(preset.getName());
        w.add(nameLabel, new Coord(nameX, (baseHeight - nameLabel.sz.y) / 2));

        // Recipe name (shorter, to the right of preset name)
        String recipeName = preset.getRecipeName() != null ? "(" + preset.getRecipeName() + ")" : "";
        if (!recipeName.isEmpty()) {
            Label recipeLabel = new Label(recipeName);
            w.add(recipeLabel, new Coord(nameX + nameLabel.sz.x + UI.scale(10), (baseHeight - recipeLabel.sz.y) / 2));
        }

        // Delete button
        w.add(new Button(btnW, "Delete", () -> {
            CraftPresetManager.getInstance().deletePreset(preset.getId());
            expandedPresets.remove(preset.getId());
            rebuildPresetList();
        }), new Coord(delBtnX, (baseHeight - UI.scale(24)) / 2));

        // Expanded details
        if (isExpanded) {
            int detailY = baseHeight + UI.scale(5);

            // Inputs
            StringBuilder inputs = new StringBuilder("Inputs: ");
            for (CraftPreset.InputSpec input : preset.getInputs()) {
                if (inputs.length() > 8) inputs.append(", ");
                inputs.append(input.getName());
                if (input.getCount() > 1) inputs.append(" x").append(input.getCount());
            }
            String inputsStr = inputs.length() > 70 ? inputs.substring(0, 67) + "..." : inputs.toString();
            w.add(new Label(inputsStr), new Coord(nameX, detailY));

            // Outputs
            StringBuilder outputs = new StringBuilder("Outputs: ");
            for (CraftPreset.OutputSpec output : preset.getOutputs()) {
                if (outputs.length() > 9) outputs.append(", ");
                outputs.append(output.getName());
                if (output.getCount() > 1) outputs.append(" x").append(output.getCount());
            }
            String outputsStr = outputs.length() > 70 ? outputs.substring(0, 67) + "..." : outputs.toString();
            w.add(new Label(outputsStr), new Coord(nameX, detailY + UI.scale(18)));
        }

        return w;
    }

    @Override
    public void show() {
        CraftPresetManager.getInstance().loadPresets();
        rebuildPresetList();
        super.show();
    }

    @Override
    public void load() {
        CraftPresetManager.getInstance().loadPresets();
        rebuildPresetList();
    }
}
