package nurgling.actions.bots;

import nurgling.NGameUI;
import nurgling.actions.*;
import nurgling.areas.NArea;
import nurgling.tools.NAlias;

import java.util.ArrayList;


public class HempFarmer implements Action {
    @Override
    public Results run(NGameUI gui) throws InterruptedException {

        NArea.Specialisation field = new NArea.Specialisation("crop", "Hemp");
        NArea.Specialisation seed = new NArea.Specialisation("seed", "Hemp");
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
            new HarvestCrop(NArea.findSpec(field),NArea.findSpec(seed),NArea.findSpec(trough),NArea.findSpec(swill),new NAlias("plants/hemp"),new NAlias("Hemp"),4).run(gui);
            if(NArea.findOut("Hemp Fibres")!=null)
                new CollectItemsToPile(NArea.findSpec(field).getRCArea(),NArea.findOut("Hemp Fibres").getRCArea(),new NAlias("hempfibre", "Hemp Fibres")).run(gui);
            new SeedCrop(NArea.findSpec(field),NArea.findSpec(seed),new NAlias("plants/hemp"),new NAlias("Hemp")).run(gui);
            return Results.SUCCESS();
        }



        return Results.FAIL();
    }
}
