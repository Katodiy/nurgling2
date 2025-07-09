package nurgling.actions.bots;

import nurgling.NGameUI;
import nurgling.NInventory;
import nurgling.NUtils;
import nurgling.actions.*;
import nurgling.areas.NArea;
import nurgling.areas.NContext;
import nurgling.tools.NAlias;
import nurgling.widgets.Specialisation;

import java.util.ArrayList;

public class GarlicFarmer implements Action {
    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        boolean oldStackingValue = ((NInventory) NUtils.getGameUI().maininv).bundle.a;

        NArea.Specialisation field = new NArea.Specialisation(Specialisation.SpecName.crop.toString(), "Garlic");
        NArea.Specialisation garlicAsSeed = new NArea.Specialisation(Specialisation.SpecName.seed.toString(), "Garlic");
        NArea.Specialisation trough = new NArea.Specialisation(Specialisation.SpecName.trough.toString());
        NArea.Specialisation swill = new NArea.Specialisation(Specialisation.SpecName.swill.toString());

        ArrayList<NArea.Specialisation> req = new ArrayList<>();
        req.add(field);
        ArrayList<NArea.Specialisation> opt = new ArrayList<>();
        opt.add(swill);

        if (new Validator(req, opt).run(gui).IsSuccess()) {
            NUtils.stackSwitch(true);

            new HarvestCrop(
                    NContext.findSpec(field),
                    NContext.findSpec(garlicAsSeed),
                    NContext.findSpec(trough),
                    NContext.findSpec(swill),
                    new NAlias("plants/garlic")
            ).run(gui);
            if (NContext.findSpec(garlicAsSeed) != null)
                new CollectItemsToPile(NContext.findSpec(field).getRCArea(), NContext.findSpec(garlicAsSeed).getRCArea(), new NAlias("items/garlic", "Garlic")).run(gui);

            new SeedCrop(NContext.findSpec(field), NContext.findSpec(garlicAsSeed), new NAlias("plants/garlic"), new NAlias("Garlic"), true).run(gui);

            NUtils.stackSwitch(oldStackingValue);

            return Results.SUCCESS();
        }

        NUtils.stackSwitch(oldStackingValue);

        return Results.FAIL();
    }
}
