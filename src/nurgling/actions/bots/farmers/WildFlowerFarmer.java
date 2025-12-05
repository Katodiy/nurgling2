package nurgling.actions.bots.farmers;

import nurgling.NConfig;
import nurgling.NGameUI;
import nurgling.NInventory;
import nurgling.NUtils;
import nurgling.actions.*;
import nurgling.actions.bots.EquipTravellersSacksFromBelt;
import nurgling.areas.NArea;
import nurgling.areas.NContext;
import nurgling.tools.NAlias;
import nurgling.widgets.Specialisation;

import java.util.ArrayList;
import java.util.Arrays;

public class WildFlowerFarmer implements Action {

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        NContext nContext = new NContext(gui);
        boolean oldStackingValue = ((NInventory) NUtils.getGameUI().maininv).bundle.a;

        NArea.Specialisation field = new NArea.Specialisation(Specialisation.SpecName.crop.toString(), "Wild Flower");
        NArea.Specialisation seed = new NArea.Specialisation(Specialisation.SpecName.seed.toString(), "Wild Flower");
        NArea.Specialisation trough = new NArea.Specialisation(Specialisation.SpecName.trough.toString());
        NArea.Specialisation swill = new NArea.Specialisation(Specialisation.SpecName.swill.toString());

        nContext.getSpecArea(Specialisation.SpecName.crop, "Wild Flower");

        NArea wildFlowerArea = NContext.findOut("Wildflower", 1);

        if(wildFlowerArea == null) {
            gui.msg("PUT Area for Wild Flower not found, flowers will not be collected.");
        }

        ArrayList<NArea.Specialisation> req = new ArrayList<>();
        req.add(field);
        req.add(seed);
        ArrayList<NArea.Specialisation> opt = new ArrayList<>();
        req.add(trough);
        opt.add(swill);

        if (new Validator(req, opt).run(gui).IsSuccess()) {
            NUtils.stackSwitch(true);

            if ((Boolean) NConfig.get(NConfig.Key.validateAllCropsBeforeHarvest)) {
                if (!new ValidateAllCropsReady(NContext.findSpec(field), new NAlias("plants/wildflower")).run(gui).isSuccess) {
                    NUtils.stackSwitch(oldStackingValue);
                    gui.msg("Not all wild flower crops are ready for harvest, skipping harvest.");
                    return Results.SUCCESS();
                }
            }

            new HarvestCrop(
                    NContext.findSpec(field),
                    NContext.findSpec(seed),
                    NContext.findSpec(trough),
                    NContext.findSpec(swill),
                    new NAlias("plants/wildflower")
            ).run(gui);
            
            if ((Boolean) NConfig.get(NConfig.Key.autoEquipTravellersSacks)) {
                new EquipTravellersSacksFromBelt().run(gui);
            }
            
            if (wildFlowerArea != null)
                new CollectItemsToPile(NContext.findSpec(field).getRCArea(), wildFlowerArea.getRCArea(), new NAlias(new ArrayList<>(Arrays.asList("flower-wild", "Wildflower")), new ArrayList<>(Arrays.asList("seed")))).run(gui);
            new SeedCrop(NContext.findSpec(field), NContext.findSpec(seed), new NAlias("plants/wildflower"), new NAlias("Wildflower Seeds"), false).run(gui);

            NUtils.stackSwitch(oldStackingValue);

            return Results.SUCCESS();
        }

        NUtils.stackSwitch(oldStackingValue);

        return Results.FAIL();
    }
}
