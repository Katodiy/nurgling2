package nurgling.actions.bots;

import nurgling.NGameUI;
import nurgling.actions.*;
import nurgling.areas.NArea;
import nurgling.areas.NContext;
import nurgling.tools.NAlias;
import nurgling.widgets.Specialisation;

import java.util.ArrayList;

public class RedOnionFarmer implements Action {
    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        NArea.Specialisation field = new NArea.Specialisation(Specialisation.SpecName.crop.toString(), "Red Onion");
        NArea redOnion = NContext.findOut("Red Onion", 1);
        NArea redOnionAsSeed = NContext.findIn("Red Onion");
        NArea.Specialisation trough = new NArea.Specialisation(Specialisation.SpecName.trough.toString());
        NArea.Specialisation swill = new NArea.Specialisation(Specialisation.SpecName.swill.toString());

        ArrayList<NArea.Specialisation> req = new ArrayList<>();
        req.add(field);
        ArrayList<NArea.Specialisation> opt = new ArrayList<>();
        opt.add(swill);


        if(new Validator(req, opt).run(gui).IsSuccess())
        {
            new HarvestCrop(
                    NContext.findSpec(field),
                    redOnion,
                    NContext.findSpec(trough),
                    NContext.findSpec(swill),
                    new NAlias("plants/redonion")
            ).run(gui);
            if(NContext.findOut("Red Onion", 1)!=null)
                new CollectItemsToPile(NContext.findSpec(field).getRCArea(),NContext.findOut("Red Onion", 1).getRCArea(),new NAlias("items/redonion", "Red Onion")).run(gui);

            new SeedCrop(NContext.findSpec(field),redOnionAsSeed,new NAlias("plants/redonion"),new NAlias("Red Onion"), true).run(gui);
        }

        return Results.SUCCESS();

    }
}
