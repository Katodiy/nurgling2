package nurgling.actions.bots;

import nurgling.NGameUI;
import nurgling.actions.*;
import nurgling.areas.NArea;
import nurgling.tools.NAlias;

import java.util.ArrayList;


public class FlaxFarmer implements Action {
    @Override
    public Results run(NGameUI gui) throws InterruptedException {

        NArea.Specialisation field = new NArea.Specialisation("crop", "Flax");
        NArea.Specialisation seed = new NArea.Specialisation("seed", "Flax");
        NArea.Specialisation trough = new NArea.Specialisation("trough");
        NArea.Specialisation swill = new NArea.Specialisation("swill");
        ArrayList<NArea.Specialisation> req = new ArrayList<>();
        req.add(field);
        req.add(seed);
        req.add(trough);
        ArrayList<NArea.Specialisation> opt = new ArrayList<>();
        opt.add(swill);

        if(new Validator(req, opt).run(gui).IsSuccess())
        {
            new HarvestCrop(NArea.findSpec(field),NArea.findSpec(seed),NArea.findSpec(trough),NArea.findSpec(swill),new NAlias("plants/flax"),new NAlias("Flax"),3).run(gui);
            if(NArea.findOut("Flax Fibres")!=null)
                new CollectItemsToPile(NArea.findSpec(field).getRCArea(),NArea.findOut("Flax Fibres").getRCArea(),new NAlias("flaxfibre", "Flax Fibres")).run(gui);
            new SeedCrop(NArea.findSpec(field),NArea.findSpec(seed),new NAlias("plants/flax"),new NAlias("Flax")).run(gui);
            return Results.SUCCESS();
        }



        return Results.FAIL();
    }
}
