package nurgling.actions.bots;

import nurgling.NGameUI;
import nurgling.actions.*;
import nurgling.areas.NArea;
import nurgling.tools.NAlias;
import nurgling.widgets.Specialisation;

import java.util.ArrayList;

public class BeetrootFarmer implements Action {
    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        NArea.Specialisation field = new NArea.Specialisation(Specialisation.SpecName.crop.toString(), "Beetroot");
        NArea beetroot = NArea.findOut("Beetroot", 1);
        NArea beetrootAsSeed = NArea.findIn("Beetroot");
        NArea beetrootLeaves = NArea.findOut("Beetroot Leaves", 1);
        NArea.Specialisation trough = new NArea.Specialisation(Specialisation.SpecName.trough.toString());
        NArea.Specialisation swill = new NArea.Specialisation(Specialisation.SpecName.swill.toString());

        ArrayList<NArea.Specialisation> req = new ArrayList<>();
        req.add(field);
        ArrayList<NArea.Specialisation> opt = new ArrayList<>();
        opt.add(swill);


        if(new Validator(req, opt).run(gui).IsSuccess())
        {
            new HarvestCrop(NArea.findSpec(field),beetroot,NArea.findSpec(trough),NArea.findSpec(swill),new NAlias("plants/beet"),new NAlias("Beetroot"),3, true).run(gui);

            if(NArea.findOut("Beetroot Leaves", 1)!=null)
                new CollectItemsToPile(NArea.findSpec(field).getRCArea(),NArea.findOut("Beetroot Leaves", 1).getRCArea(),new NAlias("beetleaves", "Beetroot Leaves")).run(gui);

            if(NArea.findOut("Beetroot", 1)!=null)
                new CollectItemsToPile(NArea.findSpec(field).getRCArea(),NArea.findOut("Beetroot", 1).getRCArea(),new NAlias("items/beet", "Beetroot")).run(gui);

            new SeedCrop(NArea.findSpec(field),beetrootAsSeed,new NAlias("plants/beet"),new NAlias("Beetroot"), true).run(gui);
        }

        return Results.SUCCESS();

    }
}
