package nurgling.actions.bots.farmers;

import nurgling.NConfig;
import nurgling.NGameUI;
import nurgling.NInventory;
import nurgling.NUtils;
import nurgling.actions.*;
import nurgling.actions.ValidateAllCropsReady;
import nurgling.actions.bots.EquipTravellersSacksFromBelt;
import nurgling.areas.NArea;
import nurgling.areas.NContext;
import nurgling.tools.NAlias;
import nurgling.widgets.Specialisation;

import java.util.ArrayList;


public class BarleyFarmer implements Action {
    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        NContext nContext = new NContext(gui);
        boolean oldStackingValue = ((NInventory) NUtils.getGameUI().maininv).bundle.a;

        NArea.Specialisation field = new NArea.Specialisation(Specialisation.SpecName.crop.toString(), "Barley");
        NArea.Specialisation seed = new NArea.Specialisation(Specialisation.SpecName.seed.toString(), "Barley");
        NArea.Specialisation trough = new NArea.Specialisation(Specialisation.SpecName.trough.toString());
        NArea.Specialisation swill = new NArea.Specialisation(Specialisation.SpecName.swill.toString());

        nContext.getSpecArea(Specialisation.SpecName.crop, "Barley");

        boolean ignoreStraw = (Boolean) NConfig.get(NConfig.Key.ignoreStrawInFarmers);

        NArea strawArea = NContext.findOut("Straw", 1);

        if(!ignoreStraw && strawArea == null) {
            return Results.ERROR("PUT Area for Straw required, but not found!");
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
                if (!new ValidateAllCropsReady(NContext.findSpec(field), new NAlias("plants/barley")).run(gui).isSuccess) {
                    NUtils.stackSwitch(oldStackingValue);
                    gui.msg("Not all barley crops are ready for harvest, skipping harvest.");
                    return Results.SUCCESS();
                }
            }

            new HarvestCrop(
                    NContext.findSpec(field),
                    NContext.findSpec(seed),
                    NContext.findSpec(trough),
                    NContext.findSpec(swill),
                    new NAlias("plants/barley")
            ).run(gui);
            
            // Auto-equip traveller's sacks if setting is enabled
            if ((Boolean) NConfig.get(NConfig.Key.autoEquipTravellersSacks)) {
                new EquipTravellersSacksFromBelt().run(gui);
            }
            
            if (!ignoreStraw && strawArea != null)
                new CollectItemsToPile(NContext.findSpec(field).getRCArea(), strawArea.getRCArea(), new NAlias("straw", "Straw")).run(gui);
            new SeedCrop(NContext.findSpec(field), NContext.findSpec(seed), new NAlias("plants/barley"), new NAlias("Barley"), false).run(gui);

            NUtils.stackSwitch(oldStackingValue);

            return Results.SUCCESS();
        }

        NUtils.stackSwitch(oldStackingValue);

        return Results.FAIL();
    }
}
