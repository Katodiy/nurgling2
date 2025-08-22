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

public class RedOnionFarmer implements Action {
    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        NContext nContext = new NContext(gui);
        boolean oldStackingValue = ((NInventory) NUtils.getGameUI().maininv).bundle.a;

        NArea.Specialisation field = new NArea.Specialisation(Specialisation.SpecName.crop.toString(), "Red Onion");
        NArea.Specialisation redOnionAsSeed = new NArea.Specialisation(Specialisation.SpecName.seed.toString(), "Red Onion");
        NArea.Specialisation trough = new NArea.Specialisation(Specialisation.SpecName.trough.toString());
        NArea.Specialisation swill = new NArea.Specialisation(Specialisation.SpecName.swill.toString());

        nContext.getSpecArea(Specialisation.SpecName.crop, "Red Onion");

        ArrayList<NArea.Specialisation> req = new ArrayList<>();
        req.add(field);
        req.add(redOnionAsSeed);
        ArrayList<NArea.Specialisation> opt = new ArrayList<>();
        opt.add(swill);

        if (new Validator(req, opt).run(gui).IsSuccess()) {
            NUtils.stackSwitch(true);

            if ((Boolean) NConfig.get(NConfig.Key.validateAllCropsBeforeHarvest)) {
                if (!new ValidateAllCropsReady(NContext.findSpec(field), new NAlias("plants/redonion")).run(gui).isSuccess) {
                    NUtils.stackSwitch(oldStackingValue);
                    return Results.ERROR("Not all red onion crops are ready for harvest");
                }
            }

            new HarvestCrop(
                    NContext.findSpec(field),
                    NContext.findSpec(redOnionAsSeed),
                    NContext.findSpec(trough),
                    NContext.findSpec(swill),
                    new NAlias("plants/redonion")
            ).run(gui);
            
            // Auto-equip traveller's sacks if setting is enabled
            if ((Boolean) NConfig.get(NConfig.Key.autoEquipTravellersSacks)) {
                new EquipTravellersSacksFromBelt().run(gui);
            }
            
            if (NContext.findSpec(redOnionAsSeed) != null)
                new CollectItemsToPile(NContext.findSpec(field).getRCArea(), NContext.findSpec(redOnionAsSeed).getRCArea(), new NAlias("items/redonion", "Red Onion")).run(gui);

            new SeedCrop(NContext.findSpec(field), NContext.findSpec(redOnionAsSeed), new NAlias("plants/redonion"), new NAlias("Red Onion"), true).run(gui);

            NUtils.stackSwitch(oldStackingValue);

            return Results.SUCCESS();
        }

        NUtils.stackSwitch(oldStackingValue);

        return Results.FAIL();
    }
}
