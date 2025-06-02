package nurgling.actions;

import nurgling.NGameUI;
import nurgling.areas.NArea;
import nurgling.tools.NAlias;
import nurgling.widgets.Specialisation;

import java.util.ArrayList;

public class PumpkinFarmer implements Action {
    @Override
    public Results run(NGameUI gui) throws InterruptedException {

        NArea.Specialisation field = new NArea.Specialisation(Specialisation.SpecName.crop.toString(), "Pumpkin");
        NArea.Specialisation seed = new NArea.Specialisation(Specialisation.SpecName.seed.toString(), "Pumpkin");
        NArea lettuceLeaf = NArea.findOut(new NAlias("Pumpkin Flesh"), 1);
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
            new HarvestCrop(NArea.findSpec(field),NArea.findSpec(seed),NArea.findSpec(trough),NArea.findSpec(swill),new NAlias("plants/pumpkin"),new NAlias("Pumpkin"),4, false).run(gui);
            if(NArea.findOut("Giant Pumpkin", 1)!=null)
                new LettuceAndPumpkinCollector(NArea.findSpec(field), NArea.findSpec(seed), lettuceLeaf, new NAlias("items/giantpumpkin", "Giant Pumpkin"), NArea.findSpec(trough)).run(gui);
            new SeedCrop(NArea.findSpec(field),NArea.findSpec(seed),new NAlias("plants/pumpkin"),new NAlias("Pumpkin"), false).run(gui);
            return Results.SUCCESS();
        }

        return Results.FAIL();
    }
}
