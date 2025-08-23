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

public class BeetrootFarmer implements Action {
    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        NContext nContext = new NContext(gui);
        boolean oldStackingValue = ((NInventory) NUtils.getGameUI().maininv).bundle.a;

        NArea.Specialisation field = new NArea.Specialisation(Specialisation.SpecName.crop.toString(), "Beetroot");
        NArea.Specialisation beetrootAsSeed = new NArea.Specialisation(Specialisation.SpecName.seed.toString(), "Beetroot");
        NArea.Specialisation trough = new NArea.Specialisation(Specialisation.SpecName.trough.toString());
        NArea.Specialisation swill = new NArea.Specialisation(Specialisation.SpecName.swill.toString());
        NArea beetrootLeavesArea = NContext.findOut("Beetroot Leaves", 1);

        nContext.getSpecArea(Specialisation.SpecName.crop, "Beetroot");

        if(beetrootLeavesArea == null) {
            return Results.ERROR("PUT Area for Beetroot Leaves required, but not found!");
        }

        ArrayList<NArea.Specialisation> req = new ArrayList<>();
        req.add(field);
        req.add(beetrootAsSeed);
        ArrayList<NArea.Specialisation> opt = new ArrayList<>();
        opt.add(trough);
        opt.add(swill);

        if (new Validator(req, opt).run(gui).IsSuccess()) {
            NUtils.stackSwitch(true);

            if ((Boolean) NConfig.get(NConfig.Key.validateAllCropsBeforeHarvest)) {
                if (!new ValidateAllCropsReady(NContext.findSpec(field), new NAlias("plants/beet")).run(gui).isSuccess) {
                    NUtils.stackSwitch(oldStackingValue);
                    gui.msg("Not all beetroot crops are ready for harvest, skipping harvest.");
                    return Results.SUCCESS();
                }
            }

            new HarvestCrop(
                    NContext.findSpec(field),
                    NContext.findSpec(beetrootAsSeed),
                    NContext.findSpec(trough),
                    NContext.findSpec(swill),
                    new NAlias("plants/beet")
            ).run(gui);
            
            // Auto-equip traveller's sacks if setting is enabled
            if ((Boolean) NConfig.get(NConfig.Key.autoEquipTravellersSacks)) {
                new EquipTravellersSacksFromBelt().run(gui);
            }
            
            if (beetrootLeavesArea != null)
                new CollectItemsToPile(NContext.findSpec(field).getRCArea(), NContext.findOut("Beetroot Leaves", 1).getRCArea(), new NAlias("beetleaves", "Beetroot Leaves")).run(gui);

            if (NContext.findSpec(beetrootAsSeed) != null)
                new CollectItemsToPile(NContext.findSpec(field).getRCArea(), NContext.findSpec(beetrootAsSeed).getRCArea(), new NAlias("items/beet", "Beetroot")).run(gui);

            new SeedCrop(NContext.findSpec(field), NContext.findSpec(beetrootAsSeed), new NAlias("plants/beet"), new NAlias("Beetroot"), true).run(gui);

            NUtils.stackSwitch(oldStackingValue);

            return Results.SUCCESS();
        }

        NUtils.stackSwitch(oldStackingValue);

        return Results.FAIL();
    }
}
