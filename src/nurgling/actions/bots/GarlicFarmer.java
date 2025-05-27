package nurgling.actions.bots;

import nurgling.NGameUI;
import nurgling.actions.*;
import nurgling.areas.NArea;
import nurgling.tools.NAlias;
import nurgling.widgets.Specialisation;

import java.util.ArrayList;

public class GarlicFarmer implements Action {
    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        NArea.Specialisation field = new NArea.Specialisation(Specialisation.SpecName.crop.toString(), "Garlic");
        NArea garlic = NArea.findOut("Garlic", 1);
        NArea garlicAsSeed = NArea.findIn("Garlic");
        NArea.Specialisation trough = new NArea.Specialisation(Specialisation.SpecName.trough.toString());
        NArea.Specialisation swill = new NArea.Specialisation(Specialisation.SpecName.swill.toString());

        ArrayList<NArea.Specialisation> req = new ArrayList<>();
        req.add(field);
        ArrayList<NArea.Specialisation> opt = new ArrayList<>();
        opt.add(swill);


        if(new Validator(req, opt).run(gui).IsSuccess())
        {
            new HarvestCrop(NArea.findSpec(field),garlic,NArea.findSpec(trough),NArea.findSpec(swill),new NAlias("plants/garlic"),new NAlias("Garlic"),4, true).run(gui);

            if(NArea.findOut("Garlic", 1)!=null)
                new CollectItemsToPile(NArea.findSpec(field).getRCArea(),NArea.findOut("Garlic", 1).getRCArea(),new NAlias("items/garlic", "Garlic")).run(gui);

            new SeedCrop(NArea.findSpec(field),garlicAsSeed,new NAlias("plants/garlic"),new NAlias("Garlic"), true).run(gui);
        }

        return Results.SUCCESS();

    }
}
