package nurgling.actions.bots;

import nurgling.NGameUI;
import nurgling.actions.*;
import nurgling.areas.NArea;
import nurgling.areas.NContext;
import nurgling.tools.NAlias;
import nurgling.widgets.Specialisation;

import java.util.ArrayList;

public class FlaxFarmerQ implements Action {
    @Override
    public Results run(NGameUI gui) throws InterruptedException {

        NArea.Specialisation cropQ = new NArea.Specialisation(Specialisation.SpecName.cropQ.toString(), "Flax");
        NArea.Specialisation seedQ = new NArea.Specialisation(Specialisation.SpecName.seedQ.toString(), "Flax");
        ArrayList<NArea.Specialisation> req = new ArrayList<>();
        req.add(cropQ);
        req.add(seedQ);

        new HarvestCrop(
                NContext.findSpec(cropQ),
                NContext.findSpec(seedQ),
                null,
                new NAlias("plants/flax"),
                true
        ).run(gui);
        if(NContext.findOut("Flax Fibres", 1)!=null)
            new CollectItemsToPile(NContext.findSpec(cropQ).getRCArea(),NContext.findOut("Flax Fibres", 1).getRCArea(),new NAlias("flaxfibre", "Flax Fibres")).run(gui);
        new SeedCrop(NContext.findSpec(cropQ),NContext.findSpec(seedQ),new NAlias("plants/flax"),new NAlias("Flax"), false, true).run(gui);
        return Results.SUCCESS();
    }
}
