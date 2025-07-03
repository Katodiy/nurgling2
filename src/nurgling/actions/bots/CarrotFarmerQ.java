package nurgling.actions.bots;

import nurgling.NGameUI;
import nurgling.actions.*;
import nurgling.areas.NArea;
import nurgling.areas.NContext;
import nurgling.tools.NAlias;
import nurgling.widgets.Specialisation;

import java.util.ArrayList;


public class CarrotFarmerQ implements Action {
    @Override
    public Results run(NGameUI gui) throws InterruptedException {

        NArea.Specialisation cropQ = new NArea.Specialisation(Specialisation.SpecName.cropQ.toString(), "Carrot");
        NArea.Specialisation seedQ = new NArea.Specialisation(Specialisation.SpecName.seedQ.toString(), "Carrot");

        ArrayList<NArea.Specialisation> req = new ArrayList<>();
        req.add(cropQ);
        req.add(seedQ);

        ArrayList<NArea.Specialisation> opt = new ArrayList<>();

        if(new Validator(req, opt).run(gui).IsSuccess())
        {
            new HarvestCrop(
                    NContext.findSpec(cropQ),
                    NContext.findSpec(seedQ),
                    null,
                    new NAlias("plants/carrot"),
                    true
            ).run(gui);
            if(NContext.findOut("Carrot", 1)!=null)
                new CollectItemsToPile(NContext.findSpec(cropQ).getRCArea(),NContext.findOut("Carrot", 1).getRCArea(),new NAlias("items/carrot", "Carrot")).run(gui);

            new SeedCrop(NContext.findSpec(cropQ),NContext.findSpec(seedQ),new NAlias("plants/carrot"),new NAlias("Carrot"), false, true).run(gui);

            return Results.SUCCESS();
        }

        return Results.FAIL();
    }
}
