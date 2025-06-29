package nurgling.actions.bots;

import nurgling.NGameUI;
import nurgling.actions.*;
import nurgling.areas.NArea;
import nurgling.areas.NContext;
import nurgling.tools.NAlias;
import nurgling.widgets.Specialisation;

import java.util.ArrayList;


public class WheatFarmerQ implements Action {
    @Override
    public Results run(NGameUI gui) throws InterruptedException {

        NArea.Specialisation cropQ = new NArea.Specialisation(Specialisation.SpecName.cropQ.toString(), "Wheat");
        NArea.Specialisation seedQ = new NArea.Specialisation(Specialisation.SpecName.seedQ.toString(), "Wheat");
        ArrayList<NArea.Specialisation> req = new ArrayList<>();
        req.add(cropQ);
        req.add(seedQ);

        new HarvestCrop(
                NContext.findSpec(cropQ),
                NContext.findSpec(seedQ),
                null,
                    new NAlias("plants/wheat"),
                true
        ).run(gui);
        if(NContext.findOut("Straw", 1)!=null)
            new CollectItemsToPile(NContext.findSpec(cropQ).getRCArea(),NContext.findOut("Straw", 1).getRCArea(),new NAlias("straw", "Straw")).run(gui);
        new SeedCrop(NContext.findSpec(cropQ),NContext.findSpec(seedQ),new NAlias("plants/wheat"),new NAlias("Wheat"), false, true).run(gui);
        return Results.SUCCESS();
    }
}
