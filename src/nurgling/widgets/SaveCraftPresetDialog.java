package nurgling.widgets;

import haven.*;
import nurgling.NUtils;
import nurgling.i18n.L10n;
import nurgling.scenarios.CraftPreset;
import nurgling.scenarios.CraftPresetManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Dialog for saving the current crafting configuration as a preset.
 * Captures recipe info, inputs, outputs, and ingredient preferences.
 */
public class SaveCraftPresetDialog extends Window {

    private final NMakewindow mwnd;
    private TextEntry nameEntry;
    private Label statusLabel;

    public SaveCraftPresetDialog(NMakewindow mwnd) {
        super(UI.scale(new Coord(300, 250)), L10n.get("craftpreset.save_title"));
        this.mwnd = mwnd;
        createUI();
    }

    private void createUI() {
        int y = 10;

        // Recipe name display
        add(new Label(L10n.get("craftpreset.recipe") + " " + (mwnd.rcpnm != null ? mwnd.rcpnm : L10n.get("craftpreset.unknown"))), UI.scale(10, y));
        y += 25;

        // Preset name input
        add(new Label(L10n.get("craftpreset.preset_name")), UI.scale(10, y));
        y += 18;

        String defaultName = mwnd.rcpnm != null ? mwnd.rcpnm : "New Preset";
        nameEntry = add(new TextEntry(UI.scale(270), defaultName), UI.scale(10, y));
        y += 35;

        // Inputs summary
        add(new Label(L10n.get("craftpreset.inputs")), UI.scale(10, y));
        y += 18;

        StringBuilder inputsSummary = new StringBuilder();
        for (NMakewindow.Spec spec : mwnd.inputs) {
            String itemName = getSpecDisplayName(spec);
            if (inputsSummary.length() > 0) inputsSummary.append(", ");
            inputsSummary.append(itemName);
            if (spec.count > 1) inputsSummary.append(" x").append(spec.count);
        }
        String inputsText = inputsSummary.length() > 40
            ? inputsSummary.substring(0, 37) + "..."
            : inputsSummary.toString();
        add(new Label(inputsText.isEmpty() ? L10n.get("craftpreset.none") : inputsText), UI.scale(20, y));
        y += 20;

        // Outputs summary
        add(new Label(L10n.get("craftpreset.outputs")), UI.scale(10, y));
        y += 18;

        StringBuilder outputsSummary = new StringBuilder();
        for (NMakewindow.Spec spec : mwnd.outputs) {
            String itemName = getSpecDisplayName(spec);
            if (outputsSummary.length() > 0) outputsSummary.append(", ");
            outputsSummary.append(itemName);
            if (spec.count > 1) outputsSummary.append(" x").append(spec.count);
        }
        String outputsText = outputsSummary.length() > 40
            ? outputsSummary.substring(0, 37) + "..."
            : outputsSummary.toString();
        add(new Label(outputsText.isEmpty() ? L10n.get("craftpreset.none") : outputsText), UI.scale(20, y));
        y += 25;

        // Status label for feedback
        statusLabel = add(new Label(""), UI.scale(10, y));
        y += 25;

        // Buttons
        add(new Button(UI.scale(80), L10n.get("common.cancel")) {
            @Override
            public void click() {
                close();
            }
        }, UI.scale(50, y));

        add(new Button(UI.scale(80), L10n.get("common.save")) {
            @Override
            public void click() {
                savePreset();
            }
        }, UI.scale(160, y));
    }

    public void close() {
        ui.destroy(this);
    }

    @Override
    public void wdgmsg(Widget sender, String msg, Object... args) {
        if (msg.equals("close")) {
            close();
        } else {
            super.wdgmsg(sender, msg, args);
        }
    }

    private String getSpecDisplayName(NMakewindow.Spec spec) {
        if (spec.ing != null) {
            return spec.ing.name;
        }
        return spec.name != null ? spec.name : "Unknown";
    }

    private void savePreset() {
        String presetName = nameEntry.text().trim();
        if (presetName.isEmpty()) {
            statusLabel.settext(L10n.get("craftpreset.enter_name"));
            return;
        }

        try {
            CraftPreset preset = createPresetFromWindow();
            preset.setName(presetName);

            CraftPresetManager.getInstance().addOrUpdatePreset(preset);

            NUtils.getGameUI().msg(L10n.get("craftpreset.saved").replace("%s", presetName));
            close();

        } catch (Exception e) {
            statusLabel.settext("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private CraftPreset createPresetFromWindow() {
        CraftPreset preset = new CraftPreset();

        // Set recipe info
        preset.setRecipeName(mwnd.rcpnm);

        // Get recipe resource from NMakewindow (captured at creation time)
        if (mwnd.recipeResource != null) {
            preset.setRecipeResource(mwnd.recipeResource);
        }

        // Capture inputs
        List<CraftPreset.InputSpec> inputs = new ArrayList<>();
        for (NMakewindow.Spec spec : mwnd.inputs) {
            CraftPreset.InputSpec inputSpec = new CraftPreset.InputSpec();
            inputSpec.setName(spec.name);
            inputSpec.setCount(spec.count);
            inputSpec.setCategory(spec.categories);
            inputSpec.setOptional(spec.opt());

            // Capture resource path if available
            try {
                if (spec.res != null && spec.res.get() != null) {
                    inputSpec.setResourcePath(spec.res.get().name);
                }
            } catch (Loading l) {
                // Resource not loaded
            }

            // Capture ingredient preference for categories
            if (spec.ing != null) {
                inputSpec.setPreferredIngredient(spec.ing.name);
                inputSpec.setIgnored(spec.ing.isIgnored);
            }

            inputs.add(inputSpec);
        }
        preset.setInputs(inputs);

        // Capture outputs
        List<CraftPreset.OutputSpec> outputs = new ArrayList<>();
        for (NMakewindow.Spec spec : mwnd.outputs) {
            CraftPreset.OutputSpec outputSpec = new CraftPreset.OutputSpec();
            outputSpec.setName(spec.name);
            outputSpec.setCount(spec.count);

            // Capture resource path and item size
            // The crafting window uses "small" preview icons, so we need to load the real item resource
            try {
                if (spec.res != null && spec.res.get() != null) {
                    String resName = spec.res.get().name;

                    // Strip "/small/" from the path to get the actual item resource
                    String actualResName = resName.replace("/small/", "/");
                    outputSpec.setResourcePath(actualResName);

                    Resource actualRes = Resource.remote().loadwait(actualResName);
                    Resource.Image img = actualRes.layer(Resource.imgc);

                    if (img != null && img.ssz != null) {
                        Coord imgSz = img.ssz;
                        // Convert pixel size to inventory squares
                        int width = Math.max(1, (imgSz.x + Inventory.sqsz.x - 1) / Inventory.sqsz.x);
                        int height = Math.max(1, (imgSz.y + Inventory.sqsz.y - 1) / Inventory.sqsz.y);
                        outputSpec.setWidth(width);
                        outputSpec.setHeight(height);
                    }
                }
            } catch (Loading | Resource.LoadException l) {
                // Resource not loaded or doesn't exist, use default 1x1
            }

            outputs.add(outputSpec);
        }
        preset.setOutputs(outputs);

        return preset;
    }
}
