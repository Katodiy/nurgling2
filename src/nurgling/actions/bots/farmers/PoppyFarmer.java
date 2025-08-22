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

public class PoppyFarmer implements Action {

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        NContext nContext = new NContext(gui);
        boolean oldStackingValue = ((NInventory) NUtils.getGameUI().maininv).bundle.a;

        NArea.Specialisation field = new NArea.Specialisation(Specialisation.SpecName.crop.toString(), "Poppy");
        NArea.Specialisation seed = new NArea.Specialisation(Specialisation.SpecName.seed.toString(), "Poppy");
        NArea.Specialisation trough = new NArea.Specialisation(Specialisation.SpecName.trough.toString());
        NArea.Specialisation swill = new NArea.Specialisation(Specialisation.SpecName.swill.toString());

        nContext.getSpecArea(Specialisation.SpecName.crop, "Poppy");

        NArea poppyFlowerArea = NContext.findOut("Poppy Flower", 1);

        if(poppyFlowerArea == null) {
            return Results.ERROR("PUT Area for Poppy Flower required, but not found!");
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
                if (!new ValidateAllCropsReady(NContext.findSpec(field), new NAlias("plants/poppy")).run(gui).isSuccess) {
                    NUtils.stackSwitch(oldStackingValue);
                    return Results.ERROR("Not all poppy crops are ready for harvest");
                }
            }

            new HarvestCrop(
                    NContext.findSpec(field),
                    NContext.findSpec(seed),
                    NContext.findSpec(trough),
                    NContext.findSpec(swill),
                    new NAlias("plants/poppy")
            ).run(gui);
            
            // Auto-equip traveller's sacks if setting is enabled
            if ((Boolean) NConfig.get(NConfig.Key.autoEquipTravellersSacks)) {
                new EquipTravellersSacksFromBelt().run(gui);
            }
            
            if (poppyFlowerArea != null)
                new CollectItemsToPile(NContext.findSpec(field).getRCArea(), poppyFlowerArea.getRCArea(), new NAlias("flower-poppy", "Poppy Flower")).run(gui);
            new SeedCrop(NContext.findSpec(field), NContext.findSpec(seed), new NAlias("plants/poppy"), new NAlias("Poppy"), false).run(gui);

            NUtils.stackSwitch(oldStackingValue);

            return Results.SUCCESS();
        }

        NUtils.stackSwitch(oldStackingValue);

        return Results.FAIL();
    }
}
