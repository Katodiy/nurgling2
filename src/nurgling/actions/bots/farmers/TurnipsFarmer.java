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


public class TurnipsFarmer implements Action {
    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        NContext nContext = new NContext(gui);
        boolean oldStackingValue = ((NInventory) NUtils.getGameUI().maininv).bundle.a;

        NArea.Specialisation field = new NArea.Specialisation(Specialisation.SpecName.crop.toString(), "Turnip");
        NArea.Specialisation seed = new NArea.Specialisation(Specialisation.SpecName.seed.toString(), "Turnip");
        NArea.Specialisation trough = new NArea.Specialisation(Specialisation.SpecName.trough.toString());
        NArea.Specialisation swill = new NArea.Specialisation(Specialisation.SpecName.swill.toString());

        nContext.getSpecArea(Specialisation.SpecName.crop, "Turnip");

        NArea turnipArea = NContext.findOut("Turnip", 1);

        ArrayList<NArea.Specialisation> req = new ArrayList<>();
        req.add(field);
        req.add(seed);
        req.add(trough);
        ArrayList<NArea.Specialisation> opt = new ArrayList<>();
        opt.add(swill);

        if (new Validator(req, opt).run(gui).IsSuccess()) {
            NUtils.stackSwitch(true);

            if ((Boolean) NConfig.get(NConfig.Key.validateAllCropsBeforeHarvest)) {
                if (!new ValidateAllCropsReady(NContext.findSpec(field), new NAlias("plants/turnip")).run(gui).isSuccess) {
                    NUtils.stackSwitch(oldStackingValue);
                    return Results.ERROR("Not all turnip crops are ready for harvest");
                }
            }

            new HarvestCrop(
                    NContext.findSpec(field),
                    NContext.findSpec(seed),
                    NContext.findSpec(trough),
                    NContext.findSpec(swill),
                    new NAlias("plants/turnip")
            ).run(gui);
            
            // Auto-equip traveller's sacks if setting is enabled
            if ((Boolean) NConfig.get(NConfig.Key.autoEquipTravellersSacks)) {
                new EquipTravellersSacksFromBelt().run(gui);
            }
            
            if (turnipArea != null)
                new CollectItemsToPile(NContext.findSpec(field).getRCArea(), turnipArea.getRCArea(), new NAlias("items/turnip", "Turnip")).run(gui);
            new SeedCrop(NContext.findSpec(field), NContext.findSpec(seed), new NAlias("plants/turnip"), new NAlias("Turnip"), false).run(gui);

            NUtils.stackSwitch(oldStackingValue);

            return Results.SUCCESS();
        }

        NUtils.stackSwitch(oldStackingValue);

        return Results.FAIL();
    }
}
