package nurgling.actions.bots;

import nurgling.*;
import nurgling.actions.*;
import nurgling.areas.NArea;
import nurgling.areas.NContext;
import nurgling.widgets.Specialisation;

import java.util.ArrayList;

/**
 * Orchestrator bot that manages the full garden pot farming cycle.
 * For each area with "Planting Garden Pots" specialization:
 * 1. Harvest ready plants (GardenPotHarvester)
 * 2. Fill pots with soil and water (GardenPotFiller)
 * 3. Plant new items (GardenPotPlanter)
 */
public class GardenPotFarmer implements Action {

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        NContext context = new NContext(gui);

        // Find all areas with "Planting Garden Pots" specialization
        ArrayList<NArea> potAreas = NContext.findAllSpec(
            Specialisation.SpecName.plantingGardenPots.toString()
        );

        if (potAreas.isEmpty()) {
            return Results.ERROR("No Planting Garden Pots areas found. Please configure the specialization.");
        }

        gui.msg("Found " + potAreas.size() + " garden pot area(s)");

        // Process each area
        for (NArea area : potAreas) {
            String plantType = getAreaSubtype(area);
            gui.msg("Processing area: " + area.name + " (plant type: " + (plantType != null ? plantType : "none") + ")");

            Results result = processArea(gui, context, area, plantType);
            if (!result.IsSuccess()) {
                gui.msg("Warning: Failed to process area " + area.name);
                // Continue to next area
            }
        }

        gui.msg("Garden pot farming complete!");
        return Results.SUCCESS();
    }

    /**
     * Get the subspecialization (plant type) for an area.
     */
    private String getAreaSubtype(NArea area) {
        for (NArea.Specialisation spec : area.spec) {
            if (spec.name.equals(Specialisation.SpecName.plantingGardenPots.toString())) {
                return spec.subtype;
            }
        }
        return null;
    }

    /**
     * Process a single garden pot area through all phases.
     */
    private Results processArea(NGameUI gui, NContext context, NArea area, String plantType)
            throws InterruptedException {

        // Step 1: Harvest ready plants (pots with 2 Equed overlays)
        Results harvestResult = new GardenPotHarvester(area, context).run(gui);
        if (!harvestResult.IsSuccess()) {
            gui.msg("Warning: Harvest phase had issues");
            // Continue anyway
        }

        // Step 2: Fill pots with soil and water
        Results fillResult = new GardenPotFiller(area, context).run(gui);
        if (!fillResult.IsSuccess()) {
            gui.msg("Warning: Fill phase had issues");
            // Continue anyway, some pots may be ready
        }

        // Step 3: Plant in pots that are ready (marker = 3, no plant)
        Results plantResult = new GardenPotPlanter(area, context, plantType).run(gui);
        if (!plantResult.IsSuccess()) {
            gui.msg("Warning: Plant phase had issues");
        }

        return Results.SUCCESS();
    }
}
