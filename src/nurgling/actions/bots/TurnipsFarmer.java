package nurgling.actions.bots;

import nurgling.NGameUI;
import nurgling.actions.*;
import nurgling.areas.NArea;
import nurgling.tools.NAlias;

import java.util.ArrayList;


public class TurnipsFarmer implements Action {
    @Override
    public Results run(NGameUI gui) throws InterruptedException {

        NArea.Specialisation field = new NArea.Specialisation("crop", "Turnip");
        NArea.Specialisation seed = new NArea.Specialisation("seed", "Turnip");
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
            new HarvestCrop(NArea.findSpec(field),NArea.findSpec(seed),NArea.findSpec(trough),NArea.findSpec(swill),new NAlias("plants/turnip"),new NAlias("Turnip"),1).run(gui);
            new SeedCrop(NArea.findSpec(field),NArea.findSpec(seed),NArea.findSpec(trough),NArea.findSpec(swill),new NAlias("plants/turnip"),new NAlias("Turnip"),1).run(gui);
            return Results.SUCCESS();
        }



        return Results.FAIL();
    }
}
