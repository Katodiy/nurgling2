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

public class WildGourdFarmer implements Action {
    private static final NAlias PLANT_ALIAS = new NAlias("plants/gourd");
    private static final NAlias SEED_ALIAS = new NAlias("Wild Gourd", "pre-gourd", "pregourd");
    private static final NAlias WILDGOURD_ALIAS = new NAlias("Wild Gourd");

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        boolean oldStackingValue = ((NInventory) NUtils.getGameUI().maininv).bundle.a;

        NArea.Specialisation crop = new NArea.Specialisation(
            Specialisation.SpecName.crop.toString(),
            "Wild Gourd"
        );

        NArea wildGourdPutArea = NContext.findOut("Wild Gourd", 1);

        if (wildGourdPutArea == null) {
            return Results.ERROR("PUT Area for Wild Gourd required, but not found!");
        }

        ArrayList<NArea.Specialisation> req = new ArrayList<>();
        req.add(crop);

        if (new Validator(req, new ArrayList<>()).run(gui).IsSuccess()) {
            NUtils.stackSwitch(true);

            NArea cropArea = NContext.findSpec(crop);

            List<HarvestResultConfig> results = Arrays.asList(
                new HarvestResultConfig(
                    SEED_ALIAS,
                    wildGourdPutArea,
                    1,
                    CropRegistry.StorageBehavior.STOCKPILE
                )
            );

            new HarvestTrellis(cropArea, PLANT_ALIAS, results).run(gui);

            NUtils.stackSwitch(false);
            new PlantTrellis(cropArea, PLANT_ALIAS, SEED_ALIAS, wildGourdPutArea).run(gui);

            NUtils.stackSwitch(oldStackingValue);
            return Results.SUCCESS();
        }

        NUtils.stackSwitch(oldStackingValue);
        return Results.FAIL();
    }
}
