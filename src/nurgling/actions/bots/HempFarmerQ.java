package nurgling.actions.bots;

import nurgling.NGameUI;
import nurgling.actions.*;
import nurgling.areas.NArea;
import nurgling.areas.NContext;
import nurgling.tools.NAlias;
import nurgling.widgets.Specialisation;

import java.util.ArrayList;

public class HempFarmerQ implements Action {
    @Override
    public Results run(NGameUI gui) throws InterruptedException {

        NArea.Specialisation cropQ = new NArea.Specialisation(Specialisation.SpecName.cropQ.toString(), "Hemp");
        NArea.Specialisation seedQ = new NArea.Specialisation(Specialisation.SpecName.seedQ.toString(), "Hemp");
        ArrayList<NArea.Specialisation> req = new ArrayList<>();
        req.add(cropQ);
        req.add(seedQ);

        new HarvestCrop(
                NContext.findSpec(cropQ),
                NContext.findSpec(seedQ),
                null,
                new NAlias("plants/hemp"),
                true
        ).run(gui);
        if(NContext.findOut("Hemp Fibres", 1)!=null)
            new CollectItemsToPile(NContext.findSpec(cropQ).getRCArea(),NContext.findOut("Hemp Fibres", 1).getRCArea(),new NAlias("hempfibre", "Hemp Fibres")).run(gui);
        new SeedCrop(NContext.findSpec(cropQ),NContext.findSpec(seedQ),new NAlias("plants/hemp"),new NAlias("Hemp"), false, true).run(gui);
        return Results.SUCCESS();
    }
}
