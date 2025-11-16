package nurgling.actions.bots;

import nurgling.NGameUI;
import nurgling.NInventory;
import nurgling.NUtils;
import nurgling.actions.Action;
import nurgling.actions.HarvestResultConfig;
import nurgling.actions.HarvestTrellis;
import nurgling.actions.Results;
import nurgling.actions.Validator;
import nurgling.areas.NArea;
import nurgling.areas.NContext;
import nurgling.conf.CropRegistry;
import nurgling.tools.NAlias;
import nurgling.widgets.Specialisation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PeppercornFarmer implements Action {
    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        boolean oldStackingValue = ((NInventory) NUtils.getGameUI().maininv).bundle.a;

        // Define required crop area with "Peppercorn" sub-specialization
        NArea.Specialisation crop = new NArea.Specialisation(
            Specialisation.SpecName.crop.toString(),
            "Peppercorn"
        );

        // Find PUT area for peppercorn storage (contains containers)
        NArea peppercornPutArea = NContext.findOut("Peppercorn", 1);

        if (peppercornPutArea == null) {
            return Results.ERROR("PUT Area for Peppercorn required, but not found!");
        }

        // Validate crop area exists
        ArrayList<NArea.Specialisation> req = new ArrayList<>();
        req.add(crop);

        if (new Validator(req, new ArrayList<>()).run(gui).IsSuccess()) {
            NUtils.stackSwitch(true);

            // Create single-item CONTAINER result config
            List<HarvestResultConfig> results = Arrays.asList(
                new HarvestResultConfig(
                    new NAlias("Peppercorn"),
                    peppercornPutArea,
                    1,  // Priority (only one item)
                    CropRegistry.StorageBehavior.CONTAINER
                )
            );

            // Harvest trellis crops using modified HarvestTrellis
            new HarvestTrellis(
                NContext.findSpec(crop),
                new NAlias("plants/pepper"),
                results
            ).run(gui);

            NUtils.stackSwitch(oldStackingValue);
            return Results.SUCCESS();
        }

        NUtils.stackSwitch(oldStackingValue);
        return Results.FAIL();
    }
}
