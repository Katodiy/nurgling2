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

public class PeaFarmer implements Action {
    private static final NAlias PLANT_ALIAS = new NAlias("plants/pea");
    private static final NAlias SEED_ALIAS = new NAlias("Peapod");

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        boolean oldStackingValue = ((NInventory) NUtils.getGameUI().maininv).bundle.a;

        // Crop area with "Pea" sub-specialization
        NArea.Specialisation crop = new NArea.Specialisation(
            Specialisation.SpecName.crop.toString(),
            "Pea"
        );

        // PUT area for peapods (both harvest output AND replanting source)
        NArea peapodPutArea = NContext.findOut("Peapod", 1);

        if (peapodPutArea == null) {
            return Results.ERROR("PUT Area for Peapod required, but not found!");
        }

        // Validate crop area exists
        ArrayList<NArea.Specialisation> req = new ArrayList<>();
        req.add(crop);

        if (new Validator(req, new ArrayList<>()).run(gui).IsSuccess()) {
            NUtils.stackSwitch(true);

            NArea cropArea = NContext.findSpec(crop);

            // ===== PHASE 1: HARVEST ALL MATURE PEAS =====
            List<HarvestResultConfig> results = Arrays.asList(
                new HarvestResultConfig(
                    SEED_ALIAS,
                    peapodPutArea,
                    1,
                    CropRegistry.StorageBehavior.STOCKPILE
                )
            );

            new HarvestTrellis(cropArea, PLANT_ALIAS, results).run(gui);

            NUtils.stackSwitch(false);
            // ===== PHASE 2: REPLANT ALL EMPTY TRELLIS =====
            new PlantTrellis(cropArea, PLANT_ALIAS, SEED_ALIAS, peapodPutArea).run(gui);

            NUtils.stackSwitch(oldStackingValue);
            return Results.SUCCESS();
        }

        NUtils.stackSwitch(oldStackingValue);
        return Results.FAIL();
    }
}
