package nurgling.actions.bots;

import nurgling.NConfig;
import nurgling.NGameUI;
import nurgling.actions.*;
import nurgling.areas.NArea;
import nurgling.areas.NContext;
import nurgling.tools.NAlias;
import nurgling.widgets.Specialisation;

import java.util.ArrayList;

public class PoppyFarmerQ implements Action {
    @Override
    public Results run(NGameUI gui) throws InterruptedException {

        NArea.Specialisation cropQ = new NArea.Specialisation(Specialisation.SpecName.cropQ.toString(), "Poppy");
        NArea.Specialisation seedQ = new NArea.Specialisation(Specialisation.SpecName.seedQ.toString(), "Poppy");
        NArea.Specialisation trough = new NArea.Specialisation(Specialisation.SpecName.trough.toString());

        Boolean cleanupQContainers = (Boolean) NConfig.get(NConfig.Key.cleanupQContainers);

        ArrayList<NArea.Specialisation> req = new ArrayList<>();
        req.add(cropQ);
        req.add(seedQ);
        ArrayList<NArea.Specialisation> opt = new ArrayList<>();
        opt.add(trough);

        if (new Validator(req, opt).run(gui).IsSuccess()) {
            new HarvestCrop(
                    NContext.findSpec(cropQ),
                    NContext.findSpec(seedQ),
                    null,
                    new NAlias("plants/poppy"),
                    true
            ).run(gui);
            if (NContext.findOut("Poppy Flower", 1) != null)
                new CollectItemsToPile(NContext.findSpec(cropQ).getRCArea(), NContext.findOut("Poppy Flower", 1).getRCArea(), new NAlias("flower-poppy", "Poppy Flower")).run(gui);
            new SeedCrop(NContext.findSpec(cropQ), NContext.findSpec(seedQ), new NAlias("plants/poppy"), new NAlias("Poppy"), false, true).run(gui);

            if (cleanupQContainers && NContext.findSpec(trough) != null) {
                new CleanupSeedQContainer(NContext.findSpec(seedQ), new NAlias("Poppy"), NContext.findSpec(trough)).run(gui);
            }

            return Results.SUCCESS();
        }

        return Results.FAIL();
    }
}
