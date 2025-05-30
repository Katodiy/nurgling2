package nurgling.actions.bots;

import nurgling.NGameUI;
import nurgling.actions.*;
import nurgling.areas.NArea;
import nurgling.tools.NAlias;
import nurgling.widgets.Specialisation;

import java.util.ArrayList;

public class LettuceFarmer implements Action {
    @Override
    public Results run(NGameUI gui) throws InterruptedException {

        NArea.Specialisation field = new NArea.Specialisation(Specialisation.SpecName.crop.toString(), "Lettuce");
        NArea.Specialisation seed = new NArea.Specialisation(Specialisation.SpecName.seed.toString(), "Lettuce");
        NArea lettuceLeaf = NArea.findOut(new NAlias("Lettuce Leaf"), 1);
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
            new HarvestCrop(NArea.findSpec(field),NArea.findSpec(seed),NArea.findSpec(trough),NArea.findSpec(swill),new NAlias("plants/lettuce"),new NAlias("Lettuce"),4, false).run(gui);
            if(NArea.findOut("Head of Lettuce", 1)!=null)
                new LettuceAndPumpkinCollector(NArea.findSpec(field), NArea.findSpec(seed), lettuceLeaf, new NAlias("items/lettucehead", "Head of Lettuce"), NArea.findSpec(trough)).run(gui);
            new SeedCrop(NArea.findSpec(field),NArea.findSpec(seed),new NAlias("plants/lettuce"),new NAlias("Lettuce"), false).run(gui);
            return Results.SUCCESS();
        }

        return Results.FAIL();
    }
}
