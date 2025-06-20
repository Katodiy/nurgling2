package nurgling.actions.bots;

import nurgling.NGameUI;
import nurgling.actions.*;
import nurgling.areas.NArea;
import nurgling.areas.NContext;
import nurgling.tools.NAlias;
import nurgling.widgets.Specialisation;

import java.util.ArrayList;


public class TurnipsFarmer implements Action {
    @Override
    public Results run(NGameUI gui) throws InterruptedException {

        NArea.Specialisation field = new NArea.Specialisation(Specialisation.SpecName.crop.toString(), "Turnip");
        NArea.Specialisation seed = new NArea.Specialisation(Specialisation.SpecName.seed.toString(), "Turnip");
        NArea.Specialisation trough = new NArea.Specialisation(Specialisation.SpecName.trough.toString());
        NArea.Specialisation swill = new NArea.Specialisation(Specialisation.SpecName.swill.toString());
        ArrayList<NArea.Specialisation> req = new ArrayList<>();
        req.add(field);
        req.add(seed);
        req.add(trough);
        ArrayList<NArea.Specialisation> opt = new ArrayList<>();
        opt.add(swill);

        if(new Validator(req, opt).run(gui).IsSuccess())
        {
//            new HarvestCrop(NArea.findSpec(field),NArea.findSpec(seed),NArea.findSpec(trough),NArea.findSpec(swill),new NAlias("plants/turnip"),new NAlias("Turnip"),1, false).run(gui);
            new HarvestCrop(
                    NContext.findSpec(field),
                    NContext.findSpec(seed),
                    NContext.findSpec(trough),
                    NContext.findSpec(swill),
                    new NAlias("plants/turnip")
            ).run(gui);
            if(NContext.findOut("Turnip", 1)!=null)
                new CollectItemsToPile(NContext.findSpec(field).getRCArea(),NContext.findOut("Turnip", 1).getRCArea(),new NAlias("items/turnip", "Turnip")).run(gui);
            new SeedCrop(NContext.findSpec(field),NContext.findSpec(seed),new NAlias("plants/turnip"),new NAlias("Turnip"), false).run(gui);
            return Results.SUCCESS();
        }

        return Results.FAIL();
    }
}
