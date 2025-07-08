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
        NArea.Specialisation redOnionAsSeed = new NArea.Specialisation(Specialisation.SpecName.seed.toString(), "Red Onion");
        NArea.Specialisation trough = new NArea.Specialisation(Specialisation.SpecName.trough.toString());
        NArea.Specialisation swill = new NArea.Specialisation(Specialisation.SpecName.swill.toString());

        ArrayList<NArea.Specialisation> req = new ArrayList<>();
        req.add(field);
        req.add(redOnionAsSeed);
        ArrayList<NArea.Specialisation> opt = new ArrayList<>();
        opt.add(swill);


        if(new Validator(req, opt).run(gui).IsSuccess())
        {
            new HarvestCrop(
                    NContext.findSpec(field),
                    NContext.findSpec(redOnionAsSeed),
                    NContext.findSpec(trough),
                    NContext.findSpec(swill),
                    new NAlias("plants/redonion")
            ).run(gui);
            if(NContext.findOut("Red Onion", 1)!=null)
                new CollectItemsToPile(NContext.findSpec(field).getRCArea(),NContext.findOut("Red Onion", 1).getRCArea(),new NAlias("items/redonion", "Red Onion")).run(gui);

            new SeedCrop(NContext.findSpec(field),NContext.findSpec(redOnionAsSeed),new NAlias("plants/redonion"),new NAlias("Red Onion"), true).run(gui);
        }

        return Results.SUCCESS();

    }
}
