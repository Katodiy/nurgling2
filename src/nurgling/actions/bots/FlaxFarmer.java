package nurgling.actions.bots;

import nurgling.NGameUI;
import nurgling.actions.*;
import nurgling.areas.NArea;
import nurgling.tools.NAlias;
import nurgling.widgets.Specialisation;

import java.util.ArrayList;


public class FlaxFarmer implements Action {
    @Override
    public Results run(NGameUI gui) throws InterruptedException {

        NArea.Specialisation field = new NArea.Specialisation(Specialisation.SpecName.crop.toString(), "Flax");
        NArea.Specialisation seed = new NArea.Specialisation(Specialisation.SpecName.seed.toString(), "Flax");
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
            new HarvestCrop(NArea.findSpec(field),NArea.findSpec(seed),NArea.findSpec(trough),NArea.findSpec(swill),new NAlias("plants/flax"),new NAlias("Flax"),3).run(gui);
            if(NArea.findOut("Flax Fibres", 1)!=null)
                new CollectItemsToPile(NArea.findSpec(field).getRCArea(),NArea.findOut("Flax Fibres", 1).getRCArea(),new NAlias("flaxfibre", "Flax Fibres")).run(gui);
            new SeedCrop(NArea.findSpec(field),NArea.findSpec(seed),new NAlias("plants/flax"),new NAlias("Flax")).run(gui);
            return Results.SUCCESS();
        }



        return Results.FAIL();
    }
}
