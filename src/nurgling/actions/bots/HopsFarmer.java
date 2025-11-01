package nurgling.actions.bots;

import nurgling.NGameUI;
import nurgling.NInventory;
import nurgling.NUtils;
import nurgling.actions.*;
import nurgling.areas.NArea;
import nurgling.areas.NContext;
import nurgling.conf.CropRegistry;
import nurgling.tools.NAlias;
import nurgling.widgets.Specialisation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class HopsFarmer implements Action {
    private static final NAlias PLANT_ALIAS = new NAlias("plants/hops");
    private static final NAlias SEED_ALIAS = new NAlias("cones");

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        boolean oldStackingValue = ((NInventory) NUtils.getGameUI().maininv).bundle.a;

        // Define required crop area with "Hops" sub-specialization
        NArea.Specialisation crop = new NArea.Specialisation(
            Specialisation.SpecName.crop.toString(),
            "Hops"
        );

        // Find PUT areas for both hop cone types
        NArea largeConeArea = NContext.findOut("Unusually Large Hop Cone", 1);
        NArea regularConeArea = NContext.findOut("Hop Cones", 1);

        if (largeConeArea == null) {
            return Results.ERROR("PUT Area for 'Unusually Large Hop Cone' required, but not found!");
        }

        if (regularConeArea == null) {
            return Results.ERROR("PUT Area for 'Hop Cones' required, but not found!");
        }

        // Validate crop area exists
        ArrayList<NArea.Specialisation> req = new ArrayList<>();
        req.add(crop);

        if (new Validator(req, new ArrayList<>()).run(gui).IsSuccess()) {
            NUtils.stackSwitch(true);

            NArea cropArea = NContext.findSpec(crop);

            // Create multi-result harvest configs with priority
            List<HarvestResultConfig> results = Arrays.asList(
                new HarvestResultConfig(
                    new NAlias("Unusually Large Hop Cone"),
                    largeConeArea,
                    1,  // Priority 1 - drop first (rare item)
                    CropRegistry.StorageBehavior.STOCKPILE
                ),
                new HarvestResultConfig(
                    new NAlias("Hop Cones"),
                    regularConeArea,
                    2,  // Priority 2 - drop second (common item)
                    CropRegistry.StorageBehavior.STOCKPILE
                )
            );

            // Harvest trellis crops using modified HarvestTrellis
            new HarvestTrellis(
                NContext.findSpec(crop),
                new NAlias("plants/hops"),
                results
            ).run(gui);

            NUtils.stackSwitch(false);
            // ===== PHASE 2: REPLANT ALL EMPTY TRELLIS =====
            new PlantTrellis(cropArea, PLANT_ALIAS, SEED_ALIAS, regularConeArea).run(gui);

            NUtils.stackSwitch(oldStackingValue);
            return Results.SUCCESS();
        }

        NUtils.stackSwitch(oldStackingValue);
        return Results.FAIL();
    }
}
