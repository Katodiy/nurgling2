package nurgling.actions.bots;

import nurgling.NGameUI;
import nurgling.actions.*;
import nurgling.areas.NArea;
import nurgling.areas.NContext;
import nurgling.tools.NAlias;
import nurgling.widgets.Specialisation;

import java.util.ArrayList;


public class MilletFarmer implements Action {
    @Override
    public Results run(NGameUI gui) throws InterruptedException {

        NArea.Specialisation field = new NArea.Specialisation(Specialisation.SpecName.crop.toString(), "Millet");
        NArea.Specialisation seed = new NArea.Specialisation(Specialisation.SpecName.seed.toString(), "Millet");
        NArea.Specialisation trough = new NArea.Specialisation(Specialisation.SpecName.trough.toString());
        NArea.Specialisation swill = new NArea.Specialisation(Specialisation.SpecName.swill.toString());
        ArrayList<NArea.Specialisation> req = new ArrayList<>();
        req.add(field);
        req.add(seed);
        ArrayList<NArea.Specialisation> opt = new ArrayList<>();
        req.add(trough);
        opt.add(swill);

        if(new Validator(req, opt).run(gui).IsSuccess())
        {
            new HarvestCrop(
                    NContext.findSpec(field),
                    NContext.findSpec(seed),
                    NContext.findSpec(trough),
                    NContext.findSpec(swill),
                    new NAlias("plants/millet")
            ).run(gui);
            if(NContext.findOut("Straw", 1)!=null)
                new CollectItemsToPile(NContext.findSpec(field).getRCArea(),NContext.findOut("Straw", 1).getRCArea(),new NAlias("straw", "Straw")).run(gui);
            new SeedCrop(NContext.findSpec(field),NContext.findSpec(seed),new NAlias("plants/millet"),new NAlias("Millet"), false).run(gui);
            return Results.SUCCESS();
        }

        return Results.FAIL();
    }
}
