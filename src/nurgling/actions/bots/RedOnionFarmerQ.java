package nurgling.actions.bots;

import nurgling.NGameUI;
import nurgling.actions.*;
import nurgling.areas.NArea;
import nurgling.areas.NContext;
import nurgling.tools.NAlias;
import nurgling.widgets.Specialisation;

import java.util.ArrayList;

public class RedOnionFarmerQ implements Action {
    @Override
    public Results run(NGameUI gui) throws InterruptedException {

        NArea.Specialisation cropQ = new NArea.Specialisation(Specialisation.SpecName.cropQ.toString(), "Red Onion");
        NArea.Specialisation seedQ = new NArea.Specialisation(Specialisation.SpecName.seedQ.toString(), "Red Onion");

        ArrayList<NArea.Specialisation> req = new ArrayList<>();
        req.add(cropQ);
        req.add(seedQ);

        ArrayList<NArea.Specialisation> opt = new ArrayList<>();

        if(new Validator(req, opt).run(gui).IsSuccess()) {
            new HarvestCrop(
                    NContext.findSpec(cropQ),
                    NContext.findSpec(seedQ),
                    null,
                    new NAlias("plants/redonion"),
                    true
            ).run(gui);
            if (NContext.findOut("Red Onion", 1) != null)
                new CollectItemsToPile(NContext.findSpec(cropQ).getRCArea(), NContext.findOut("Red Onion", 1).getRCArea(), new NAlias("items/redonion", "Red Onion")).run(gui);

            new SeedCrop(NContext.findSpec(cropQ), NContext.findSpec(seedQ), new NAlias("plants/redonion"), new NAlias("Red Onion"), false, true).run(gui);
        }

        return Results.SUCCESS();
    }
}
