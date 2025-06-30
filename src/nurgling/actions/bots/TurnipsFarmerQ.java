package nurgling.actions.bots;

import nurgling.*;
import nurgling.actions.*;
import nurgling.areas.*;
import nurgling.tools.*;
import nurgling.widgets.Specialisation;

import java.util.*;

public class TurnipsFarmerQ implements Action {
    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        NArea.Specialisation cropQ = new NArea.Specialisation(Specialisation.SpecName.cropQ.toString(), "Turnip");
        NArea.Specialisation seedQ = new NArea.Specialisation(Specialisation.SpecName.seedQ.toString(), "Turnip");
        ArrayList<NArea.Specialisation> req = new ArrayList<>();
        req.add(cropQ);
        req.add(seedQ);

        // 1. Harvest all in cropQ and drop to chest in seedQ
        new HarvestCrop(
                NContext.findSpec(cropQ),
                NContext.findSpec(seedQ),
                null,
                new NAlias("plants/turnip"),
                true
        ).run(gui);

        if(NContext.findOut("Turnip", 1)!=null)
            new CollectItemsToPile(NContext.findSpec(cropQ).getRCArea(),NContext.findOut("Turnip", 1).getRCArea(),new NAlias("items/turnip", "Turnip")).run(gui);
        new SeedCrop(NContext.findSpec(cropQ),NContext.findSpec(seedQ),new NAlias("plants/turnip"),new NAlias("Turnip"), false, true).run(gui);
        return Results.SUCCESS();
    }
}