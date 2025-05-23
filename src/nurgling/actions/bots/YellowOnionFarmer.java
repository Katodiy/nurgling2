package nurgling.actions.bots;

import nurgling.NGameUI;
import nurgling.actions.*;
import nurgling.areas.NArea;
import nurgling.tools.NAlias;
import nurgling.widgets.Specialisation;

import java.util.ArrayList;

public class YellowOnionFarmer implements Action {
    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        NArea.Specialisation field = new NArea.Specialisation(Specialisation.SpecName.crop.toString(), "Yellow Onion");
        NArea redOnion = NArea.findOut("Yellow Onion", 1);
        NArea redOnionAsSeed = NArea.findIn("Yellow Onion");
        NArea.Specialisation trough = new NArea.Specialisation(Specialisation.SpecName.trough.toString());
        NArea.Specialisation swill = new NArea.Specialisation(Specialisation.SpecName.swill.toString());

        ArrayList<NArea.Specialisation> req = new ArrayList<>();
        req.add(field);
        ArrayList<NArea.Specialisation> opt = new ArrayList<>();
        opt.add(swill);


        if(new Validator(req, opt).run(gui).IsSuccess())
        {
            new HarvestCrop(NArea.findSpec(field),redOnion,NArea.findSpec(trough),NArea.findSpec(swill),new NAlias("plants/yellowonion"),new NAlias("Yellow Onion"),3, true).run(gui);

            if(NArea.findOut("Yellow Onion", 1)!=null)
                new CollectItemsToPile(NArea.findSpec(field).getRCArea(),NArea.findOut("Yellow Onion", 1).getRCArea(),new NAlias("items/yellowonion", "Yellow Onion")).run(gui);

            new SeedCrop(NArea.findSpec(field),redOnionAsSeed,new NAlias("plants/yellowonion"),new NAlias("Yellow Onion"), true).run(gui);
        }

        return Results.SUCCESS();

    }
}
