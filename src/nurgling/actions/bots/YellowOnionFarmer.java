package nurgling.actions.bots;

import nurgling.NConfig;
import nurgling.NGameUI;
import nurgling.NInventory;
import nurgling.NUtils;
import nurgling.actions.*;
import nurgling.areas.NArea;
import nurgling.areas.NContext;
import nurgling.tools.NAlias;
import nurgling.widgets.Specialisation;

import java.util.ArrayList;

public class YellowOnionFarmer implements Action {
    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        boolean oldStackingValue = ((NInventory) NUtils.getGameUI().maininv).bundle.a;

        NArea.Specialisation field = new NArea.Specialisation(Specialisation.SpecName.crop.toString(), "Yellow Onion");
        NArea.Specialisation yellowOnionAsSeed = new NArea.Specialisation(Specialisation.SpecName.seed.toString(), "Yellow Onion");
        NArea.Specialisation trough = new NArea.Specialisation(Specialisation.SpecName.trough.toString());
        NArea.Specialisation swill = new NArea.Specialisation(Specialisation.SpecName.swill.toString());

        ArrayList<NArea.Specialisation> req = new ArrayList<>();
        req.add(field);
        req.add(yellowOnionAsSeed);
        ArrayList<NArea.Specialisation> opt = new ArrayList<>();
        opt.add(swill);

        if (new Validator(req, opt).run(gui).IsSuccess()) {
            NUtils.stackSwitch(true);

            new HarvestCrop(
                    NContext.findSpec(field),
                    NContext.findSpec(yellowOnionAsSeed),
                    NContext.findSpec(trough),
                    NContext.findSpec(swill),
                    new NAlias("plants/yellowonion")
            ).run(gui);
            
            // Auto-equip traveller's sacks if setting is enabled
            if ((Boolean) NConfig.get(NConfig.Key.autoEquipTravellersSacks)) {
                new EquipTravellersSacksFromBelt().run(gui);
            }
            
            if (NContext.findSpec(yellowOnionAsSeed) != null)
                new CollectItemsToPile(NContext.findSpec(field).getRCArea(), NContext.findSpec(yellowOnionAsSeed).getRCArea(), new NAlias("items/yellowonion", "Yellow Onion")).run(gui);

            new SeedCrop(NContext.findSpec(field), NContext.findSpec(yellowOnionAsSeed), new NAlias("plants/yellowonion"), new NAlias("Yellow Onion"), true).run(gui);

            NUtils.stackSwitch(oldStackingValue);

            return Results.SUCCESS();
        }

        NUtils.stackSwitch(oldStackingValue);

        return Results.FAIL();
    }
}
