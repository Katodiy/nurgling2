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

public class BeetrootFarmer implements Action {
    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        boolean oldStackingValue = ((NInventory) NUtils.getGameUI().maininv).bundle.a;

        NArea.Specialisation field = new NArea.Specialisation(Specialisation.SpecName.crop.toString(), "Beetroot");
        NArea.Specialisation beetrootAsSeed = new NArea.Specialisation(Specialisation.SpecName.seed.toString(), "Beetroot");
        NArea.Specialisation trough = new NArea.Specialisation(Specialisation.SpecName.trough.toString());
        NArea.Specialisation swill = new NArea.Specialisation(Specialisation.SpecName.swill.toString());

        NArea beetrootLeavesArea = NContext.findOut("Beetroot Leaves", 1);

        ArrayList<NArea.Specialisation> req = new ArrayList<>();
        req.add(field);
        req.add(beetrootAsSeed);
        ArrayList<NArea.Specialisation> opt = new ArrayList<>();
        opt.add(trough);
        opt.add(swill);

        if (new Validator(req, opt).run(gui).IsSuccess()) {
            NUtils.stackSwitch(true);

            new HarvestCrop(
                    NContext.findSpec(field),
                    NContext.findSpec(beetrootAsSeed),
                    NContext.findSpec(trough),
                    NContext.findSpec(swill),
                    new NAlias("plants/beet")
            ).run(gui);
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
