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

public class WildTuberFarmer implements Action {
    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        NContext nContext = new NContext(gui);
        boolean oldStackingValue = ((NInventory) NUtils.getGameUI().maininv).bundle.a;

        NArea.Specialisation field = new NArea.Specialisation(Specialisation.SpecName.crop.toString(), "Wild Tuber");
        NArea.Specialisation wildTuberAsSeed = new NArea.Specialisation(Specialisation.SpecName.seed.toString(), "Wild Tuber");
        NArea.Specialisation trough = new NArea.Specialisation(Specialisation.SpecName.trough.toString());
        NArea.Specialisation swill = new NArea.Specialisation(Specialisation.SpecName.swill.toString());

        nContext.getSpecArea(Specialisation.SpecName.crop, "Wild Tuber");

        ArrayList<NArea.Specialisation> req = new ArrayList<>();
        req.add(field);
        req.add(wildTuberAsSeed);
        ArrayList<NArea.Specialisation> opt = new ArrayList<>();
        opt.add(trough);
        opt.add(swill);

        if (new Validator(req, opt).run(gui).IsSuccess()) {
            NUtils.stackSwitch(true);

            if ((Boolean) NConfig.get(NConfig.Key.validateAllCropsBeforeHarvest)) {
                if (!new ValidateAllCropsReady(NContext.findSpec(field), new NAlias("plants/tuber")).run(gui).isSuccess) {
                    NUtils.stackSwitch(oldStackingValue);
                    gui.msg("Not all wild tuber crops are ready for harvest, skipping harvest.");
                    return Results.SUCCESS();
                }
            }

            new HarvestCrop(
                    NContext.findSpec(field),
                    NContext.findSpec(wildTuberAsSeed),
                    NContext.findSpec(trough),
                    NContext.findSpec(swill),
                    new NAlias("plants/tuber")
            ).run(gui);
            
            if ((Boolean) NConfig.get(NConfig.Key.autoEquipTravellersSacks)) {
                new EquipTravellersSacksFromBelt().run(gui);
            }
            
            if (NContext.findSpec(wildTuberAsSeed) != null)
                new CollectItemsToPile(NContext.findSpec(field).getRCArea(), NContext.findSpec(wildTuberAsSeed).getRCArea(), new NAlias("items/pretuber", "Wild Tuber")).run(gui);

            new SeedCrop(NContext.findSpec(field), NContext.findSpec(wildTuberAsSeed), new NAlias("plants/tuber"), new NAlias("Wild Tuber"), true).run(gui);

            NUtils.stackSwitch(oldStackingValue);

            return Results.SUCCESS();
        }

        NUtils.stackSwitch(oldStackingValue);

        return Results.FAIL();
    }
}
