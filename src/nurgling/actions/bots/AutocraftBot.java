package nurgling.actions.bots;

import haven.*;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.actions.Action;
import nurgling.actions.Results;
import nurgling.scenarios.CraftPreset;
import nurgling.scenarios.CraftPresetManager;
import nurgling.areas.NContext;
import nurgling.tasks.NTask;
import nurgling.tools.VSpec;
import nurgling.widgets.NMakewindow;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Map;

/**
 * AutocraftBot executes crafting based on a saved preset.
 * Used in scenarios to automate crafting with pre-configured recipes.
 */
public class AutocraftBot implements Action {
    private String presetId;
    private int quantity = 1;

    public AutocraftBot() {
    }

    public AutocraftBot(Map<String, Object> settings) {
        if (settings != null) {
            if (settings.containsKey("presetId")) {
                this.presetId = (String) settings.get("presetId");
            }
            if (settings.containsKey("quantity")) {
                Object q = settings.get("quantity");
                if (q instanceof Integer) {
                    this.quantity = (Integer) q;
                } else if (q instanceof Long) {
                    this.quantity = ((Long) q).intValue();
                } else if (q instanceof Number) {
                    this.quantity = ((Number) q).intValue();
                }
            }
        }
    }

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        if (presetId == null || presetId.isEmpty()) {
            return Results.ERROR("No craft preset selected");
        }

        CraftPreset preset = CraftPresetManager.getInstance().getPreset(presetId);
        if (preset == null) {
            return Results.ERROR("Craft preset not found: " + presetId);
        }

        gui.msg("Starting autocraft: " + preset.getName() + " x" + quantity);

        // Get the recipe resource path and activate it via menu
        String recipeResource = preset.getRecipeResource();
        if (recipeResource == null || recipeResource.isEmpty()) {
            return Results.ERROR("No recipe resource in preset");
        }

        // Check if craft window is already open with the correct recipe
        boolean needToOpenRecipe = true;
        if (gui.craftwnd != null && gui.craftwnd.makeWidget != null) {
            NMakewindow existingMwnd = gui.craftwnd.makeWidget;
            if (existingMwnd.recipeResource != null && existingMwnd.recipeResource.equals(recipeResource)) {
                needToOpenRecipe = false;
            }
        }

        if (needToOpenRecipe) {
            // Load the resource and find the pagina
            Indir<Resource> res = Resource.remote().load(recipeResource);
            MenuGrid.Pagina pag = gui.menu.paginafor(res);

            if (pag == null) {
                return Results.ERROR("Could not find recipe in menu: " + recipeResource);
            }

            // Activate the recipe via menu
            gui.menu.use(pag.button(), new MenuGrid.Interaction(), false);

            // Wait for the crafting window to appear with makeWidget initialized
            NUtils.addTask(new NTask() {
                @Override
                public boolean check() {
                    return gui.craftwnd != null && gui.craftwnd.makeWidget != null;
                }
            });
        }

        NMakewindow mwnd = gui.craftwnd.makeWidget;
        if (mwnd == null) {
            return Results.ERROR("Crafting window did not open");
        }

        // Enable auto mode FIRST - this is needed for tick() to populate categories
        mwnd.autoMode = true;
        if (mwnd.noTransfer != null) {
            mwnd.noTransfer.visible = true;
        }

        // Wait for inputs to be fully populated (names loaded and categories checked)
        // In headless mode, skip sprite check since sprites won't render
        final boolean isHeadless = nurgling.headless.Headless.isHeadless();
        NUtils.addTask(new NTask() {
            @Override
            public boolean check() {
                if (mwnd.inputs == null || mwnd.inputs.isEmpty()) {
                    return false;
                }
                // Ensure all specs have their names loaded (skip sprite check in headless mode)
                for (NMakewindow.Spec spec : mwnd.inputs) {
                    if (spec.name == null) {
                        return false;
                    }
                    if (!isHeadless && spec.spr == null) {
                        return false;
                    }
                }
                return true;
            }
        });

        // Verify recipe name matches
        if (preset.getRecipeName() != null && !preset.getRecipeName().isEmpty()) {
            if (!preset.getRecipeName().equals(mwnd.rcpnm)) {
                gui.msg("Warning: Recipe name mismatch. Expected: " + preset.getRecipeName() + ", Got: " + mwnd.rcpnm);
            }
        }

        // Configure ingredients from preset
        configureIngredients(mwnd, preset);

        // Run the craft
        Craft craft = new Craft(mwnd, quantity);
        Results result = craft.run(gui);

        gui.msg("Autocraft completed: " + preset.getName());
        return result;
    }

    /**
     * Configures the NMakewindow ingredients based on preset preferences.
     */
    private void configureIngredients(NMakewindow mwnd, CraftPreset preset) {
        // Match preset inputs to mwnd inputs and set ingredient preferences
        for (CraftPreset.InputSpec presetInput : preset.getInputs()) {
            if (!presetInput.isCategory()) {
                continue; // Only categories need configuration
            }

            // Find matching spec in mwnd.inputs
            for (NMakewindow.Spec spec : mwnd.inputs) {
                if (spec.name != null && spec.name.equals(presetInput.getName())) {
                    if (presetInput.isIgnored()) {
                        // Mark as ignored
                        spec.ing = mwnd.new Ingredient(
                            Resource.loadsimg("nurgling/hud/autocraft/ignore"),
                            "Ignore ingredient",
                            true
                        );
                    } else if (presetInput.getPreferredIngredient() != null) {
                        // Set preferred ingredient
                        setPreferredIngredient(mwnd, spec, presetInput.getPreferredIngredient());
                    }
                    break;
                }
            }
        }
    }

    /**
     * Sets the preferred ingredient for a category spec.
     * Only sets the ingredient if an area is defined for it, otherwise leaves it
     * for Craft.java to auto-select an ingredient that has an area.
     */
    private void setPreferredIngredient(NMakewindow mwnd, NMakewindow.Spec spec, String preferredName) {
        ArrayList<JSONObject> categoryItems = VSpec.categories.get(spec.name);
        if (categoryItems == null) {
            return;
        }

        for (JSONObject obj : categoryItems) {
            if (obj == null) {
                continue;
            }
            if (!obj.has("name")) {
                continue;
            }
            String itemName = obj.getString("name");
            if (itemName != null && itemName.equals(preferredName)) {
                // Only set the ingredient if an area exists for it
                if (NContext.findIn(itemName) != null || NContext.findInGlobal(itemName) != null) {
                    spec.ing = mwnd.new Ingredient(obj);
                }
                // If no area, leave spec.ing null so Craft.java can auto-select
                return;
            }
        }
    }
}
