package nurgling.actions;

import nurgling.NGameUI;
import nurgling.areas.NArea;
import nurgling.areas.NContext;
import nurgling.tools.NAlias;
import nurgling.widgets.Specialisation;

import java.util.ArrayList;

public class PumpkinFarmer implements Action {
    @Override
    public Results run(NGameUI gui) throws InterruptedException {

        NArea.Specialisation field = new NArea.Specialisation(Specialisation.SpecName.crop.toString(), "Pumpkin");
        NArea.Specialisation seed = new NArea.Specialisation(Specialisation.SpecName.seed.toString(), "Pumpkin");
        NArea pumpkinFlesh = NContext.findOut(new NAlias("Pumpkin Flesh"), 1);
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
            new HarvestCrop(NContext.findSpec(field),NContext.findSpec(seed),NContext.findSpec(trough),NContext.findSpec(swill),new NAlias("plants/pumpkin"),new NAlias("Pumpkin"),4, true).run(gui);
            if(pumpkinFlesh !=null)
                new LettuceAndPumpkinCollector(NContext.findSpec(field), NContext.findSpec(seed), pumpkinFlesh, new NAlias("items/pumpkin", "Pumpkin"), NContext.findSpec(trough)).run(gui);
            new SeedCrop(NContext.findSpec(field),NContext.findSpec(seed),new NAlias("plants/pumpkin"),new NAlias("Pumpkin"), false).run(gui);
            return Results.SUCCESS();
        }

        return Results.FAIL();
    }
}
