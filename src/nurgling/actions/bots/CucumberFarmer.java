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

public class CucumberFarmer implements Action {
    private static final NAlias PLANT_ALIAS = new NAlias("plants/cucumber");
    private static final NAlias SEED_ALIAS = new NAlias("Seeds of Cucumber");
    private static final NAlias CUCUMBER_ALIAS = new NAlias("Cucumber");

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        boolean oldStackingValue = ((NInventory) NUtils.getGameUI().maininv).bundle.a;

        // Crop area with "Cucumber" sub-specialization
        NArea.Specialisation crop = new NArea.Specialisation(
            Specialisation.SpecName.crop.toString(),
            "Cucumber"
        );

        // Seed area with "seed" specialization (barrel storage, same as field crops)
        NArea.Specialisation seedSpec = new NArea.Specialisation(
            Specialisation.SpecName.seed.toString(),
            "Cucumber"
        );

        // PUT area for cucumbers (stockpile storage)
        NArea cucumberPutArea = NContext.findOut(CUCUMBER_ALIAS, 1);

        if (cucumberPutArea == null) {
            return Results.ERROR("PUT Area for 'Cucumber' required, but not found!");
        }

        // Validate crop area and seed area exist
        ArrayList<NArea.Specialisation> req = new ArrayList<>();
        req.add(crop);
        req.add(seedSpec);

        if (new Validator(req, new ArrayList<>()).run(gui).IsSuccess()) {
            NUtils.stackSwitch(true);

            NArea cropArea = NContext.findSpec(crop);
            NArea seedArea = NContext.findSpec(seedSpec);

            // ===== PHASE 1: HARVEST ALL MATURE CUCUMBERS =====
            // HarvestTrellis handles both results with priority ordering:
            // 1. Seeds → Barrel in seed area (priority 1, first in CropRegistry)
            // 2. Cucumbers → Stockpile in PUT area (priority 2, second in CropRegistry)
            List<HarvestResultConfig> results = Arrays.asList(
                new HarvestResultConfig(
                    SEED_ALIAS,
                    seedArea,  // Specialization area with barrels
                    1,
                    CropRegistry.StorageBehavior.BARREL
                ),
                new HarvestResultConfig(
                    CUCUMBER_ALIAS,
                    cucumberPutArea,  // PUT area with stockpiles
                    2,
                    CropRegistry.StorageBehavior.STOCKPILE
                )
            );

            new HarvestTrellis(cropArea, PLANT_ALIAS, results).run(gui);

            NUtils.stackSwitch(false);
            // ===== PHASE 2: REPLANT ALL EMPTY TRELLIS =====
            // PlantTrellis fetches seeds from seedArea barrels and replants
            // Then returns remaining seeds to seedArea barrels
            new PlantTrellis(cropArea, PLANT_ALIAS, SEED_ALIAS, seedArea).run(gui);

            NUtils.stackSwitch(oldStackingValue);
            return Results.SUCCESS();
        }

        NUtils.stackSwitch(oldStackingValue);
        return Results.FAIL();
    }
}
